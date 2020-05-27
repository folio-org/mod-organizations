package org.folio.service.protection;

import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;

import io.vertx.core.Context;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.AcquisitionsUnit;
import org.folio.rest.acq.model.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.AcquisitionsUnitMembership;
import org.folio.rest.acq.model.AcquisitionsUnitMembershipCollection;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.service.BaseService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Service
public class AcquisitionsUnitsServiceImpl extends BaseService implements AcquisitionsUnitsService {

  @Override
  public CompletableFuture<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, String lang, Context context, Map<String, String> headers) {
    HttpClientInterface client = getHttpClient(headers);
    if (StringUtils.isEmpty(query)) {
      query = ACTIVE_UNITS_CQL;
    } else if (!query.contains(IS_DELETED_PROP)) {
      query = combineCqlExpressions("and", ACTIVE_UNITS_CQL, query);
    }
    String endpoint = String.format(GET_UNITS_BY_QUERY, limit, offset, buildQuery(query, logger), lang);
    return handleGetRequest(endpoint, client, context, headers, logger)
      .thenApply(jsonUnits -> jsonUnits.mapTo(AcquisitionsUnitCollection.class))
      .handle((acqUnitsColl, t) -> {
        client.closeClient();
        if (Objects.nonNull(t)) {
          throw new CompletionException(t.getCause());
        }
        return acqUnitsColl;
      });
  }

  @Override
  public CompletableFuture<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit, String lang, Context context, Map<String, String> headers) {
    HttpClientInterface client = getHttpClient(headers);
    String endpoint = String.format(GET_UNITS_MEMBERSHIPS_BY_QUERY, limit, offset, buildQuery(query, logger), lang);
    return handleGetRequest(endpoint, client, context, headers, logger)
      .thenApply(jsonUnitsMembership -> jsonUnitsMembership.mapTo(AcquisitionsUnitMembershipCollection.class))
      .handle((acqUnitsMembershipColl, t) -> {
        client.closeClient();
        if (Objects.nonNull(t)) {
          throw new CompletionException(t.getCause());
        }
        return acqUnitsMembershipColl;
      });
  }

  @Override
  public CompletableFuture<String> buildAcqUnitsCqlClause(String query, int offset, int limit, String lang, Context context, Map<String, String> headers) {
    return getAcqUnitIdsForSearch(lang, context, headers)
      .thenApply(ids -> {
        if (ids.isEmpty()) {
          return NO_ACQ_UNIT_ASSIGNED_CQL;
        }
        return String.format("%s or (%s)", convertIdsToCqlQuery(ids, ACQUISITIONS_UNIT_IDS, false), NO_ACQ_UNIT_ASSIGNED_CQL);
      });
  }

  private CompletableFuture<List<String>> getAcqUnitIdsForSearch(String lang, Context context, Map<String, String> headers) {
    return getAcqUnitIdsForUser(headers.get(OKAPI_USERID_HEADER), lang, context, headers)
      .thenCombine(getOpenForReadAcqUnitIds(lang, context, headers), (unitsForUser, unitsAllowRead) -> StreamEx.of(unitsForUser, unitsAllowRead)
        .flatCollection(strings -> strings)
        .distinct()
        .toList());
  }

  private CompletableFuture<List<String>> getAcqUnitIdsForUser(String userId, String lang, Context context, Map<String, String> headers) {
    return getAcquisitionsUnitsMemberships("userId==" + userId, 0, Integer.MAX_VALUE, lang, context, headers)
      .thenApply(memberships -> {
        List<String> ids = memberships.getAcquisitionsUnitMemberships()
          .stream()
          .map(AcquisitionsUnitMembership::getAcquisitionsUnitId)
          .collect(Collectors.toList());

        if (logger.isDebugEnabled()) {
          logger.debug("User belongs to {} acq units: {}", ids.size(), StreamEx.of(ids).joining(", "));
        }

        return ids;
      });
  }

  private CompletableFuture<List<String>> getOpenForReadAcqUnitIds(String lang, Context context, Map<String, String> headers) {
    return getAcquisitionsUnits("protectRead==false", 0, Integer.MAX_VALUE, lang, context, headers)
      .thenApply(units -> {
        List<String> ids = units.getAcquisitionsUnits()
          .stream()
          .map(AcquisitionsUnit::getId)
          .collect(Collectors.toList());
        if (logger.isDebugEnabled()) {
          logger.debug("{} acq units with 'protectRead==false' are found: {}", ids.size(), StreamEx.of(ids).joining(", "));
        }
        return ids;
    });
  }
}
