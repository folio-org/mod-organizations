package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.config.Constants.OKAPI_URL;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
  public void getOrganizationsOrganizations(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    organizationService.getOrganizationCollection(offset, limit, query, lang, vertxContext, okapiHeaders)
      .thenAccept(organizations -> asyncResultHandler.handle(succeededFuture(buildOkResponse(organizations))))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void postOrganizationsOrganizations(String lang, Organization entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to create organization with name: {}", entity.getName());
    organizationService.createOrganization(entity, vertxContext, okapiHeaders)
      .thenAccept(organization -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL),
          String.format(ORGANIZATIONS_LOCATION_PREFIX, organization.getId()), organization))))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void getOrganizationsOrganizationsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to get organization with id: {}", id);
    organizationService.getOrganizationById(id, lang, vertxContext, okapiHeaders)
      .thenAccept(organization -> asyncResultHandler.handle(succeededFuture(buildOkResponse(organization))))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void putOrganizationsOrganizationsById(String id, String lang, Organization entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to update organization with id: {}", id);
    organizationService.updateOrganizationById(id, entity, lang, vertxContext, okapiHeaders)
      .thenAccept(vVoid -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void deleteOrganizationsOrganizationsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to delete organization by id: {}", id);
    organizationService.deleteOrganizationById(id, vertxContext, okapiHeaders)
      .thenAccept(vVoid -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }
}
