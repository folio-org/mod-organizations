package org.folio.rest.impl;

import static org.folio.util.ResourcePathResolver.ORGANIZATIONS;

import java.util.Collections;

import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.model.OrganizationCollection;

import io.vertx.core.json.JsonObject;

public enum TestEntities {
  ORGANIZATION("organizations/organizations", ORGANIZATIONS, getEntity(), getEntityCollection(), Organization.class, "code",
      "TST-ORG");

  String resource;
  String url;
  JsonObject sample;
  JsonObject collection;
  String updatedFieldName;
  Object updatedFieldValue;
  Class clazz;

  TestEntities(String url, String resource, JsonObject sample, JsonObject collection, Class clazz, String updatedFieldName,
      Object updatedFieldValue) {
    this.resource = resource;
    this.url = url;
    this.sample = sample;
    this.collection = collection;
    this.clazz = clazz;
    this.updatedFieldName = updatedFieldName;
    this.updatedFieldValue = updatedFieldValue;
  }

  private static JsonObject getEntity() {
    return JsonObject.mapFrom(new Organization().withStatus(Organization.Status.ACTIVE)
      .withName("Organization")
      .withCode("ORG"));
  }

  private static JsonObject getEntityCollection() {
    return JsonObject.mapFrom(new OrganizationCollection()
      .withOrganizations(Collections.singletonList(new Organization().withStatus(Organization.Status.ACTIVE)
        .withName("Organization")
        .withCode("ORG")))
      .withTotalRecords(1));
  }

  public String getUrl() {
    return url;
  }

  public JsonObject getSample() {
    return sample.copy();
  }

  public Class getClazz() {
    return clazz;
  }

  public String getResource() {
    return resource;
  }

  public JsonObject getCollection() {
    return collection.copy();
  }

  public String getUpdatedFieldName() {
    return updatedFieldName;
  }

  public Object getUpdatedFieldValue() {
    return updatedFieldValue;
  }
}
