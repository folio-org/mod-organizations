package org.folio.service.organization;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.exception.ErrorCodes.ACCOUNT_NUMBER_MUST_BE_UNIQUE;
import static org.folio.exception.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.service.protection.ProtectedOperationType.READ;
import static org.folio.util.ResourcePathResolver.ORGANIZATIONS;
import static org.folio.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.util.ResourcePathResolver.resourcesPath;
import static org.folio.util.RestUtils.SEARCH_PARAMS;
import static org.folio.util.RestUtils.buildQuery;
import static org.folio.util.RestUtils.combineCqlExpressions;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.exception.HttpException;
import org.folio.rest.client.RequestContext;
import org.folio.rest.client.RestClient;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.model.OrganizationCollection;
import org.folio.service.protection.AcquisitionsUnitsService;
import org.folio.service.protection.ProtectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

@Service
public class OrganizationStorageService implements OrganizationService {

  private static final Logger logger = LogManager.getLogger(OrganizationStorageService.class);
  public static final String GET_ORGANIZATIONS_BY_QUERY = resourcesPath(ORGANIZATIONS) + SEARCH_PARAMS;

  private ProtectionService protectionService;

  private RestClient restClient;
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
    return restClient.post(organization, resourcesPath(ORGANIZATIONS), Organization.class, requestContext);
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
    return restClient.get(resourceByIdPath(ORGANIZATIONS, id), Organization.class, requestContext)
      .compose(organization -> protectionService
        .checkOperationsRestrictions(organization.getAcqUnitIds(), Collections.singleton(READ), context, headers)
          .map(organization)
          .onFailure(t -> logger.warn("Operation is restricted by acquisition units for organization id: {}", organization.getId(), t)))
      .onFailure(t -> logger.error("Error loading organization with id: {}", id, t));
  }

  @Override
  public Future<OrganizationCollection> getOrganizationCollection(int offset, int limit, String query, String lang,
      Context context, Map<String, String> headers) {
    logger.debug("getOrganizationCollection:: Trying to get organization collection with query: {}, offset: {}, limit: {}", query, offset, limit);
    RequestContext requestContext = new RequestContext(context, headers);
    return acquisitionsUnitsService.buildAcqUnitsCqlClause(query, offset, limit, context, headers)
      .compose(clause -> {
        String endpoint = StringUtils.isEmpty(query) ?
          String.format(GET_ORGANIZATIONS_BY_QUERY, limit, offset, buildQuery(clause), lang) :
          String.format(GET_ORGANIZATIONS_BY_QUERY, limit, offset, buildQuery(combineCqlExpressions("and", clause, query)), lang);
        return restClient.get(endpoint, OrganizationCollection.class, requestContext);
      })
      .onFailure( t -> logger.warn("Error loading organization collection with query: {}, offset: {}, limit: {}", query, offset, limit, t));
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
      return Future.failedFuture(new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError()));
    }
    if (isSameAccountNumbers(updatedOrganization)) {
      logger.warn("updateOrganization:: Account number of organization '{}' is not unique", id);
      return Future.failedFuture(new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(),
        ACCOUNT_NUMBER_MUST_BE_UNIQUE.toError()));
    }
    return restClient.get(resourceByIdPath(ORGANIZATIONS, id), Organization.class, requestContext)
      .compose(existingOrganization -> protectionService.validateAcqUnitsOnUpdate(updatedOrganization, existingOrganization, context, headers)
      .compose(ok -> restClient.put(resourceByIdPath(ORGANIZATIONS, updatedOrganization.getId()), JsonObject.mapFrom(updatedOrganization), requestContext)));
  }

  @Override
  public Future<Void> deleteOrganizationById(String id, Context context, Map<String, String> headers) {
    logger.debug("deleteOrganizationById:: Trying to delete organization by id: {}", id);
    RequestContext requestContext = new RequestContext(context, headers);
    return restClient.delete(resourceByIdPath(ORGANIZATIONS, id), requestContext);
  }

  @Autowired
  public void setProtectionService(ProtectionService protectionService) {
    this.protectionService = protectionService;
  }

  @Autowired
  public void setRestClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Autowired
  public void setAcquisitionsUnitsService(AcquisitionsUnitsService acquisitionsUnitsService) {
    this.acquisitionsUnitsService = acquisitionsUnitsService;
  }
}
