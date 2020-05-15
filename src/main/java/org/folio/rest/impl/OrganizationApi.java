package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.config.Constants.OKAPI_URL;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.Vertx;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.resource.Organizations;
import org.folio.service.organization.OrganizationService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class OrganizationApi extends BaseApi implements Organizations {

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
    organizationService.createOrganization(entity, vertxContext, okapiHeaders)
      .thenAccept(organization -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL),
          String.format(ORGANIZATIONS_LOCATION_PREFIX, organization.getId()), organization))))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void getOrganizationsOrganizationsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    organizationService.getOrganizationById(id, vertxContext, okapiHeaders)
      .thenAccept(organization -> asyncResultHandler.handle(succeededFuture(buildOkResponse(organization))))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void putOrganizationsOrganizationsById(String id, String lang, Organization entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    organizationService.updateOrganizationById(id, entity, vertxContext, okapiHeaders)
      .thenAccept(vVoid -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void deleteOrganizationsOrganizationsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    organizationService.deleteOrganizationById(id, vertxContext, okapiHeaders)
      .thenAccept(vVoid -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .exceptionally(t -> handleErrorResponse(asyncResultHandler, t));
  }
}
