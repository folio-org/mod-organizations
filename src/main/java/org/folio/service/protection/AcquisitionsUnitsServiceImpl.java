package org.folio.service.protection;

import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.acq.model.AcquisitionsUnit;
import org.folio.rest.acq.model.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.AcquisitionsUnitMembership;
import org.folio.rest.acq.model.AcquisitionsUnitMembershipCollection;
import org.folio.rest.client.RequestContext;
import org.folio.service.BaseService;
import org.springframework.stereotype.Service;

import io.vertx.core.Context;
import one.util.streamex.StreamEx;

@Service
public class AcquisitionsUnitsServiceImpl extends BaseService implements AcquisitionsUnitsService {
  protected final Logger logger = LogManager.getLogger(this.getClass());

  @Override
  public Future<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, String lang, Context context, Map<String, String> headers) {
    logger.debug("getAcquisitionsUnits:: Trying to get acquisition units with query: {}, offset: {}, limit: {}", query, offset, limit);
    RequestContext requestContext = new RequestContext(context, headers);
    if (StringUtils.isEmpty(query)) {
      query = ACTIVE_UNITS_CQL;
    } else if (!query.contains(IS_DELETED_PROP)) {
      query = combineCqlExpressions("and", ACTIVE_UNITS_CQL, query);
    }
    String endpoint = String.format(GET_UNITS_BY_QUERY, limit, offset, buildQuery(query, logger), lang);
    return handleGetRequest(endpoint, requestContext, logger)
      .map(jsonUnits -> jsonUnits.mapTo(AcquisitionsUnitCollection.class));
  }

  @Override
  public Future<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit, String lang, Context context, Map<String, String> headers) {
    logger.debug("getAcquisitionsUnitsMemberships:: Trying to get acquisition units memberships with query: {}, offset: {}, limit: {}", query, offset, limit);
    RequestContext requestContext = new RequestContext(context, headers);
    Promise<AcquisitionsUnitMembershipCollection> promise = Promise.promise();
    String endpoint = String.format(GET_UNITS_MEMBERSHIPS_BY_QUERY, limit, offset, buildQuery(query, logger), lang);
    return handleGetRequest(endpoint, requestContext, logger)
      .compose(jsonUnitsMembership -> {
        promise.complete(jsonUnitsMembership.mapTo(AcquisitionsUnitMembershipCollection.class));
        return promise.future();
      })
      .onFailure(t -> {
        if (Objects.nonNull(t)) {
          logger.warn("getAcquisitionsUnitsMemberships:: Error getting acquisition units memberships by endpoint: {}", endpoint, t);
          promise.fail(new CompletionException(t));
        }
      });
  }

  @Override
  public Future<String> buildAcqUnitsCqlClause(String query, int offset, int limit, String lang, Context context, Map<String, String> headers) {
    return getAcqUnitIdsForSearch(lang, context, headers)
      .compose(ids -> {
        if (ids.isEmpty()) {
          return Future.succeededFuture(NO_ACQ_UNIT_ASSIGNED_CQL);
        }
        return Future.succeededFuture(String.format("%s or (%s)", convertIdsToCqlQuery(ids, ACQUISITIONS_UNIT_IDS, false), NO_ACQ_UNIT_ASSIGNED_CQL));
      });
  }

  private Future<List<String>> getAcqUnitIdsForSearch(String lang, Context context, Map<String, String> headers) {
    return getAcqUnitIdsForUser(headers.get(OKAPI_USERID_HEADER), lang, context, headers)
      .compose(unitsForUser -> getOpenForReadAcqUnitIds(lang, context, headers)
      .map(unitsAllowRead -> StreamEx.of(unitsForUser, unitsAllowRead)
        .flatCollection(strings -> strings)
        .distinct()
        .toList()));
  }

  private Future<List<String>> getAcqUnitIdsForUser(String userId, String lang, Context context, Map<String, String> headers) {
    logger.debug("getAcqUnitIdsForUser:: Trying to get acquisition unit ids with userId: {}", userId);
    return getAcquisitionsUnitsMemberships("userId==" + userId, 0, Integer.MAX_VALUE, lang, context, headers)
      .map(memberships -> {
        List<String> ids = memberships.getAcquisitionsUnitMemberships()
          .stream()
          .map(AcquisitionsUnitMembership::getAcquisitionsUnitId)
          .collect(Collectors.toList());
          logger.debug("getAcqUnitIdsForUser:: User belongs to {} acq units: {}", ids.size(), StreamEx.of(ids).joining(", "));
        return ids;
      });
  }

  private Future<List<String>> getOpenForReadAcqUnitIds(String lang, Context context, Map<String, String> headers) {
    logger.debug("getOpenForReadAcqUnitIds:: Trying to get acquisition unit ids with open status");
    return getAcquisitionsUnits("protectRead==false", 0, Integer.MAX_VALUE, lang, context, headers)
      .map(units -> {
        List<String> ids = units.getAcquisitionsUnits()
          .stream()
          .map(AcquisitionsUnit::getId)
          .collect(Collectors.toList());
        if (logger.isDebugEnabled()) {
          logger.debug("getOpenForReadAcqUnitIds:: {} acq units with 'protectRead==false' are found: {}", ids.size(), StreamEx.of(ids).joining(", "));
        }
        return ids;
      });
  }
}
