package org.folio.service.protection;

import static org.folio.exception.ErrorCodes.ORGANIZATION_UNITS_NOT_FOUND;
import static org.folio.exception.ErrorCodes.USER_HAS_NO_PERMISSIONS;
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;

import io.vertx.core.Context;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.folio.HttpStatus;
import org.folio.exception.HttpException;
import org.folio.rest.acq.model.AcquisitionsUnit;
import org.folio.rest.jaxrs.model.Error;
import org.folio.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class OrganizationsProtectionService extends BaseService implements ProtectionService {

  public static final String ACQUISITIONS_UNIT_IDS = "acqUnitIds";

  private AcquisitionUnitsService acquisitionUnitsService;
  private final List<AcquisitionsUnit> fetchedUnits = new ArrayList<>();

  @Autowired
  public void setAcquisitionUnitsService(AcquisitionUnitsService acquisitionUnitsService) {
    this.acquisitionUnitsService = acquisitionUnitsService;
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
    return acquisitionUnitsService.getAcquisitionsUnits(query, 0, Integer.MAX_VALUE, lang, context, headers)
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
    return acquisitionUnitsService.getAcquisitionsUnitsMemberships(query, 0, 0, lang, context, headers)
      .thenAccept(unit -> {
        if (unit.getTotalRecords() == 0) {
          throw new HttpException(HttpStatus.HTTP_FORBIDDEN.toInt(), USER_HAS_NO_PERMISSIONS);
        }
      });
  }

  private List<String> extractUnitIds(List<AcquisitionsUnit> activeUnits) {
    return activeUnits.stream().map(AcquisitionsUnit::getId).collect(Collectors.toList());
  }

  private Error buildUnitsNotFoundError(List<String> expectedUnitIds, List<String> availableUnitIds) {
    List<String> missingUnitIds = ListUtils.subtract(expectedUnitIds, availableUnitIds);
    return ORGANIZATION_UNITS_NOT_FOUND.toError().withAdditionalProperty(ACQUISITIONS_UNIT_IDS, missingUnitIds);
  }
}
