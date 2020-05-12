package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.config.Constants.OKAPI_URL;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.config.ApplicationConfig;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.resource.Organizations;
import org.folio.service.organization.OrganizationService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class OrganizationApi extends BaseApi implements Organizations {

  private final String ORGANIZATIONS_LOCATION_PREFIX = "/organizations/organizations/%s";

  private final OrganizationService organizationService;

  public OrganizationApi() {
    ApplicationContext ctx = new AnnotationConfigApplicationContext(ApplicationConfig.class);
    organizationService = ctx.getBean(OrganizationService.class);
  }

  @Override
  public void getOrganizationsOrganizations(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
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
