package org.folio.service.organization;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.exception.ErrorCodes.ACCOUNT_NUMBER_MUST_BE_UNIQUE;
import static org.folio.exception.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.service.protection.ProtectedOperationType.READ;
import static org.folio.util.ResourcePathResolver.ORGANIZATIONS;
import static org.folio.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.util.ResourcePathResolver.resourcesPath;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.exception.HttpException;
import org.folio.rest.client.RequestContext;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.model.OrganizationCollection;
import org.folio.rest.client.RestClient;
import org.folio.service.protection.AcquisitionsUnitsService;
import org.folio.service.protection.ProtectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

@Service
public class OrganizationStorageService extends RestClient implements OrganizationService {

  private static final Logger logger = LogManager.getLogger(OrganizationStorageService.class);
  public static final String GET_ORGANIZATIONS_BY_QUERY = resourcesPath(ORGANIZATIONS) + SEARCH_PARAMS;

  private ProtectionService protectionService;
  private AcquisitionsUnitsService acquisitionsUnitsService;

  @Override
  public Future<Organization> createOrganization(Organization organization, Context context, Map<String, String> headers) {
    logger.debug("createOrganization:: Trying to create organization with name: {}", organization.getName());
    RequestContext requestContext = new RequestContext(context, headers);

    if (isSameAccountNumbers(organization)) {
      logger.warn("crateOrganization:: Account number of organization '{}' is not unique", organization.getName());
      return Future.failedFuture(new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(),
        ACCOUNT_NUMBER_MUST_BE_UNIQUE.toError()));
    }
    return handlePostRequest(organization, resourcesPath(ORGANIZATIONS), Organization.class, requestContext, logger);
  }

  private boolean isSameAccountNumbers(Organization organization) {
    Set<String> uniqueAccounts = organization.getAccounts().stream()
      .map(Account::getAccountNo)
      .collect(Collectors.toSet());
    logger.debug("isSameAccountNumbers:: match unique accounts numbers size '{}' with accounts size '{}'", uniqueAccounts.size(), organization.getAccounts().size());
    return organization.getAccounts().size() != uniqueAccounts.size();
  }

  @Override
  public Future<Organization> getOrganizationById(String id, String lang, Context context, Map<String, String> headers) {
    logger.debug("getOrganizationById:: Trying to get organization by id: {}", id);
    RequestContext requestContext = new RequestContext(context, headers);
    Promise<Organization> promise = Promise.promise();
    handleGetRequest(resourceByIdPath(ORGANIZATIONS, id), requestContext, logger)
      .compose(json -> Future.succeededFuture(json.mapTo(Organization.class)))
      .compose(organization ->
         protectionService.checkOperationsRestrictions(organization.getAcqUnitIds(), Collections.singleton(READ), lang, context, headers)
      .onSuccess(ok ->
         promise.complete(organization)
      )
      .onFailure(t -> {
        if (Objects.nonNull(t)) {
          logger.warn("Error loading organization with id: {}", organization.getId(), t);
            promise.fail(new CompletionException(t));
          }
        }))
      .onFailure(t -> {
        if (Objects.nonNull(t)) {
          logger.debug("Error loading organization", t);
          promise.fail(new CompletionException(t));
        }
      });

    return promise.future();
  }

  @Override
  public Future<OrganizationCollection> getOrganizationCollection(int offset, int limit, String query, String lang,
      Context context, Map<String, String> headers) {
    logger.debug("getOrganizationCollection:: Trying to get organization collection with query: {}, offset: {}, limit: {}", query, offset, limit);
    RequestContext requestContext = new RequestContext(context, headers);
    Promise<OrganizationCollection> promise = Promise.promise();
    acquisitionsUnitsService.buildAcqUnitsCqlClause(query, offset, limit, lang, context, headers)
      .compose(clause -> {
        String endpoint = StringUtils.isEmpty(query) ?
          String.format(GET_ORGANIZATIONS_BY_QUERY, limit, offset, buildQuery(clause, logger), lang) :
          String.format(GET_ORGANIZATIONS_BY_QUERY, limit, offset, buildQuery(combineCqlExpressions("and", clause, query), logger), lang);
        return handleGetRequest(endpoint, requestContext, logger);
      })
      .compose(json -> {
        OrganizationCollection organizationCollection = json.mapTo(OrganizationCollection.class);
        promise.complete(organizationCollection);
      return promise.future();
      })
      .onFailure( t -> {
        if (Objects.nonNull(t)) {
          logger.warn("Error loading organization collection with query: {}, offset: {}, limit: {}", query, offset, limit, t);
          promise.fail(new CompletionException(t));
        }
      });
    return promise.future();
  }

  @Override
  public Future<Void> updateOrganizationById(String id, Organization updatedOrganization, String lang, Context context,
      Map<String, String> headers) {
    logger.debug("updateOrganization:: Trying to update organization with id: {}", id);
    RequestContext requestContext = new RequestContext(context, headers);

    if (isEmpty(updatedOrganization.getId())) {
      updatedOrganization.setId(id);
    } else if (!id.equals(updatedOrganization.getId())) {
      logger.warn("updateOrganization:: Mismatch between id '{}' in path and request body '{}'", id, updatedOrganization.getId());
      return Future.failedFuture(new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(),
        MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError()));
    }
    if (isSameAccountNumbers(updatedOrganization)) {
      logger.warn("updateOrganization:: Account number of organization '{}' is not unique", id);
      return Future.failedFuture(new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(),
        ACCOUNT_NUMBER_MUST_BE_UNIQUE.toError()));
    }
    return handleGetRequest(resourceByIdPath(ORGANIZATIONS, id), requestContext, logger)
      .compose(existingOrganizationJson -> Future.succeededFuture(existingOrganizationJson.mapTo(Organization.class)))
      .compose(existingOrganization -> protectionService.validateAcqUnitsOnUpdate(updatedOrganization, existingOrganization, lang, context, headers)
      .compose(ok -> handlePutRequest(resourceByIdPath(ORGANIZATIONS, updatedOrganization.getId()), JsonObject.mapFrom(updatedOrganization), logger, requestContext)));
  }

  @Override
  public Future<Void> deleteOrganizationById(String id, Context context, Map<String, String> headers) {
    logger.debug("deleteOrganizationById:: Trying to delete organization by id: {}", id);
    RequestContext requestContext = new RequestContext(context, headers);
    return handleDeleteRequest(resourceByIdPath(ORGANIZATIONS, id), requestContext, logger);
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
