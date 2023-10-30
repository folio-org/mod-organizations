package org.folio.rest.impl;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.restassured.http.Headers;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.config.Constants.ID;
import static org.folio.rest.impl.TestEntities.BANKING_INFORMATION_ENTITY;
import static wiremock.org.hamcrest.MatcherAssert.assertThat;
import static wiremock.org.hamcrest.Matchers.*;

public class BankingInformationAPITest extends ApiTestBase {

  private static final Logger logger = LogManager.getLogger(BankingInformationAPITest.class);

  @Test
  void shouldSuccessfullyPost() {
    logger.info("===== Verify POST : Successful =====");

    JsonObject posted = BANKING_INFORMATION_ENTITY.getSample();
    assertThat(posted.getString(ID), nullValue());

    JsonObject actual = new JsonObject(verifyPostRequest(BANKING_INFORMATION_ENTITY.getUrl(), JsonObject.mapFrom(posted)
      .encode()).getBody()
      .print());

    assertThat(actual.getString(ID), is(BANKING_INFORMATION_ENTITY.getId()));
    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }

  @Test
  void shouldThrowValidationException() {
    logger.info("===== Verify POST: Unprocessable Entity Error =====");

    JsonObject posted = BANKING_INFORMATION_ENTITY.getSample();
    posted.putNull("organizationId");

    Headers headers = Headers.headers(X_OKAPI_URL, X_OKAPI_TENANT);
    verifyPostRequest(BANKING_INFORMATION_ENTITY.getUrl(), posted
      .encode(), headers, APPLICATION_JSON, HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt());

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(0));
  }

  @Test
  void shouldSuccessfullyGet() {
    logger.info("===== Verify GET : Successful =====");

    Headers headers = Headers.headers(X_OKAPI_URL, X_OKAPI_TENANT);
    JsonObject expected = BANKING_INFORMATION_ENTITY.getSample();
    expected.put(ID, BANKING_INFORMATION_ENTITY.getId());

    String url = BANKING_INFORMATION_ENTITY.getUrl() + PATH_SEPARATOR + BANKING_INFORMATION_ENTITY.getId();
    JsonObject actual = new JsonObject(verifyGetRequest(url, headers, APPLICATION_JSON, HttpStatus.HTTP_OK.toInt()).getBody()
      .print());
    assertThat(actual, equalTo(expected));
  }

  @Test
  void shouldSuccessfullyPut() {
    logger.info("===== Verify PUT : Successful =====");

    JsonObject expected = BANKING_INFORMATION_ENTITY.getSample();
    expected.put(BANKING_INFORMATION_ENTITY.getUpdatedFieldName(), BANKING_INFORMATION_ENTITY.getUpdatedFieldValue());

    verifyPutRequest(BANKING_INFORMATION_ENTITY.getUrl() + PATH_SEPARATOR + BANKING_INFORMATION_ENTITY.getId(), JsonObject.mapFrom(expected)
      .encode());

    assertThat(MockServer.getInstance().getAllServeEvents().stream()
      .filter(event -> event.getRequest().getMethod().equals(RequestMethod.PUT))
      .collect(Collectors.toList()), hasSize(1));
  }

  @Test
  void shouldSuccessfullyDelete() {
    logger.info("===== Verify DELETE : Successful =====");

    verifyDeleteRequest(BANKING_INFORMATION_ENTITY.getUrl() + PATH_SEPARATOR + BANKING_INFORMATION_ENTITY.getId());

    assertThat(MockServer.getInstance()
      .getAllServeEvents(), hasSize(1));
  }
}
