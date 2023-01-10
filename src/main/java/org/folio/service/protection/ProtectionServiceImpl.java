package org.folio.service.protection;

import static org.folio.config.Constants.EMPTY_ARRAY;
import static org.folio.exception.ErrorCodes.ORGANIZATION_UNITS_NOT_FOUND;
import static org.folio.exception.ErrorCodes.USER_HAS_NO_ACQ_PERMISSIONS;
import static org.folio.exception.ErrorCodes.USER_HAS_NO_PERMISSIONS;
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.service.protection.AcqDesiredPermissions.MANAGE;
import static org.folio.service.protection.ProtectedOperationType.UPDATE;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.exception.HttpException;
import org.folio.rest.acq.model.AcquisitionsUnit;
import org.folio.rest.acq.model.AcquisitionsUnitMembershipCollection;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;

@Service
public class ProtectionServiceImpl extends BaseService implements ProtectionService {
  protected final Logger logger = LogManager.getLogger(this.getClass());
  private AcquisitionsUnitsService acquisitionsUnitsService;
  public static final String OKAPI_HEADER_PERMISSIONS = "X-Okapi-Permissions";

  @Autowired
  public void setAcquisitionsUnitsService(AcquisitionsUnitsService acquisitionsUnitsService) {
    this.acquisitionsUnitsService = acquisitionsUnitsService;
  }

  @Override
  public Future<Void> checkOperationsRestrictions(List<String> unitIds, Set<ProtectedOperationType> operations, String lang, Context context, Map<String, String> headers) {
    logger.debug("checkOperationsRestrictions:: Trying to check operation restrictions by unitIds: {} and '{}' operations", unitIds, operations.size());
    Promise<Void> promise = Promise.promise();
    if (CollectionUtils.isNotEmpty(unitIds)) {
      return getUnitsByIds(unitIds, lang, context, headers)
        .compose(units -> {
          if (unitIds.size() == units.size()) {
            logger.info("checkOperationsRestrictions:: equal unitIds size '{}' and fetched units size '{}'", unitIds.size(), units.size());
            List<AcquisitionsUnit> activeUnits = units.stream()
              .filter(unit -> !unit.getIsDeleted())
              .collect(Collectors.toList());
            if (!activeUnits.isEmpty() && applyMergingStrategy(activeUnits, operations)) {
              return verifyUserIsMemberOfOrganizationUnits(extractUnitIds(activeUnits), headers.get(OKAPI_USERID_HEADER), lang, context, headers);
            }
          } else {
            logger.warn("checkOperationsRestrictions:: mismatch between unitIds size '{}' and fetched units size '{}'", unitIds.size(), units.size());
            throw new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), buildUnitsNotFoundError(unitIds, extractUnitIds(units)));
          }
          promise.complete();
          return promise.future();
        });
    } else {
      logger.warn("checkOperationsRestrictions:: unitIds is empty");
      promise.complete();
      return promise.future();
    }
  }

  @Override
  public Future<Void> validateAcqUnitsOnUpdate(Organization updatedOrg, Organization currentOrg, String lang, Context context, Map<String, String> headers) {
    logger.debug("validateAcqUnitsOnUpdate:: Trying to verify acquisition units for updating between updateOrg '{}' and currentOrg '{}'", updatedOrg, currentOrg);
    List<String> updatedAcqUnitIds = updatedOrg.getAcqUnitIds();
    List<String> currentAcqUnitIds = currentOrg.getAcqUnitIds();

    verifyUserHasManagePermission(updatedAcqUnitIds, currentAcqUnitIds, getProvidedPermissions(headers));
    return verifyIfUnitsAreActive(ListUtils.subtract(updatedAcqUnitIds, currentAcqUnitIds), lang, context, headers)
      .compose(ok -> checkOperationsRestrictions(currentAcqUnitIds, Collections.singleton(UPDATE), lang, context, headers));
  }

  private Future<List<AcquisitionsUnit>> getUnitsByIds(List<String> unitIds, String lang, Context context, Map<String, String> headers) {
    logger.debug("getUnitsByIds:: Trying to get units by unitIds: {}", unitIds);
    Promise<List<AcquisitionsUnit>> promise = Promise.promise();
    String query = combineCqlExpressions("and", ALL_UNITS_CQL, convertIdsToCqlQuery(unitIds));
    return acquisitionsUnitsService.getAcquisitionsUnits(query, 0, Integer.MAX_VALUE, lang, context, headers)
      .compose(ids -> {
        promise.complete(ids.getAcquisitionsUnits());
      return promise.future();
      });
  }

  private boolean applyMergingStrategy(List<AcquisitionsUnit> units, Set<ProtectedOperationType> operations) {
    return units.stream().allMatch(unit -> operations.stream().anyMatch(operation -> operation.isProtected(unit)));
  }

  private Future<Void> verifyUserIsMemberOfOrganizationUnits(List<String> unitIdsAssignedToOrg, String currentUserId, String lang, Context context, Map<String, String> headers) {
    logger.debug("verifyUserIsMemberOfOrganizationUnits:: Trying to verify user '{}' is member of organization units: {}", currentUserId, unitIdsAssignedToOrg);
    String query = String.format("userId==%s AND %s", currentUserId, convertIdsToCqlQuery(unitIdsAssignedToOrg, ACQUISITIONS_UNIT_ID, true));
    Promise<AcquisitionsUnitMembershipCollection> promise = Promise.promise();
    Promise<Void> promise1 = Promise.promise();
    return acquisitionsUnitsService.getAcquisitionsUnitsMemberships(query, 0, Integer.MAX_VALUE, lang, context, headers)
      .compose(unit -> {
        if (unit.getTotalRecords() == 0) {
          promise1.fail(new HttpException(HttpStatus.HTTP_FORBIDDEN.toInt(), USER_HAS_NO_PERMISSIONS));
        }
        else {
        promise1.complete();}
        return promise1.future();
      })
      .onFailure(t -> {
        logger.error("Error while getting user's '{}' units memberships by unIdsAssignedToOrg '{}'", currentUserId, unitIdsAssignedToOrg, t);
        promise.fail(new CompletionException(t));
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
  public Future<Void> verifyIfUnitsAreActive(List<String> acqUnitIds, String lang, Context context, Map<String, String> headers) {
    logger.debug("verifyIfUnitsAreActive:: Trying to verify if units are active by acqUnitsIds: {}", acqUnitIds);
    Promise<Void> promise = Promise.promise();
    if (acqUnitIds.isEmpty()) {
      promise.complete();
      return promise.future();
    }
    return getUnitsByIds(acqUnitIds, lang, context, headers).compose(units -> {
      List<String> activeUnitIds = units.stream()
        .filter(unit -> !unit.getIsDeleted())
        .map(AcquisitionsUnit::getId)
        .collect(Collectors.toList());

      if (acqUnitIds.size() != activeUnitIds.size()) {
        promise.fail(new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), buildUnitsNotFoundError(acqUnitIds, activeUnitIds)));
      }
      promise.complete();
      return promise.future();
    }).onFailure(t ->
      promise.fail(new CompletionException(t))
    );
  }
}
