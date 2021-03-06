package org.folio.service.organization;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.exception.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.service.protection.ProtectedOperationType.READ;
import static org.folio.util.ResourcePathResolver.ORGANIZATIONS;
import static org.folio.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.util.ResourcePathResolver.resourcesPath;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang3.StringUtils;
import org.folio.exception.HttpException;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.model.OrganizationCollection;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.service.BaseService;
import org.folio.service.protection.AcquisitionsUnitsService;
import org.folio.service.protection.ProtectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

@Service
public class OrganizationStorageService extends BaseService implements OrganizationService {

  public static final String GET_ORGANIZATIONS_BY_QUERY = resourcesPath(ORGANIZATIONS) + SEARCH_PARAMS;

  private ProtectionService protectionService;
  private AcquisitionsUnitsService acquisitionsUnitsService;

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
  public CompletableFuture<Organization> getOrganizationById(String id, String lang, Context context, Map<String, String> headers) {
    HttpClientInterface client = getHttpClient(headers);
    CompletableFuture<Organization> future = new CompletableFuture<>();

    handleGetRequest(resourceByIdPath(ORGANIZATIONS, id), client, headers, logger)
      .thenApply(json -> json.mapTo(Organization.class))
      .thenApply(organization -> protectionService.checkOperationsRestrictions(organization.getAcqUnitIds(), Collections.singleton(READ), lang, context, headers)
        .handle((res, t) -> {
          client.closeClient();
          if (Objects.nonNull(t)) {
            future.completeExceptionally(t.getCause());
          }
          future.complete(organization);
          return null;
        }))
      .exceptionally(throwable -> {
        client.closeClient();
        future.completeExceptionally(throwable);
        return null;
    });
    return future;
  }

  @Override
  public CompletableFuture<OrganizationCollection> getOrganizationCollection(int offset, int limit, String query, String lang,
      Context context, Map<String, String> headers) {
    CompletableFuture<OrganizationCollection> future = new CompletableFuture<>();
    HttpClientInterface client = getHttpClient(headers);
    acquisitionsUnitsService.buildAcqUnitsCqlClause(query, offset, limit, lang, context, headers)
      .thenCompose(clause -> {
        String endpoint = StringUtils.isEmpty(query) ?
          String.format(GET_ORGANIZATIONS_BY_QUERY, limit, offset, buildQuery(clause, logger), lang) :
          String.format(GET_ORGANIZATIONS_BY_QUERY, limit, offset, buildQuery(combineCqlExpressions("and", clause, query), logger), lang);
        return handleGetRequest(endpoint, client, headers, logger);
      })
      .thenApply(json -> json.mapTo(OrganizationCollection.class))
      .handle((collection, t) -> {
        client.closeClient();
        if (Objects.nonNull(t)) {
          future.completeExceptionally(t.getCause());
        } else {
          future.complete(collection);
        }
        return null;
      });
    return future;
  }

  @Override
  public CompletableFuture<Void> updateOrganizationById(String id, Organization updatedOrganization, String lang, Context context,
      Map<String, String> headers) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    if (isEmpty(updatedOrganization.getId())) {
      updatedOrganization.setId(id);
    } else if (!id.equals(updatedOrganization.getId())) {
      future.completeExceptionally(new HttpException(422, MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError()));
      return future;
    }
    HttpClientInterface client = getHttpClient(headers);
    handleGetRequest(resourceByIdPath(ORGANIZATIONS, id), client, headers, logger)
      .thenApply(existingOrganizationJson -> existingOrganizationJson.mapTo(Organization.class))
      .thenCompose(existingOrganization -> protectionService.validateAcqUnitsOnUpdate(updatedOrganization, existingOrganization, lang, context, headers))
      .thenAccept(ok -> handlePutRequest(resourceByIdPath(ORGANIZATIONS, updatedOrganization.getId()), JsonObject.mapFrom(updatedOrganization), client, headers,
        logger).handle((org, t) -> {
        client.closeClient();
        if (Objects.nonNull(t)) {
          future.completeExceptionally(t);
        } else {
          future.complete(null);
        }
        return null;
      }))
    .exceptionally(t -> {
      future.completeExceptionally(t.getCause());
      return null;
    });
    return future;
  }

  @Override
  public CompletableFuture<Void> deleteOrganizationById(String id, Context context, Map<String, String> headers) {
    HttpClientInterface client = getHttpClient(headers);
    return handleDeleteRequest(resourceByIdPath(ORGANIZATIONS, id), client, headers, logger).handle((org, t) -> {
      client.closeClient();
      if (Objects.nonNull(t)) {
        throw new CompletionException(t.getCause());
      }
      return null;
    });
  }

  @Autowired
  public void setProtectionService(ProtectionService protectionService) {
    this.protectionService = protectionService;
  }

  @Autowired
  public void setAcquisitionsUnitsService(AcquisitionsUnitsService acquisitionsUnitsService) {
    this.acquisitionsUnitsService = acquisitionsUnitsService;
  }
}
