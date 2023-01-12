package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.config.Constants.ID;
import static org.folio.rest.RestVerticle.OKAPI_USERID_HEADER;
import static org.folio.rest.impl.MockServer.ACQ_UNIT_READ_ONLY_ID;
import static org.folio.rest.impl.MockServer.ACQ_UNIT_UPDATE_ONLY_ID;
import static org.folio.rest.impl.MockServer.ID_INTERNAL_SERVER_ERROR;
import static org.folio.rest.impl.MockServer.ID_NOT_FOUND;
import static org.folio.rest.impl.MockServer.ISE_X_OKAPI_TENANT;
import static org.folio.rest.impl.MockServer.ORGANIZATION_NO_ACQ_ID;
import static org.folio.rest.impl.MockServer.USER_FULL_PROTECTED_MEMBERSHIP_ID;
import static org.folio.rest.impl.MockServer.USER_NO_MEMBERSHIP_ID;
import static org.folio.rest.impl.MockServer.USER_READ_ONLY_MEMBERSHIP_ID;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_FULL_PROTECTED;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_NO_ACQ;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_READ_PROTECTED;
import static org.folio.rest.impl.TestEntities.ORGANIZATION_UPDATE_PROTECTED;
import static org.folio.rest.client.RestClient.SEARCH_PARAMS;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.jaxrs.model.OrganizationCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class OrganizationApiTest extends ApiTestBase {

  private static final Logger logger = LogManager.getLogger(OrganizationApiTest.class);
  private static final List<TestEntities> openForReadEntities = Arrays.asList(ORGANIZATION_NO_ACQ, ORGANIZATION_READ_PROTECTED);
  private static final List<TestEntities> openForUpdateEntities = Arrays.asList(ORGANIZATION_NO_ACQ, ORGANIZATION_UPDATE_PROTECTED);
  private static final List<TestEntities> fullProtectedEntities = Collections.singletonList(ORGANIZATION_FULL_PROTECTED);
  private static final String MANAGE_PERMISSIONS = "organizations.acquisitions-units-assignments.manage";

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
  @EnumSource(TestEntities.class)
  void testPostWithAccountNumberDuplicate(TestEntities e) {
    logger.info("===== Verify POST " + e.name() + ": Account number must be unique =====");

    JsonObject entity = createAccountsArray(e.getSample());
    Headers headers = Headers.headers(X_OKAPI_URL, new Header(X_OKAPI_TENANT.getName(), ISE_X_OKAPI_TENANT));

    verifyPostRequest(e.getUrl(), entity
      .encode(), headers, APPLICATION_JSON, HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt());

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(0));
  }

  @ParameterizedTest
  @MethodSource("getOpenForReadEntities")
  void testGetByIdReadOnlyOrNoAcqUnits(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getSample();
    expected.put(ID, e.getId());

    JsonObject actual = new JsonObject(verifyGetRequest(e.getUrl() + PATH_SEPARATOR + e.getId(),
      headersForUserAndPermissions(USER_NO_MEMBERSHIP_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody().print());
    assertThat(actual, equalTo(expected));

    actual = new JsonObject(verifyGetRequest(e.getUrl() + PATH_SEPARATOR + e.getId(),
      headersForUserAndPermissions(USER_READ_ONLY_MEMBERSHIP_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody().print());
    assertThat(actual, equalTo(expected));
  }

  @ParameterizedTest
  @MethodSource("getFullProtectedEntities")
  void testGetByIdProtectedWithValidMembership(TestEntities e) {
    logger.info("===== Verify GET by ID full protected organization with full protected user membership: Successful =====");

    JsonObject expected = e.getSample();
    expected.put(ID, e.getId());

    JsonObject actual = new JsonObject(verifyGetRequest(e.getUrl() + PATH_SEPARATOR + e.getId(),
      headersForUserAndPermissions(USER_FULL_PROTECTED_MEMBERSHIP_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody().print());

    assertThat(actual, equalTo(expected));
  }

  @ParameterizedTest
  @MethodSource("getFullProtectedEntities")
  void testGetByIdProtectedWithWrongMembership(TestEntities e) {
    logger.info("===== Verify GET by ID full protected organization with read-only user membership: Forbidden =====");

    verifyGetRequest(e.getUrl() + PATH_SEPARATOR + e.getId(),
      headersForUserAndPermissions(USER_NO_MEMBERSHIP_ID), APPLICATION_JSON, 403);

    verifyGetRequest(e.getUrl() + PATH_SEPARATOR + e.getId(),
      headersForUserAndPermissions(USER_READ_ONLY_MEMBERSHIP_ID), APPLICATION_JSON, 403);
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
  void testGetCollectionWithNoMembership() {
    logger.info("===== Verify GET collection with read-only membership: Successful (open for read entities collection) =====");

    JsonObject expected = TestEntities.getOpenForReadEntitiesCollection();

    JsonObject actual = new JsonObject(
      verifyGetRequest(ORGANIZATION_NO_ACQ.getUrl(), headersForUserAndPermissions(USER_NO_MEMBERSHIP_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @Test
  void testGetCollectionWithReadOnlyMembership() {
    logger.info("===== Verify GET collection with read-only membership: Successful (open for read entities collection) =====");

    JsonObject expected = TestEntities.getOpenForReadEntitiesCollection();

    JsonObject actual = new JsonObject(
      verifyGetRequest(ORGANIZATION_NO_ACQ.getUrl(), headersForUserAndPermissions(USER_READ_ONLY_MEMBERSHIP_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @Test
  void testGetCollectionWithProtectedMembership() {
    logger.info("===== Verify GET collection with full-protected membership: Successful (all entities collection) =====");

    JsonObject expected = TestEntities.getAllEntitiesCollection();

    JsonObject actual = new JsonObject(
      verifyGetRequest(ORGANIZATION_NO_ACQ.getUrl(), headersForUserAndPermissions(USER_FULL_PROTECTED_MEMBERSHIP_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testGetByQueryWithAppropriateMembership(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getCollection();

    String endpoint = String.format(e.getUrl() + SEARCH_PARAMS, 10, 0, "&query=id==" + e.getId(), "en");
    JsonObject actual = new JsonObject(
      verifyGetRequest(endpoint, headersForUserAndPermissions(e.getUserId()), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @MethodSource("getOpenForReadEntities")
  void testGetByQueryOpenForReadEntities(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getCollection();

    String endpoint = String.format(e.getUrl() + SEARCH_PARAMS, 10, 0, "&query=id==" + e.getId(), "en");
    JsonObject actual = new JsonObject(
      verifyGetRequest(endpoint, headersForUserAndPermissions(USER_READ_ONLY_MEMBERSHIP_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @MethodSource("getFullProtectedEntities")
  void testGetProtectedCollectionNoMembership(TestEntities e) {
    logger.info("===== Verify GET by ID protected Organization with read-only membership: Empty Collection =====");

    JsonObject expected = JsonObject.mapFrom(new OrganizationCollection().withTotalRecords(0));

    String endpoint = String.format(e.getUrl() + SEARCH_PARAMS, 10, 0, "&query=id==" + e.getId(), "en");
    JsonObject actual = new JsonObject(
      verifyGetRequest(endpoint, headersForUserAndPermissions(USER_NO_MEMBERSHIP_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @MethodSource("getFullProtectedEntities")
  void testGetProtectedCollectionReadOnlyMembership(TestEntities e) {
    logger.info("===== Verify GET by ID protected Organization with read-only membership: Empty Collection =====");

    JsonObject expected = JsonObject.mapFrom(new OrganizationCollection().withTotalRecords(0));

    String endpoint = String.format(e.getUrl() + SEARCH_PARAMS, 10, 0, "&query=id==" + e.getId(), "en");
    JsonObject actual = new JsonObject(
      verifyGetRequest(endpoint, headersForUserAndPermissions(USER_READ_ONLY_MEMBERSHIP_ID), APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
        .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testGetByQueryInternalServerError(TestEntities e) {
    logger.info("===== Verify GET by query " + e.name() + ": Internal Server Error =====");

    String endpoint = String.format(e.getUrl() + SEARCH_PARAMS, 10, 0, "&query=id==" + ID_INTERNAL_SERVER_ERROR, "en");
    verifyGetRequest(endpoint, headersForUserAndPermissions(USER_READ_ONLY_MEMBERSHIP_ID), APPLICATION_JSON, 500);
  }

  @ParameterizedTest
  @MethodSource("getOpenForUpdateEntities")
  void testPutByIdOpenForUpdateEntities(TestEntities e) {
    logger.info("===== Verify PUT by ID open for update " + e.name() + ": Successful =====");

    JsonObject expected = e.getSample();
    expected.put(e.getUpdatedFieldName(), e.getUpdatedFieldValue());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + e.getId(), JsonObject.mapFrom(expected)
      .encode());

    assertThat(MockServer.getInstance().getAllServeEvents().stream()
      .filter(event -> event.getRequest().getMethod().equals(RequestMethod.PUT))
      .collect(Collectors.toList()), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutByIdNoAcqChangesMatchingMembership(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + " without acq. units change: Successful =====");

    JsonObject expected = e.getSample();
    expected.put(e.getUpdatedFieldName(), e.getUpdatedFieldValue());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + e.getId(), JsonObject.mapFrom(expected).encode(),
      headersForUserAndPermissions(e.getUserId()), EMPTY, 204);

    assertThat(MockServer.getInstance().getAllServeEvents().stream()
      .filter(event -> event.getRequest().getMethod().equals(RequestMethod.PUT))
      .collect(Collectors.toList()), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutByIdAcqChangesMatchingMembershipAndManagePermissions(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + " with acq. units change and manage permissions: Successful =====");

    JsonObject expected = e.getSample();
    expected.put(e.getUpdatedFieldName(), e.getUpdatedFieldValue());
    expected.put("acqUnitIds", new JsonArray(Arrays.asList(ACQ_UNIT_READ_ONLY_ID, ACQ_UNIT_UPDATE_ONLY_ID)));

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + e.getId(), JsonObject.mapFrom(expected).encode(),
      headersForUserAndPermissions(e.getUserId(), MANAGE_PERMISSIONS), EMPTY, 204);

    assertThat(MockServer.getInstance().getAllServeEvents().stream()
      .filter(event -> event.getRequest().getMethod().equals(RequestMethod.PUT))
      .collect(Collectors.toList()), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutByIdAcqChangesMatchingMembershipNoManagePermissions(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + " with acq. units change and no manage permissions: Forbidden =====");

    JsonObject expected = e.getSample();
    expected.put(e.getUpdatedFieldName(), e.getUpdatedFieldValue());
    expected.put("acqUnitIds", new JsonArray(Arrays.asList(ACQ_UNIT_READ_ONLY_ID, ACQ_UNIT_UPDATE_ONLY_ID)));

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + e.getId(), JsonObject.mapFrom(expected).encode(),
      headersForUserAndPermissions(e.getUserId()), APPLICATION_JSON, 403);

    assertThat(MockServer.getInstance().getAllServeEvents().stream()
      .filter(event -> event.getRequest().getMethod().equals(RequestMethod.PUT))
      .collect(Collectors.toList()), hasSize(0));
  }

  @ParameterizedTest
  @MethodSource("getClosedForUpdateEntities")
  void testPutByIdClosedForUpdateEntitiesWithNoMembership(TestEntities e) {
    logger.info("===== Verify PUT by ID open for update " + e.name() + ": Forbidden =====");

    JsonObject expected = e.getSample();
    expected.put(e.getUpdatedFieldName(), e.getUpdatedFieldValue());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + e.getId(), JsonObject.mapFrom(expected).encode(),
      headersForUserAndPermissions(USER_NO_MEMBERSHIP_ID), APPLICATION_JSON, 403);

    assertThat(MockServer.getInstance().getAllServeEvents().stream()
      .filter(event -> event.getRequest().getMethod().equals(RequestMethod.PUT))
      .collect(Collectors.toList()), hasSize(0));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutIdMismatchTest(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": ID Mismatch =====");

    JsonObject expected = e.getSample();
    expected.put(ID, UUID.randomUUID()
      .toString());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + e.getId(), expected.encode(),
      APPLICATION_JSON, HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt());

    assertThat(MockServer.getInstance().getAllServeEvents().stream()
      .filter(event -> event.getRequest().getMethod().equals(RequestMethod.PUT))
      .collect(Collectors.toList()), hasSize(0));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutWithAccountNumberDuplicate(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": Account number must be unique =====");

    JsonObject entity = createAccountsArray(e.getSample());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + e.getId(), entity.encode(),
      APPLICATION_JSON, HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt());

    assertThat(MockServer.getInstance().getAllServeEvents().stream()
      .filter(event -> event.getRequest().getMethod().equals(RequestMethod.PUT))
      .collect(Collectors.toList()), hasSize(0));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testPutWithoutIdTest(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": ID missed =====");

    JsonObject entity = e.getSample();
    JsonObject expected = entity.copy();
    expected.put(ID, e.getId());
    assertThat(entity.getString(ID), nullValue());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + e.getId(), JsonObject.mapFrom(entity).encode(),
      headersForUserAndPermissions(e.getUserId()), EMPTY, 204);

    assertThat(MockServer.getInstance().getAllServeEvents().stream()
      .filter(event -> event.getRequest().getMethod().equals(RequestMethod.PUT))
      .collect(Collectors.toList()), hasSize(1));
    assertThat(new JsonObject(MockServer.getInstance()
      .getAllServeEvents()
      .get(0)
      .getRequest()
      .getBodyAsString()).getString(ID), is(e.getId()));
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

  private static Headers headersForUserAndPermissions(String userId, String... permissions) {
    return Headers.headers(X_OKAPI_URL,
      X_OKAPI_TENANT,
      new Header(OKAPI_USERID_HEADER, userId),
      new Header(OKAPI_HEADER_PERMISSIONS, new JsonArray(Arrays.asList(permissions)).encode()));
  }

  private static Stream<TestEntities> getOpenForReadEntities() {
    return openForReadEntities.stream();
  }

  private static Stream<TestEntities> getOpenForUpdateEntities() {
    return openForUpdateEntities.stream();
  }

  private static Stream<TestEntities> getClosedForUpdateEntities() {
    return Stream.of(ORGANIZATION_READ_PROTECTED, ORGANIZATION_FULL_PROTECTED);
  }

  private static Stream<TestEntities> getFullProtectedEntities() {
    return fullProtectedEntities.stream();
  }

  private JsonObject createAccountsArray(JsonObject jsonObject) {
    JsonArray accounts = new JsonArray();

    JsonObject account1 = new JsonObject()
      .put("name", "Serials")
      .put("accountNo", "xxxx7859")
      .put("accountStatus", "Active");

    JsonObject account2 = new JsonObject()
      .put("name", "TestAccount")
      .put("accountNo", "xxxx7859")
      .put("accountStatus", "Active");

    accounts.add(account1).add(account2);
    jsonObject.put("accounts", accounts);

    return jsonObject;
  }
}
