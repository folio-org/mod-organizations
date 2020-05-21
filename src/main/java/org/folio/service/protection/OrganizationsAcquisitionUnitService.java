package org.folio.service.protection;

import static org.folio.util.ResourcePathResolver.*;

import io.vertx.core.Context;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.acq.model.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.AcquisitionsUnitMembershipCollection;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.service.BaseService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class OrganizationsAcquisitionUnitService extends BaseService implements AcquisitionUnitsService {

  private static final String GET_UNITS_BY_QUERY = resourcesPath(ACQUISITIONS_UNITS) + SEARCH_PARAMS;
  private static final String GET_UNITS_MEMBERSHIPS_BY_QUERY = resourcesPath(ACQUISITIONS_MEMBERSHIPS) + SEARCH_PARAMS;

  @Override
  public CompletableFuture<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, String lang, Context context, Map<String, String> headers) {
    CompletableFuture<AcquisitionsUnitCollection> future = new VertxCompletableFuture<>(context);
    HttpClientInterface client = getHttpClient(headers);
    try {
      if (StringUtils.isEmpty(query)) {
        query = ACTIVE_UNITS_CQL;
      } else if (!query.contains(IS_DELETED_PROP)) {
        query = combineCqlExpressions("and", ACTIVE_UNITS_CQL, query);
      }

      String endpoint = String.format(GET_UNITS_BY_QUERY, limit, offset, buildQuery(query, logger), lang);
      handleGetRequest(endpoint, client, context, headers, logger)
        .thenApply(jsonUnits -> jsonUnits.mapTo(AcquisitionsUnitCollection.class))
        .thenAccept(future::complete)
        .exceptionally(t -> {
          future.completeExceptionally(t.getCause());
          return null;
        });
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  @Override
  public CompletableFuture<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit, String lang, Context context, Map<String, String> headers) {
    CompletableFuture<AcquisitionsUnitMembershipCollection> future = new VertxCompletableFuture<>(context);
    HttpClientInterface client = getHttpClient(headers);
    try {
      String endpoint = String.format(GET_UNITS_MEMBERSHIPS_BY_QUERY, limit, offset, buildQuery(query, logger), lang);
      handleGetRequest(endpoint, client, context, headers, logger)
        .thenApply(jsonUnitsMembership -> jsonUnitsMembership.mapTo(AcquisitionsUnitMembershipCollection.class))
        .thenAccept(future::complete)
        .exceptionally(t -> {
          future.completeExceptionally(t.getCause());
          return null;
        });
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
    return future;
  }
}
