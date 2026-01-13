package org.folio.service.protection;

import static org.folio.config.Constants.EMPTY_ARRAY;
import static org.folio.exception.ErrorCodes.ORGANIZATION_UNITS_NOT_FOUND;
import static org.folio.exception.ErrorCodes.USER_HAS_NO_ACQ_PERMISSIONS;
import static org.folio.exception.ErrorCodes.USER_HAS_NO_PERMISSIONS;
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.service.protection.AcqDesiredPermissions.MANAGE;
import static org.folio.service.protection.ProtectedOperationType.UPDATE;
import static org.folio.util.RestUtils.ACQUISITIONS_UNIT_ID;
import static org.folio.util.RestUtils.ACQUISITIONS_UNIT_IDS;
import static org.folio.util.RestUtils.ALL_UNITS_CQL;
import static org.folio.util.RestUtils.combineCqlExpressions;
import static org.folio.util.RestUtils.convertIdsToCqlQuery;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.exception.HttpException;
import org.folio.rest.acq.model.AcquisitionsUnit;
import org.folio.rest.acq.model.AcquisitionsUnitCollection;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

@Service
public class ProtectionServiceImpl implements ProtectionService {
  protected final Logger logger = LogManager.getLogger(this.getClass());

  public static final String OKAPI_HEADER_PERMISSIONS = "X-Okapi-Permissions";

  private AcquisitionsUnitsService acquisitionsUnitsService;

  @Autowired
  public void setAcquisitionsUnitsService(AcquisitionsUnitsService acquisitionsUnitsService) {
    this.acquisitionsUnitsService = acquisitionsUnitsService;
  }

  @Override
  public Future<Void> checkOperationsRestrictions(List<String> unitIds, Set<ProtectedOperationType> operations, Context context, Map<String, String> headers) {
    logger.debug("checkOperationsRestrictions:: Trying to check operation restrictions by unitIds: {} and '{}' operations", unitIds, operations.size());

    if (CollectionUtils.isEmpty(unitIds)) {
      logger.debug("checkOperationsRestrictions:: unitIds is empty");
      return Future.succeededFuture();
    }

    return getUnitsByIds(unitIds, context, headers)
        .compose(units -> {
          if (unitIds.size() == units.size()) {
            logger.info("checkOperationsRestrictions:: equal unitIds size '{}' and fetched units size '{}'", unitIds.size(), units.size());
            List<AcquisitionsUnit> activeUnits = units.stream()
              .filter(unit -> !unit.getIsDeleted())
              .collect(Collectors.toList());
            if (!activeUnits.isEmpty() && applyMergingStrategy(activeUnits, operations)) {
              return verifyUserIsMemberOfOrganizationUnits(extractUnitIds(activeUnits), headers.get(OKAPI_USERID_HEADER), context, headers);
            }
          } else {
            logger.warn("checkOperationsRestrictions:: mismatch between unitIds size '{}' and fetched units size '{}'", unitIds.size(), units.size());
            return Future.failedFuture(new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), buildUnitsNotFoundError(unitIds, extractUnitIds(units))));
          }
          return Future.succeededFuture();
        });
  }

  @Override
  public Future<Void> validateAcqUnitsOnUpdate(Organization updatedOrg, Organization currentOrg, Context context, Map<String, String> headers) {
    logger.debug("validateAcqUnitsOnUpdate:: Trying to verify acquisition units for updating between current entity and incoming payload");
    List<String> updatedAcqUnitIds = updatedOrg.getAcqUnitIds();
    List<String> currentAcqUnitIds = currentOrg.getAcqUnitIds();

    verifyUserHasManagePermission(updatedAcqUnitIds, currentAcqUnitIds, getProvidedPermissions(headers));
    return verifyIfUnitsAreActive(ListUtils.subtract(updatedAcqUnitIds, currentAcqUnitIds), context, headers)
      .compose(ok -> checkOperationsRestrictions(currentAcqUnitIds, Collections.singleton(UPDATE), context, headers));
  }

  private Future<List<AcquisitionsUnit>> getUnitsByIds(List<String> unitIds, Context context, Map<String, String> headers) {
    logger.debug("getUnitsByIds:: Trying to get units by unitIds: {}", unitIds);
    String query = combineCqlExpressions("and", ALL_UNITS_CQL, convertIdsToCqlQuery(unitIds));
    return acquisitionsUnitsService.getAcquisitionsUnits(query, 0, Integer.MAX_VALUE, context, headers)
      .map(AcquisitionsUnitCollection::getAcquisitionsUnits);
  }

  private boolean applyMergingStrategy(List<AcquisitionsUnit> units, Set<ProtectedOperationType> operations) {
    return units.stream().allMatch(unit -> operations.stream().anyMatch(operation -> operation.isProtected(unit)));
  }

  private Future<Void> verifyUserIsMemberOfOrganizationUnits(List<String> unitIdsAssignedToOrg, String currentUserId, Context context, Map<String, String> headers) {
    logger.debug("verifyUserIsMemberOfOrganizationUnits:: Trying to verify user '{}' is member of organization units: {}", currentUserId, unitIdsAssignedToOrg);
    String query = String.format("userId==%s AND %s", currentUserId, convertIdsToCqlQuery(unitIdsAssignedToOrg, ACQUISITIONS_UNIT_ID, true));
    return acquisitionsUnitsService.getAcquisitionsUnitsMemberships(query, 0, Integer.MAX_VALUE, context, headers)
      .map(unit -> {
        if (unit.getTotalRecords() == 0) {
          throw new HttpException(HttpStatus.HTTP_FORBIDDEN.toInt(), USER_HAS_NO_PERMISSIONS);
        }
        return null;
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
   * @return future completed successfully if all units exist and active or exceptionally otherwise
   */
  public Future<Void> verifyIfUnitsAreActive(List<String> acqUnitIds, Context context, Map<String, String> headers) {
    logger.debug("verifyIfUnitsAreActive:: Trying to verify if units are active by acqUnitsIds: {}", acqUnitIds);
    if (acqUnitIds.isEmpty()) {
      return Future.succeededFuture();
    }
    return getUnitsByIds(acqUnitIds, context, headers)
      .compose(units -> {
        List<String> activeUnitIds = units.stream()
          .filter(unit -> !unit.getIsDeleted())
          .map(AcquisitionsUnit::getId)
          .collect(Collectors.toList());

        if (acqUnitIds.size() != activeUnitIds.size()) {
          return Future.failedFuture(new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), buildUnitsNotFoundError(acqUnitIds, activeUnitIds)));
        }
        return Future.succeededFuture();
      });
  }
}
