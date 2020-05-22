package org.folio.rest.impl;

import static org.folio.rest.impl.MockServer.ACQ_UNIT_FULL_PROTECTED_ID;
import static org.folio.rest.impl.MockServer.ACQ_UNIT_READ_ONLY_ID;
import static org.folio.rest.impl.MockServer.ORGANIZATION_NO_ACQ_ID;
import static org.folio.rest.impl.MockServer.ORGANIZATION_FULL_PROTECTED_ID;
import static org.folio.rest.impl.MockServer.ORGANIZATION_READ_ONLY_ID;
import static org.folio.util.ResourcePathResolver.ORGANIZATIONS;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.model.OrganizationCollection;

import io.vertx.core.json.JsonObject;

public enum TestEntities {
  ORGANIZATION_NO_ACQ(ORGANIZATION_NO_ACQ_ID, "organizations/organizations", ORGANIZATIONS, getEntity(), getEntityCollection(), Organization.class, "code",
      "TST-ORG"),
  ORGANIZATION_RO_PROTECTED(ORGANIZATION_READ_ONLY_ID, "organizations/organizations", ORGANIZATIONS, getEntity(ACQ_UNIT_READ_ONLY_ID), getEntityCollection(ACQ_UNIT_READ_ONLY_ID), Organization.class, "code",
    "TST-ORG"),
  ORGANIZATION_FULL_PROTECTED(ORGANIZATION_FULL_PROTECTED_ID, "organizations/organizations", ORGANIZATIONS, getEntity(ACQ_UNIT_FULL_PROTECTED_ID), getEntityCollection(ACQ_UNIT_FULL_PROTECTED_ID), Organization.class, "code",
    "TST-ORG");

  String id;
  String resource;
  String url;
  JsonObject sample;
  JsonObject collection;
  String updatedFieldName;
  Object updatedFieldValue;
  Class clazz;

  TestEntities(String id, String url, String resource, JsonObject sample, JsonObject collection, Class clazz, String updatedFieldName,
      Object updatedFieldValue) {
    this.id = id;
    this.resource = resource;
    this.url = url;
    this.sample = sample;
    this.collection = collection;
    this.clazz = clazz;
    this.updatedFieldName = updatedFieldName;
    this.updatedFieldValue = updatedFieldValue;
  }

  private static JsonObject getEntity(String... acqUnitIds) {
    return JsonObject.mapFrom(new Organization()
      .withStatus(Organization.Status.ACTIVE)
      .withName("Organization")
      .withCode("ORG")
      .withAcqUnitIds(Arrays.stream(acqUnitIds).collect(Collectors.toList())));
  }

  private static JsonObject getEntityCollection(String... acqUnitIds) {
    return JsonObject.mapFrom(new OrganizationCollection()
      .withOrganizations(Collections.singletonList(new Organization().withStatus(Organization.Status.ACTIVE)
        .withName("Organization")
        .withCode("ORG")
        .withAcqUnitIds(Arrays.stream(acqUnitIds).collect(Collectors.toList()))))
      .withTotalRecords(1));
  }

  public static JsonObject getEmptyEntityCollection() {
    return JsonObject.mapFrom(new OrganizationCollection().withTotalRecords(0));
  }

  public static JsonObject getOpenForReadEntitiesCollection() {
    return JsonObject.mapFrom(new OrganizationCollection()
      .withOrganizations(
        Arrays.asList(ORGANIZATION_NO_ACQ.getSample().mapTo(Organization.class),
          ORGANIZATION_RO_PROTECTED.getSample().mapTo(Organization.class)))
      .withTotalRecords(2));
  }

  public static JsonObject getAllEntitiesCollection() {
    return JsonObject.mapFrom(new OrganizationCollection()
      .withOrganizations(
        Arrays.asList(ORGANIZATION_NO_ACQ.getSample().mapTo(Organization.class),
          ORGANIZATION_RO_PROTECTED.getSample().mapTo(Organization.class),
          ORGANIZATION_FULL_PROTECTED.getSample().mapTo(Organization.class)))
      .withTotalRecords(3));
  }

  public String getId() {
    return id;
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
