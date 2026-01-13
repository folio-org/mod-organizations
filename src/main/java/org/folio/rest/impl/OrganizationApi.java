package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.config.Constants.OKAPI_URL;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.resource.Organizations;
import org.folio.service.organization.OrganizationService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class OrganizationApi extends BaseApi implements Organizations {

  private static final Logger logger = LogManager.getLogger(OrganizationApi.class);
  private static final String ORGANIZATIONS_LOCATION_PREFIX = "/organizations/organizations/%s";

  @Autowired
  private OrganizationService organizationService;

  public OrganizationApi() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getOrganizationsOrganizations(String query, String totalRecords, int offset, int limit, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    organizationService.getOrganizationCollection(offset, limit, query, vertxContext, okapiHeaders)
      .onSuccess(organizations -> asyncResultHandler.handle(succeededFuture(buildOkResponse(organizations))))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  @Validate
  public void postOrganizationsOrganizations(Organization entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to create organization, id: {}", entity.getId());
    organizationService.createOrganization(entity, vertxContext, okapiHeaders)
      .onSuccess(organization -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL),
          String.format(ORGANIZATIONS_LOCATION_PREFIX, organization.getId()), organization))))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  @Validate
  public void getOrganizationsOrganizationsById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to get organization with id: {}", id);
    organizationService.getOrganizationById(id, vertxContext, okapiHeaders)
      .onSuccess(organization -> asyncResultHandler.handle(succeededFuture(buildOkResponse(organization))))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  @Validate
  public void putOrganizationsOrganizationsById(String id, Organization entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to update organization with id: {}", id);
    organizationService.updateOrganizationById(id, entity, vertxContext, okapiHeaders)
      .onSuccess(vVoid -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  @Validate
  public void deleteOrganizationsOrganizationsById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to delete organization by id: {}", id);
    organizationService.deleteOrganizationById(id, vertxContext, okapiHeaders)
      .onSuccess(vVoid -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }
}
