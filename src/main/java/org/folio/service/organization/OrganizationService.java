package org.folio.service.organization;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.model.OrganizationCollection;

import io.vertx.core.Context;

public interface OrganizationService {

  /**
   * This method creates {@link Organization}
   *
   * @param organization organization
   * @param context      Vert.X context
   * @param headers      OKAPI headers
   * @return created {@link Organization}
   */
  CompletableFuture<Organization> createOrganization(Organization organization, Context context, Map<String, String> headers);

  /**
   * This method returns {@link Organization} by ID
   *
   * @param id      organization's id
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return {@link Organization}
   */
  CompletableFuture<Organization> getOrganizationById(String id, String lang, Context context, Map<String, String> headers);

  /**
   * This method returns {@link OrganizationCollection} by query
   *
   * @param offset  offset
   * @param limit   limit
   * @param lang    language
   * @param query   query
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return collection of organizations {@link OrganizationCollection}
   */
  CompletableFuture<OrganizationCollection> getOrganizationCollection(int offset, int limit, String lang, String query,
      Context context, Map<String, String> headers);

  /**
   * This method updates {@link Organization} by ID
   *
   * @param id      updated organization's id
   * @param entity  updated {@link Organization} entity
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return void completable future
   */
  CompletableFuture<Void> updateOrganizationById(String id, Organization entity, Context context, Map<String, String> headers);

  /**
   * This method deletes {@link Organization} by ID
   *
   * @param id      deleted organization's id
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return void completable future
   */
  CompletableFuture<Void> deleteOrganizationById(String id, Context context, Map<String, String> headers);
}
