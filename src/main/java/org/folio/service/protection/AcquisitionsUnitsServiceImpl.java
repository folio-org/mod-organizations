package org.folio.service.protection;

import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.util.RestUtils.ACQUISITIONS_UNIT_IDS;
import static org.folio.util.RestUtils.ACTIVE_UNITS_CQL;
import static org.folio.util.RestUtils.GET_UNITS_BY_QUERY;
import static org.folio.util.RestUtils.GET_UNITS_MEMBERSHIPS_BY_QUERY;
import static org.folio.util.RestUtils.IS_DELETED_PROP;
import static org.folio.util.RestUtils.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.folio.util.RestUtils.buildQuery;
import static org.folio.util.RestUtils.combineCqlExpressions;
import static org.folio.util.RestUtils.convertIdsToCqlQuery;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.acq.model.AcquisitionsUnit;
import org.folio.rest.acq.model.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.AcquisitionsUnitMembership;
import org.folio.rest.acq.model.AcquisitionsUnitMembershipCollection;
import org.folio.rest.client.RequestContext;
import org.folio.rest.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.vertx.core.Context;
import io.vertx.core.Future;
import one.util.streamex.StreamEx;

@Service
public class AcquisitionsUnitsServiceImpl implements AcquisitionsUnitsService {
  protected final Logger logger = LogManager.getLogger(this.getClass());

  private RestClient restClient;

  @Override
  public Future<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, Context context, Map<String, String> headers) {
    logger.debug("getAcquisitionsUnits:: Trying to get acquisition units with query: {}, offset: {}, limit: {}", query, offset, limit);
    RequestContext requestContext = new RequestContext(context, headers);
    if (StringUtils.isEmpty(query)) {
      query = ACTIVE_UNITS_CQL;
    } else if (!query.contains(IS_DELETED_PROP)) {
      query = combineCqlExpressions("and", ACTIVE_UNITS_CQL, query);
    }
    String endpoint = String.format(GET_UNITS_BY_QUERY, limit, offset, buildQuery(query));
    return restClient.get(endpoint, AcquisitionsUnitCollection.class, requestContext);
  }

  @Override
  public Future<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit, Context context, Map<String, String> headers) {
    logger.debug("getAcquisitionsUnitsMemberships:: Trying to get acquisition units memberships with query: {}, offset: {}, limit: {}", query, offset, limit);
    RequestContext requestContext = new RequestContext(context, headers);
    String endpoint = String.format(GET_UNITS_MEMBERSHIPS_BY_QUERY, limit, offset, buildQuery(query));
    return restClient.get(endpoint, AcquisitionsUnitMembershipCollection.class, requestContext)
      .onFailure(t -> logger.warn("getAcquisitionsUnitsMemberships:: Error getting acquisition units memberships", t));

  }

  @Override
  public Future<String> buildAcqUnitsCqlClause(String query, int offset, int limit, Context context, Map<String, String> headers) {
    return getAcqUnitIdsForSearch(context, headers)
      .compose(ids -> {
        if (ids.isEmpty()) {
          return Future.succeededFuture(NO_ACQ_UNIT_ASSIGNED_CQL);
        }
        return Future.succeededFuture(String.format("%s or (%s)", convertIdsToCqlQuery(ids, ACQUISITIONS_UNIT_IDS, false), NO_ACQ_UNIT_ASSIGNED_CQL));
      });
  }

  private Future<List<String>> getAcqUnitIdsForSearch(Context context, Map<String, String> headers) {
    return getAcqUnitIdsForUser(headers.get(OKAPI_USERID_HEADER), context, headers)
      .compose(unitsForUser -> getOpenForReadAcqUnitIds(context, headers)
      .map(unitsAllowRead -> StreamEx.of(unitsForUser, unitsAllowRead)
        .flatCollection(strings -> strings)
        .distinct()
        .toList()));
  }

  private Future<List<String>> getAcqUnitIdsForUser(String userId, Context context, Map<String, String> headers) {
    logger.debug("getAcqUnitIdsForUser:: Trying to get acquisition unit ids with userId: {}", userId);
    return getAcquisitionsUnitsMemberships("userId==" + userId, 0, Integer.MAX_VALUE, context, headers)
      .map(memberships -> {
        List<String> ids = memberships.getAcquisitionsUnitMemberships()
          .stream()
          .map(AcquisitionsUnitMembership::getAcquisitionsUnitId)
          .collect(Collectors.toList());
          logger.debug("getAcqUnitIdsForUser:: User belongs to {} acq units: {}", ids.size(), StreamEx.of(ids).joining(", "));
        return ids;
      });
  }

  private Future<List<String>> getOpenForReadAcqUnitIds(Context context, Map<String, String> headers) {
    logger.debug("getOpenForReadAcqUnitIds:: Trying to get acquisition unit ids with open status");
    return getAcquisitionsUnits("protectRead==false", 0, Integer.MAX_VALUE, context, headers)
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

  @Autowired
  public void setRestClient(RestClient restClient) {
    this.restClient = restClient;
  }
}
