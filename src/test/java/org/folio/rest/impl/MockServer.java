package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.config.Constants.ID;
import static org.folio.rest.impl.ApiTestBase.PATH_SEPARATOR;
import static org.folio.rest.impl.ApiTestBase.X_OKAPI_TENANT;
import static org.folio.rest.impl.MockAcqUnits.FULL_PROTECTED;
import static org.folio.rest.impl.MockAcqUnits.READ_ONLY;
import static org.folio.rest.impl.MockAcqUnits.UPDATE_ONLY;
import static org.folio.rest.impl.TestEntities.BANKING_INFORMATION_ENTITY;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_FULL_PROTECTED;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_NO_ACQ;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_READ_PROTECTED;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_UPDATE_PROTECTED;
import static org.folio.service.organization.OrganizationStorageService.GET_ORGANIZATIONS_BY_QUERY;
import static org.folio.util.ResourcePathResolver.ACQUISITIONS_UNITS;
import static org.folio.util.ResourcePathResolver.BANKING_INFORMATION;
import static org.folio.util.ResourcePathResolver.ORGANIZATIONS;
import static org.folio.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.util.ResourcePathResolver.resourcesPath;
import static org.folio.util.RestUtils.ACQUISITIONS_UNIT_ID;
import static org.folio.util.RestUtils.ACQUISITIONS_UNIT_IDS;
import static org.folio.util.RestUtils.ACTIVE_UNITS_CQL;
import static org.folio.util.RestUtils.ALL_UNITS_CQL;
import static org.folio.util.RestUtils.GET_UNITS_MEMBERSHIPS_BY_QUERY;
import static org.folio.util.RestUtils.NO_ACQ_UNIT_ASSIGNED_CQL;
import static org.folio.util.RestUtils.SEARCH_PARAMS;
import static org.folio.util.RestUtils.buildQuery;
import static org.folio.util.RestUtils.combineCqlExpressions;
import static org.folio.util.RestUtils.convertIdsToCqlQuery;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.vertx.core.json.JsonObject;

public class MockServer {

  public static final String ORGANIZATION_NO_ACQ_ID = "1614c7e5-7b3e-4cb3-8fb9-48fe85c7b47f";
  public static final String ORGANIZATION_READ_ONLY_ID = "874225ae-e3f5-4666-ae19-b35709ff0ee9";
  public static final String ORGANIZATION_UPDATE_ONLY_ID = "caa81a86-5829-45b7-b320-ca0d27abf9d5";
  public static final String ORGANIZATION_FULL_PROTECTED_ID = "9985ec57-91cb-4588-9fab-089859a017a5";
  public static final String BANKING_INFORMATION_ID = "40540ab1-39bf-4684-9a2f-8968ee379d8d";
  public static final String ORGANIZATION_ID = "a96e55a8-66a8-4d0c-8e8f-f379b207ac84";
  public static final String ID_NOT_FOUND = "f394664e-849d-4213-96c4-2795f772ae3a";
  public static final String ID_INTERNAL_SERVER_ERROR = "96e79e5a-c379-4a5a-8244-d6df0342e21c";
  public static final String ACQ_UNIT_READ_ONLY_ID = "6b982ffe-8efd-4690-8168-0c773b49cde1";
  public static final String ACQ_UNIT_UPDATE_ONLY_ID = "aa0ec4e1-782f-45f6-a6f3-8e6b6c00599c";
  public static final String ACQ_UNIT_FULL_PROTECTED_ID = "e68c18fc-833f-494e-9a0e-b236eb4b310b";
  public static final String USER_NO_MEMBERSHIP_ID = "6b4be232-5ad9-47a6-80b1-8c1acabd6212";
  public static final String USER_READ_ONLY_MEMBERSHIP_ID = "6e076ac5-371e-4462-af79-187c54fe70de";
  public static final String USER_UPDATE_ONLY_MEMBERSHIP_ID = "6bf43c5a-6513-4ce0-a3bd-ac447b222094";
  public static final String USER_FULL_PROTECTED_MEMBERSHIP_ID = "480dba68-ee84-4b9c-a374-7e824fc49227";
  public static final String ISE_X_OKAPI_TENANT = "ISE";
  public static WireMockServer wireMockServer;

  public static void init(int mockPort) {

    if (Objects.isNull(wireMockServer)) {
      wireMockServer = new WireMockServer(mockPort);
      wireMockServer.start();
    }

    for (TestEntities e : TestEntities.values()) {

      JsonObject expected = e.getSample();
      expected.put(ID, e.getId());

      wireMockServer
        .stubFor(get(urlEqualTo(resourceByIdPath(e.getResource(), e.getId()))).willReturn(aResponse().withBody(expected.encode())
          .withHeader(CONTENT_TYPE, TEXT_PLAIN)
          .withStatus(200)));

      wireMockServer.stubFor(get(urlEqualTo(
          String.format(resourcesPath(e.getResource()) + SEARCH_PARAMS, 0, 1, buildQuery("id==" + e.getId()))))
            .willReturn(aResponse().withBody(e.getCollection()
              .encode())
              .withHeader(CONTENT_TYPE, APPLICATION_JSON)
              .withStatus(200)));

      expected.put(e.getUpdatedFieldName(), e.getUpdatedFieldValue());

      wireMockServer.stubFor(put(urlEqualTo(resourcesPath(e.getResource()) + PATH_SEPARATOR + e.getId())).willReturn(aResponse()
        .withBody(JsonObject.mapFrom(expected)
          .encode())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(204)));

      wireMockServer.stubFor(delete(urlEqualTo(resourcesPath(e.getResource()) + PATH_SEPARATOR + e.getId()))
        .willReturn(aResponse().withStatus(204)));
    }

    JsonObject expected = ORGANIZATION_NO_ACQ.getSample();
    expected.put(ID, ORGANIZATION_NO_ACQ.getId());
    wireMockServer.stubFor(post(urlEqualTo(resourcesPath(ORGANIZATION_NO_ACQ.getResource())))
      .willReturn(aResponse().withBody(expected.encode())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(201)));

    wireMockServer.stubFor(get(urlEqualTo(resourceByIdPath(ORGANIZATIONS, ID_NOT_FOUND)))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(404)));

    wireMockServer.stubFor(get(urlEqualTo(resourceByIdPath(ORGANIZATIONS, ID_INTERNAL_SERVER_ERROR)))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(500)));

    wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(ID_INTERNAL_SERVER_ERROR, READ_ONLY.acqUnitId))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(500)));

    wireMockServer.stubFor(
      post(urlEqualTo(resourcesPath(ORGANIZATIONS))).withHeader(X_OKAPI_TENANT.getName(), containing(ISE_X_OKAPI_TENANT))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, TEXT_PLAIN)
          .withStatus(500)));

    wireMockServer.stubFor(get(urlEqualTo(String.format(resourcesPath(ORGANIZATIONS) + SEARCH_PARAMS, 0, 1,
      buildQuery("id==" + ID_INTERNAL_SERVER_ERROR))))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(500)));

    wireMockServer.stubFor(put(urlEqualTo(resourcesPath(ORGANIZATIONS) + PATH_SEPARATOR + ID_NOT_FOUND))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(404)));

    wireMockServer.stubFor(put(urlEqualTo(resourcesPath(ORGANIZATIONS) + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(500)));

    wireMockServer.stubFor(delete(urlEqualTo(resourcesPath(ORGANIZATIONS) + PATH_SEPARATOR + ID_NOT_FOUND))
      .willReturn(aResponse().withStatus(404)));

    wireMockServer.stubFor(delete(urlEqualTo(resourcesPath(ORGANIZATIONS) + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR))
      .willReturn(aResponse().withStatus(500)));

    for (MockAcqUnits unit: MockAcqUnits.values()) {
      wireMockServer.stubFor(get(urlEqualTo(urlForAcqUnit(true, unit.acqUnitId)))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(unit.getAcqUnitsCollection())
          .withStatus(200)));

      wireMockServer.stubFor(get(urlEqualTo(urlForAcqUnit(false, unit.acqUnitId)))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(unit.getAcqUnitsCollection())
          .withStatus(200)));

      wireMockServer.stubFor(get(urlEqualTo(urlForAcqUnitMembership(unit.userId)))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(unit.getAcqUnitMembershipCollection())
          .withStatus(200)));

      wireMockServer.stubFor(get(urlEqualTo(urlForAcqUnitMembership(unit.userId, unit.acqUnitId)))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(unit.getAcqUnitMembershipCollection())
          .withStatus(200)));

      wireMockServer.stubFor(get(urlEqualTo(urlForAcqUnitMembership(USER_NO_MEMBERSHIP_ID, unit.acqUnitId)))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(MockAcqUnits.getEmptyAcqUnitMembershipCollection())
          .withStatus(200)));
    }

    wireMockServer.stubFor(get(urlEqualTo(urlForAcqUnit(false, ACQ_UNIT_READ_ONLY_ID, ACQ_UNIT_UPDATE_ONLY_ID)))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(MockAcqUnits.createAcqUnitsCollection(READ_ONLY.getAcquisitionUnit(), UPDATE_ONLY.getAcquisitionUnit()).encode())
        .withStatus(200)));

    wireMockServer.stubFor(get(urlEqualTo(urlForAcqUnitMembership(USER_NO_MEMBERSHIP_ID)))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(MockAcqUnits.getEmptyAcqUnitMembershipCollection())
        .withStatus(200)));

    wireMockServer.stubFor(get(urlEqualTo(urlOpenForReadAcqUnit()))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(READ_ONLY.getAcqUnitsCollection())
        .withStatus(200)));

    wireMockServer.stubFor(get(urlEqualTo(urlForAcqUnitMembership(READ_ONLY.userId, FULL_PROTECTED.acqUnitId)))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(MockAcqUnits.getEmptyAcqUnitMembershipCollection())
        .withStatus(200)));

    wireMockServer.stubFor(get(urlEqualTo(urlForAcqUnitMembership(FULL_PROTECTED.userId, READ_ONLY.acqUnitId)))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(MockAcqUnits.getEmptyAcqUnitMembershipCollection())
        .withStatus(200)));

    //  stubs for acq. units clauses

    Arrays.asList(ORGANIZATION_NO_ACQ, ORGANIZATION_READ_PROTECTED)
      .forEach(e -> {
        wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(e.getId(), READ_ONLY.acqUnitId))
          .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(e.getCollection().encode())
            .withStatus(200)));

        wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(e.getId(), UPDATE_ONLY.acqUnitId, READ_ONLY.acqUnitId))
          .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(e.getCollection().encode())
            .withStatus(200)));

        wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(e.getId(), FULL_PROTECTED.acqUnitId, READ_ONLY.acqUnitId))
          .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(e.getCollection().encode())
            .withStatus(200)));
      });

    Arrays.asList(ORGANIZATION_UPDATE_PROTECTED, ORGANIZATION_FULL_PROTECTED)
      .forEach(e -> wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(e.getId(), READ_ONLY.acqUnitId))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(TestEntities.getEmptyEntityCollection().encode())
          .withStatus(200))));

    wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(ORGANIZATION_UPDATE_ONLY_ID, UPDATE_ONLY.acqUnitId, READ_ONLY.acqUnitId))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(ORGANIZATION_UPDATE_PROTECTED.getCollection().encode())
        .withStatus(200)));

    wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(ORGANIZATION_UPDATE_ONLY_ID, FULL_PROTECTED.acqUnitId, READ_ONLY.acqUnitId))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(TestEntities.getEmptyEntityCollection().encode())
        .withStatus(200)));

    wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(ORGANIZATION_FULL_PROTECTED_ID, UPDATE_ONLY.acqUnitId, READ_ONLY.acqUnitId))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(TestEntities.getEmptyEntityCollection().encode())
        .withStatus(200)));

    wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(ORGANIZATION_FULL_PROTECTED_ID, FULL_PROTECTED.acqUnitId, READ_ONLY.acqUnitId))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON).withBody(ORGANIZATION_FULL_PROTECTED.getCollection().encode())
        .withStatus(200)));

    // stubs for no id GET by query

    wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(EMPTY, READ_ONLY.acqUnitId))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(TestEntities.createCollection(ORGANIZATION_NO_ACQ, ORGANIZATION_READ_PROTECTED).encode())
        .withStatus(200)));

    wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(EMPTY, UPDATE_ONLY.acqUnitId, READ_ONLY.acqUnitId))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(TestEntities.createCollection(ORGANIZATION_NO_ACQ, ORGANIZATION_READ_PROTECTED, ORGANIZATION_UPDATE_PROTECTED).encode())
        .withStatus(200)));

    wireMockServer.stubFor(get(urlForQueryWithAcqUnitClause(EMPTY, FULL_PROTECTED.acqUnitId, READ_ONLY.acqUnitId))
      .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withBody(TestEntities.createCollection(ORGANIZATION_NO_ACQ, ORGANIZATION_READ_PROTECTED, ORGANIZATION_FULL_PROTECTED).encode())
        .withStatus(200)));

    // stubs for banking information
    JsonObject bankingInformation = BANKING_INFORMATION_ENTITY.getSample();
    bankingInformation.put(ID, BANKING_INFORMATION_ENTITY.getId());
    wireMockServer.stubFor(post(urlEqualTo(resourcesPath(BANKING_INFORMATION)))
      .willReturn(aResponse().withBody(bankingInformation.encode())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(201)));

    wireMockServer.stubFor(get(urlEqualTo(
      String.format(resourcesPath(BANKING_INFORMATION_ENTITY.getResource()) + SEARCH_PARAMS, 10, 0, buildQuery("id==" + BANKING_INFORMATION_ENTITY.getId()))))
      .willReturn(aResponse().withBody(BANKING_INFORMATION_ENTITY.getCollection()
          .encode())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(200)));
  }

  public static void destroy() {
    if (Objects.nonNull(wireMockServer)) {
      wireMockServer.stop();
    }
  }

  public static WireMockServer getInstance() {
    return wireMockServer;
  }

  public static void release() {
    wireMockServer.resetAll();
  }

  public static void resetRequests() {
    wireMockServer.resetRequests();
  }

  private static String urlForAcqUnit(boolean activeOnly, String... ids) {
    return String.format(resourcesPath(ACQUISITIONS_UNITS) + SEARCH_PARAMS, Integer.MAX_VALUE, 0,
      buildQuery(combineCqlExpressions("and", activeOnly ? ACTIVE_UNITS_CQL : ALL_UNITS_CQL,
        convertIdsToCqlQuery(Arrays.asList(ids)))));
  }

  private static String urlOpenForReadAcqUnit() {
    return String.format(resourcesPath(ACQUISITIONS_UNITS) + SEARCH_PARAMS, Integer.MAX_VALUE, 0,
      buildQuery(combineCqlExpressions("and", ACTIVE_UNITS_CQL, "protectRead==false")));
  }

  private static String urlForAcqUnitMembership(String userId) {
    return String.format(GET_UNITS_MEMBERSHIPS_BY_QUERY, Integer.MAX_VALUE, 0, buildQuery("userId==" + userId));
  }

  private static String urlForAcqUnitMembership(String userId, String... acqUnitIds) {
    String query = String.format("userId==%s AND %s", userId, convertIdsToCqlQuery(Arrays.stream(acqUnitIds).collect(Collectors.toList()), ACQUISITIONS_UNIT_ID, true));
    return String.format(GET_UNITS_MEMBERSHIPS_BY_QUERY, Integer.MAX_VALUE, 0, buildQuery(query));
  }

  private static String urlForQueryWithAcqUnitClause(String organizationId, String... acqUnitIds) {
    String query = organizationId.equals(EMPTY) ? EMPTY : "id==" + organizationId;
    String clause = String.format("%s or (%s)", convertIdsToCqlQuery(Arrays.stream(acqUnitIds).collect(Collectors.toList()), ACQUISITIONS_UNIT_IDS, false), NO_ACQ_UNIT_ASSIGNED_CQL);
    return StringUtils.isEmpty(query) ?
      String.format(GET_ORGANIZATIONS_BY_QUERY, 10, 0, buildQuery(clause)) :
      String.format(GET_ORGANIZATIONS_BY_QUERY, 10, 0, buildQuery(combineCqlExpressions("and", clause, query)));
  }
}
