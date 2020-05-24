package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.config.Constants.ID;
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.rest.impl.MockServer.FULL_PROTECTED_USER_ID;
import static org.folio.rest.impl.MockServer.ORGANIZATION_NO_ACQ_ID;
import static org.folio.rest.impl.MockServer.ID_INTERNAL_SERVER_ERROR;
import static org.folio.rest.impl.MockServer.ID_NOT_FOUND;
import static org.folio.rest.impl.MockServer.ISE_X_OKAPI_TENANT;
import static org.folio.rest.impl.MockServer.READ_ONLY_USER_ID;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_FULL_PROTECTED;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_NO_ACQ;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_RO_PROTECTED;
import static org.folio.service.BaseService.SEARCH_PARAMS;
import static wiremock.org.hamcrest.MatcherAssert.assertThat;
import static wiremock.org.hamcrest.Matchers.equalTo;
import static wiremock.org.hamcrest.Matchers.hasSize;
import static wiremock.org.hamcrest.Matchers.is;
import static wiremock.org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.folio.HttpStatus;
import org.folio.rest.jaxrs.model.OrganizationCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.jupiter.params.provider.MethodSource;

class OrganizationApiTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(OrganizationApiTest.class);
  private static final List<TestEntities> permittedEntities = Arrays.asList(ORGANIZATION_NO_ACQ, ORGANIZATION_RO_PROTECTED);
  private static final List<TestEntities> restrictedEntities = Collections.singletonList(ORGANIZATION_FULL_PROTECTED);

  @Test
  void testPost() {
    logger.info("===== Verify POST : Successful =====");

    JsonObject posted = ORGANIZATION_NO_ACQ.getSample();
    assertThat(posted.getString(ID), nullValue());
    JsonObject expected = ORGANIZATION_NO_ACQ.getSample()
      .copy();
    expected.put(ID, ORGANIZATION_NO_ACQ.getId());

    JsonObject actual = new JsonObject(verifyPostRequest(ORGANIZATION_NO_ACQ.getUrl(), JsonObject.mapFrom(posted)
      .encode()).getBody()
        .print());

    assertThat(actual.getString(ID), is(ORGANIZATION_NO_ACQ.getId()));
    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPostInternalServerError(TestEntities e) {
    logger.info("===== Verify POST " + e.name() + ": Internal Server Error =====");

    Headers headers = Headers.headers(X_OKAPI_URL, new Header(X_OKAPI_TENANT.getName(), ISE_X_OKAPI_TENANT));

    verifyPostRequest(e.getUrl(), e.getSample()
      .encode(), headers, APPLICATION_JSON, HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt());

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @MethodSource("getPermittedEntities")
  void testGetByIdReadOnlyOrNoAcqUnits(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getSample();
    expected.put(ID, e.getId());

    JsonObject actual = new JsonObject(verifyGetRequest(e.getUrl() + PATH_SEPARATOR + e.getId(),
      headersForUser(READ_ONLY_USER_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody().print());

    assertThat(actual, equalTo(expected));
  }

  @ParameterizedTest
  @MethodSource("getRestrictedEntities")
  void testGetByIdProtectedWithValidMembership(TestEntities e) {
    logger.info("===== Verify GET by ID full protected organization with full protected user membership: Successful =====");

    JsonObject expected = e.getSample();
    expected.put(ID, e.getId());

    JsonObject actual = new JsonObject(verifyGetRequest(e.getUrl() + PATH_SEPARATOR + e.getId(),
      headersForUser(FULL_PROTECTED_USER_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody().print());

    assertThat(actual, equalTo(expected));
  }

  @ParameterizedTest
  @MethodSource("getRestrictedEntities")
  void testGetByIdProtectedWithWrongMembership(TestEntities e) {
    logger.info("===== Verify GET by ID full protected organization with read-only user membership: Forbidden =====");

    verifyGetRequest(e.getUrl() + PATH_SEPARATOR + e.getId(),
      headersForUser(READ_ONLY_USER_ID), APPLICATION_JSON, 403);
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testGetByIdNotFound(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Not Found =====");

    verifyGetRequest(e.getUrl() + PATH_SEPARATOR + ID_NOT_FOUND, APPLICATION_JSON, 404);
  }

  @Test
  void testGetByIdInternalServerError() {
    logger.info("===== Verify GET by ID : Internal Server Error =====");

    verifyGetRequest(ORGANIZATION_NO_ACQ.getUrl() + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR, APPLICATION_JSON, 500);
  }

  @Test
  void testGetCollectionWithReadOnlyMembership() {
    logger.info("===== Verify GET collection with read-only membership: Successful (open for read entities collection) =====");

    JsonObject expected = TestEntities.getOpenForReadEntitiesCollection();

    JsonObject actual = new JsonObject(
      verifyGetRequest(ORGANIZATION_NO_ACQ.getUrl(), headersForUser(READ_ONLY_USER_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @Test
  void testGetCollectionWithProtectedMembership() {
    logger.info("===== Verify GET collection with full-protected membership: Successful (all entities collection) =====");

    JsonObject expected = TestEntities.getAllEntitiesCollection();

    JsonObject actual = new JsonObject(
      verifyGetRequest(ORGANIZATION_NO_ACQ.getUrl(), headersForUser(FULL_PROTECTED_USER_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testGetByQueryWithProtectedMembership(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getCollection();

    String endpoint = String.format(e.getUrl() + SEARCH_PARAMS, 10, 0, "&query=id==" + e.getId(), "en");
    JsonObject actual = new JsonObject(
      verifyGetRequest(endpoint, headersForUser(FULL_PROTECTED_USER_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @MethodSource("getPermittedEntities")
  void testGetByQueryOpenForReadEntities(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getCollection();

    String endpoint = String.format(e.getUrl() + SEARCH_PARAMS, 10, 0, "&query=id==" + e.getId(), "en");
    JsonObject actual = new JsonObject(
      verifyGetRequest(endpoint, headersForUser(READ_ONLY_USER_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @MethodSource("getRestrictedEntities")
  void testGetProtectedCollectionUserNotMember(TestEntities e) {
    logger.info("===== Verify GET by ID protected Organization with read-only membership: Empty Collection =====");

    JsonObject expected = JsonObject.mapFrom(new OrganizationCollection().withTotalRecords(0));

    String endpoint = String.format(e.getUrl() + SEARCH_PARAMS, 10, 0, "&query=id==" + e.getId(), "en");
    JsonObject actual = new JsonObject(
      verifyGetRequest(endpoint, headersForUser(READ_ONLY_USER_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testGetByQueryInternalServerError(TestEntities e) {
    logger.info("===== Verify GET by query " + e.name() + ": Internal Server Error =====");

    String endpoint = String.format(e.getUrl() + SEARCH_PARAMS, 10, 0, "&query=id==" + ID_INTERNAL_SERVER_ERROR, "en");
    verifyGetRequest(endpoint, headersForUser(READ_ONLY_USER_ID), APPLICATION_JSON, 500);
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutById(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getSample();
    expected.put(e.getUpdatedFieldName(), e.getUpdatedFieldValue());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + ORGANIZATION_NO_ACQ_ID, JsonObject.mapFrom(expected)
      .encode());

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutIdMismatchTest(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": ID Mismatch =====");

    JsonObject expected = e.getSample();
    expected.put(ID, UUID.randomUUID()
      .toString());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + e.getId(), expected.encode(), APPLICATION_JSON, 422);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(0));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutWithoutIdTest(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": ID missed =====");

    JsonObject entity = e.getSample();
    JsonObject expected = entity.copy();
    expected.put(ID, ORGANIZATION_NO_ACQ_ID);
    assertThat(entity.getString(ID), nullValue());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + ORGANIZATION_NO_ACQ_ID, JsonObject.mapFrom(entity)
      .encode());

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
    assertThat(new JsonObject(MockServer.getInstance()
      .getAllServeEvents()
      .get(0)
      .getRequest()
      .getBodyAsString()).getString(ID), is(ORGANIZATION_NO_ACQ_ID));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutNotFound(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": Not Found =====");

    JsonObject entity = e.getSample();

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + ID_NOT_FOUND, JsonObject.mapFrom(entity)
      .encode(), APPLICATION_JSON, 404);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutInternalServerError(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": Internal Server Error =====");

    JsonObject entity = e.getSample();

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR, JsonObject.mapFrom(entity)
      .encode(), APPLICATION_JSON, 500);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testDeleteOrganizationById(TestEntities e) {
    logger.info("===== Verify DELETE by ID " + e.name() + ": Successful =====");

    verifyDeleteRequest(e.getUrl() + PATH_SEPARATOR + ORGANIZATION_NO_ACQ_ID);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testDeleteNotFound(TestEntities e) {
    logger.info("===== Verify DELETE by ID " + e.name() + ": Not Found =====");

    verifyDeleteRequest(e.getUrl() + PATH_SEPARATOR + ID_NOT_FOUND, APPLICATION_JSON, 404);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testDeleteInternalServerError(TestEntities e) {
    logger.info("===== Verify DELETE by ID " + e.name() + ": Internal Server Error =====");

    verifyDeleteRequest(e.getUrl() + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR, APPLICATION_JSON, 500);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  private static Headers headersForUser(String userId) {
    return Headers.headers(X_OKAPI_URL, X_OKAPI_TENANT, new Header(OKAPI_USERID_HEADER, userId));
  }

  private static Stream<TestEntities> getPermittedEntities() {
    return permittedEntities.stream();
  }

  private static Stream<TestEntities> getRestrictedEntities() {
    return restrictedEntities.stream();
  }
}
