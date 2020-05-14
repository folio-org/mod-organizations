package org.folio.service.organization;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.exception.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.util.ResourcePathResolver.ORGANIZATIONS;
import static org.folio.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.util.ResourcePathResolver.resourcesPath;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.folio.exception.HttpException;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.model.OrganizationCollection;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.service.BaseService;
import org.springframework.stereotype.Service;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

@Service
public class OrganizationStorageService extends BaseService implements OrganizationService {

  private static final String GET_ORGANIZATIONS_BY_QUERY = resourcesPath(ORGANIZATIONS) + SEARCH_PARAMS;

  @Override
  public CompletableFuture<Organization> createOrganization(Organization organization, Context context,
      Map<String, String> headers) {
    HttpClientInterface client = getHttpClient(headers);

    return handlePostRequest(JsonObject.mapFrom(organization), resourcesPath(ORGANIZATIONS), client, context, headers, logger)
      .thenApply(id -> JsonObject.mapFrom(organization.withId(id))
        .mapTo(Organization.class))
      .handle((org, t) -> {
        client.closeClient();
        if (Objects.nonNull(t)) {
          throw new CompletionException(t.getCause());
        }
        return org;
      });
  }

  @Override
  public CompletableFuture<Organization> getOrganizationById(String id, Context context, Map<String, String> headers) {
    HttpClientInterface client = getHttpClient(headers);

    return handleGetRequest(resourceByIdPath(ORGANIZATIONS, id), client, context, headers, logger)
      .thenApply(json -> json.mapTo(Organization.class))
      .handle((org, t) -> {
        client.closeClient();
        if (Objects.nonNull(t)) {
          throw new CompletionException(t.getCause());
        }
        return org;
      });
  }

  @Override
  public CompletableFuture<OrganizationCollection> getOrganizationCollection(int offset, int limit, String query, String lang,
      Context context, Map<String, String> headers) {
    CompletableFuture<OrganizationCollection> future = new VertxCompletableFuture<>(context);
    HttpClientInterface client = getHttpClient(headers);

    String endpoint = String.format(GET_ORGANIZATIONS_BY_QUERY, limit, offset, buildQuery(query, logger), lang);
    handleGetRequest(endpoint, client, context, headers, logger)
      .thenCompose(json -> VertxCompletableFuture.supplyBlockingAsync(context, () -> json.mapTo(OrganizationCollection.class)))
      .handle((collection, t) -> {
        client.closeClient();
        if (t != null) {
          future.completeExceptionally(t.getCause());
        } else {
          future.complete(collection);
        }
        return null;
      });
    return future;
  }

  @Override
  public CompletableFuture<Void> updateOrganizationById(String id, Organization entity, Context context,
      Map<String, String> headers) {
    if (isEmpty(entity.getId())) {
      entity.setId(id);
    } else if (!id.equals(entity.getId())) {
      throw new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError());
    }
    HttpClientInterface client = getHttpClient(headers);

    return handlePutRequest(resourceByIdPath(ORGANIZATIONS, entity.getId()), JsonObject.mapFrom(entity), client, context, headers,
        logger).handle((org, t) -> {
          client.closeClient();
          if (Objects.nonNull(t)) {
            throw new CompletionException(t.getCause());
          }
          return null;
        });
  }

  @Override
  public CompletableFuture<Void> deleteOrganizationById(String id, Context context, Map<String, String> headers) {
    HttpClientInterface client = getHttpClient(headers);
    return handleDeleteRequest(resourceByIdPath(ORGANIZATIONS, id), client, context, headers, logger).handle((org, t) -> {
      client.closeClient();
      if (Objects.nonNull(t)) {
        throw new CompletionException(t.getCause());
      }
      return null;
    });
  }
}
