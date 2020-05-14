package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.config.Constants.ID;
import static org.folio.rest.impl.MockServer.ID_EXPECTED;
import static org.folio.rest.impl.MockServer.ID_INTERNAL_SERVER_ERROR;
import static org.folio.rest.impl.MockServer.ID_NOT_FOUND;
import static org.folio.rest.impl.MockServer.ISE_X_OKAPI_TENANT;
import static org.folio.service.BaseService.SEARCH_PARAMS;
import static wiremock.org.hamcrest.MatcherAssert.assertThat;
import static wiremock.org.hamcrest.Matchers.equalTo;
import static wiremock.org.hamcrest.Matchers.hasSize;
import static wiremock.org.hamcrest.Matchers.is;
import static wiremock.org.hamcrest.Matchers.nullValue;

import java.util.Objects;
import java.util.UUID;

import org.folio.HttpStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrganizationApiTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(OrganizationApiTest.class);

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testPost(TestEntities e) {
    logger.info("===== Verify POST " + e.name() + ": Successful =====");

    JsonObject posted = e.getSample();
    assertThat(posted.getString(ID), nullValue());
    JsonObject expected = e.getSample()
      .copy();
    expected.put(ID, ID_EXPECTED);

    JsonObject actual = new JsonObject(verifyPostRequest(e.getUrl(), JsonObject.mapFrom(posted)
      .encode()).getBody()
        .print());

    assertThat(actual.getString(ID), is(ID_EXPECTED));
    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testPostInternalServerError(TestEntities e) {
    logger.info("===== Verify POST " + e.name() + ": Internal Server Error =====");

    Headers headers = Headers.headers(X_OKAPI_URL, new Header(X_OKAPI_TENANT.getName(), ISE_X_OKAPI_TENANT));

    verifyPostRequest(e.getUrl(), e.getSample()
      .encode(), headers, APPLICATION_JSON, HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt());

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testGetById(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getSample();
    expected.put(ID, ID_EXPECTED);

    JsonObject actual = new JsonObject(verifyGetRequest(e.getUrl() + PATH_SEPARATOR + ID_EXPECTED).body()
      .print());

    assertThat(actual, equalTo(expected));
    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testGetByIdNotFound(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Not Found =====");

    verifyGetRequest(e.getUrl() + PATH_SEPARATOR + ID_NOT_FOUND, APPLICATION_JSON, 404);
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testGetByIdInternalServerError(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Internal Server Error =====");

    verifyGetRequest(e.getUrl() + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR, APPLICATION_JSON, 500);
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testGetByQuery(TestEntities e) {
    logger.info("===== Verify GET by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getCollection();

    JsonObject actual = new JsonObject(
        verifyGetRequest(String.format(e.getUrl() + SEARCH_PARAMS, 0, 1, "&query=id==" + ID_EXPECTED, "en")).getBody()
          .print());

    assertThat(Objects.equals(actual, expected), is(true));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testGetByQueryInternalServerError(TestEntities e) {
    logger.info("===== Verify GET by query " + e.name() + ": Internal Server Error =====");

    verifyGetRequest(String.format(e.getUrl() + SEARCH_PARAMS, 0, 1, "&query=id==" + ID_INTERNAL_SERVER_ERROR, "en"),
        APPLICATION_JSON, 500);
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testPutById(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": Successful =====");

    JsonObject expected = e.getSample();
    expected.put(e.getUpdatedFieldName(), e.getUpdatedFieldValue());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + ID_EXPECTED, JsonObject.mapFrom(expected)
      .encode());

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testPutIdMismatchTest(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": ID Mismatch =====");

    JsonObject expected = e.getSample();
    expected.put(ID, UUID.randomUUID()
      .toString());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + ID_EXPECTED, expected.encode(), TEXT_PLAIN, 400);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(0));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testPutWithoutIdTest(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": ID missed =====");

    JsonObject entity = e.getSample();
    JsonObject expected = entity.copy();
    expected.put(ID, ID_EXPECTED);
    assertThat(entity.getString(ID), nullValue());

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + ID_EXPECTED, JsonObject.mapFrom(entity)
      .encode());

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
    assertThat(new JsonObject(MockServer.getInstance()
      .getAllServeEvents()
      .get(0)
      .getRequest()
      .getBodyAsString()).getString(ID), is(ID_EXPECTED));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testPutNotFound(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": Not Found =====");

    JsonObject entity = e.getSample();

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + ID_NOT_FOUND, JsonObject.mapFrom(entity)
      .encode(), APPLICATION_JSON, 404);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testPutInternalServerError(TestEntities e) {
    logger.info("===== Verify PUT by ID " + e.name() + ": Internal Server Error =====");

    JsonObject entity = e.getSample();

    verifyPutRequest(e.getUrl() + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR, JsonObject.mapFrom(entity)
      .encode(), APPLICATION_JSON, 500);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testDeleteOrganizationById(TestEntities e) {
    logger.info("===== Verify DELETE by ID " + e.name() + ": Successful =====");

    verifyDeleteRequest(e.getUrl() + PATH_SEPARATOR + ID_EXPECTED);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testDeleteNotFound(TestEntities e) {
    logger.info("===== Verify DELETE by ID " + e.name() + ": Not Found =====");

    verifyDeleteRequest(e.getUrl() + PATH_SEPARATOR + ID_NOT_FOUND, APPLICATION_JSON, 404);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  public void testDeleteInternalServerError(TestEntities e) {
    logger.info("===== Verify DELETE by ID " + e.name() + ": Internal Server Error =====");

    verifyDeleteRequest(e.getUrl() + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR, APPLICATION_JSON, 500);

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }
}
