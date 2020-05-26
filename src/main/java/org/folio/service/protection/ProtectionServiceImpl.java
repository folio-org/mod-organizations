package org.folio.service.protection;

import static org.folio.config.Constants.EMPTY_ARRAY;
import static org.folio.exception.ErrorCodes.ORGANIZATION_UNITS_NOT_FOUND;
import static org.folio.exception.ErrorCodes.USER_HAS_NO_ACQ_PERMISSIONS;
import static org.folio.exception.ErrorCodes.USER_HAS_NO_PERMISSIONS;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_PERMISSIONS;
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.service.protection.AcqDesiredPermissions.MANAGE;
import static org.folio.service.protection.ProtectedOperationType.UPDATE;

import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.folio.HttpStatus;
import org.folio.exception.HttpException;
import org.folio.rest.acq.model.AcquisitionsUnit;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Service
public class ProtectionServiceImpl extends BaseService implements ProtectionService {
  private AcquisitionsUnitsService acquisitionsUnitsService;
  private final List<AcquisitionsUnit> fetchedUnits = new ArrayList<>();

  @Autowired
  public void setAcquisitionsUnitsService(AcquisitionsUnitsService acquisitionsUnitsService) {
    this.acquisitionsUnitsService = acquisitionsUnitsService;
  }

  @Override
  public CompletableFuture<Void> isOperationRestricted(List<String> unitIds, Set<ProtectedOperationType> operations, String lang, Context context, Map<String, String> headers) {
    if (CollectionUtils.isNotEmpty(unitIds)) {
      return getUnitsByIds(unitIds, lang, context, headers)
        .thenCompose(units -> {
          if (unitIds.size() == units.size()) {
            List<AcquisitionsUnit> activeUnits = units.stream()
              .filter(unit -> !unit.getIsDeleted())
              .collect(Collectors.toList());
            if (!activeUnits.isEmpty() && applyMergingStrategy(activeUnits, operations)) {
              return verifyUserIsMemberOfOrganizationUnits(extractUnitIds(activeUnits), headers.get(OKAPI_USERID_HEADER), lang, context, headers);
            }
            return CompletableFuture.completedFuture(null);
          } else {
            throw new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), buildUnitsNotFoundError(unitIds, extractUnitIds(units)));
          }
        });
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<Void> validateAcqUnitsOnUpdate(Organization updatedOrg, Organization currentOrg, String lang, Context context, Map<String, String> headers) {
    List<String> updatedAcqUnitIds = updatedOrg.getAcqUnitIds();
    List<String> currentAcqUnitIds = currentOrg.getAcqUnitIds();

    return VertxCompletableFuture.runAsync(context, () -> verifyUserHasManagePermission(updatedAcqUnitIds, currentAcqUnitIds, getProvidedPermissions(headers)))
    .thenCompose(ok -> verifyIfUnitsAreActive(ListUtils.subtract(updatedAcqUnitIds, currentAcqUnitIds), lang, context, headers))
    .thenCompose(ok -> isOperationRestricted(currentAcqUnitIds, Collections.singleton(UPDATE), lang, context, headers));
  }

  private CompletableFuture<List<AcquisitionsUnit>> getUnitsByIds(List<String> unitIds, String lang, Context context, Map<String, String> headers) {
    // Check if all required units are already available
    List<AcquisitionsUnit> units = fetchedUnits.stream()
      .filter(unit -> unitIds.contains(unit.getId()))
      .distinct()
      .collect(Collectors.toList());

    if (units.size() == unitIds.size()) {
      return CompletableFuture.completedFuture(units);
    }

    String query = combineCqlExpressions("and", ALL_UNITS_CQL, convertIdsToCqlQuery(unitIds));
    return acquisitionsUnitsService.getAcquisitionsUnits(query, 0, Integer.MAX_VALUE, lang, context, headers)
      .thenApply(acquisitionsUnitCollection -> {
        List<AcquisitionsUnit> acquisitionsUnits = acquisitionsUnitCollection.getAcquisitionsUnits();
        fetchedUnits.addAll(acquisitionsUnits);
        return acquisitionsUnits;
      });
  }

  private boolean applyMergingStrategy(List<AcquisitionsUnit> units, Set<ProtectedOperationType> operations) {
    return units.stream().allMatch(unit -> operations.stream().anyMatch(operation -> operation.isProtected(unit)));
  }

  private CompletableFuture<Void> verifyUserIsMemberOfOrganizationUnits(List<String> unitIdsAssignedToOrg, String currentUserId, String lang, Context context, Map<String, String> headers) {
    String query = String.format("userId==%s AND %s", currentUserId, convertIdsToCqlQuery(unitIdsAssignedToOrg, ACQUISITIONS_UNIT_ID, true));
    return acquisitionsUnitsService.getAcquisitionsUnitsMemberships(query, 0, Integer.MAX_VALUE, lang, context, headers)
      .thenAccept(unit -> {
        if (unit.getTotalRecords() == 0) {
          throw new HttpException(HttpStatus.HTTP_FORBIDDEN.toInt(), USER_HAS_NO_PERMISSIONS);
        }
      })
      .exceptionally(t -> {
        throw new CompletionException(t);
      });
  }

  private List<String> extractUnitIds(List<AcquisitionsUnit> activeUnits) {
    return activeUnits.stream().map(AcquisitionsUnit::getId).collect(Collectors.toList());
  }

  private Error buildUnitsNotFoundError(List<String> expectedUnitIds, List<String> availableUnitIds) {
    List<String> missingUnitIds = ListUtils.subtract(expectedUnitIds, availableUnitIds);
    return ORGANIZATION_UNITS_NOT_FOUND.toError().withAdditionalProperty(ACQUISITIONS_UNIT_IDS, missingUnitIds);
  }

  /**
   * The method checks if list of acquisition units to which the order is assigned is changed, if yes,
   * then check that if the user has desired permission to manage acquisition units assignments
   *
   * @throws HttpException if user does not have manage permission
   * @param newAcqUnitIds acquisitions units assigned to purchase order from request
   * @param currentAcqUnitIds acquisitions units assigned to purchase order from storage
   */
  private void verifyUserHasManagePermission(List<String> newAcqUnitIds, List<String> currentAcqUnitIds, List<String> permissions) {
    Set<String> newAcqUnits = new HashSet<>(CollectionUtils.emptyIfNull(newAcqUnitIds));
    Set<String> acqUnitsFromStorage = new HashSet<>(CollectionUtils.emptyIfNull(currentAcqUnitIds));

    if (isManagePermissionRequired(newAcqUnits, acqUnitsFromStorage) && isUserDoesNotHaveDesiredPermission(MANAGE, permissions)){
      throw new HttpException(HttpStatus.HTTP_FORBIDDEN.toInt(), USER_HAS_NO_ACQ_PERMISSIONS);
    }
  }

  private boolean isManagePermissionRequired(Set<String> newAcqUnits, Set<String> acqUnitsFromStorage) {
    return !CollectionUtils.isEqualCollection(newAcqUnits, acqUnitsFromStorage);
  }

  private boolean isUserDoesNotHaveDesiredPermission(AcqDesiredPermissions acqPerm, List<String> permissions) {
    return !permissions.contains(acqPerm.getPermission());
  }

  private List<String> getProvidedPermissions(Map<String, String> headers) {
    return new JsonArray(headers.getOrDefault(OKAPI_HEADER_PERMISSIONS, EMPTY_ARRAY)).stream().
      map(Object::toString)
      .collect(Collectors.toList());
  }

  /**
   * Verifies if all acquisition units exist and active based on passed ids
   *
   * @param acqUnitIds list of unit IDs.
   * @return completable future completed successfully if all units exist and active or exceptionally otherwise
   */
  public CompletableFuture<Void> verifyIfUnitsAreActive(List<String> acqUnitIds, String lang, Context context, Map<String, String> headers) {
    if (acqUnitIds.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    return getUnitsByIds(acqUnitIds, lang, context, headers).thenAccept(units -> {
      List<String> activeUnitIds = units.stream()
        .filter(unit -> !unit.getIsDeleted())
        .map(AcquisitionsUnit::getId)
        .collect(Collectors.toList());

      if (acqUnitIds.size() != activeUnitIds.size()) {
        throw new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), buildUnitsNotFoundError(acqUnitIds, activeUnitIds));
      }
    });
  }
}
