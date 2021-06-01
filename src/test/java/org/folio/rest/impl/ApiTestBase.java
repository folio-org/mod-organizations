package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.config.Constants.OKAPI_URL;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.TestSuite.isInitialized;
import static org.folio.rest.impl.TestSuite.mockPort;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;

public class ApiTestBase {

  public static final Header X_OKAPI_URL = new Header(OKAPI_URL, "http://localhost:" + mockPort);
  public static final Header X_OKAPI_TENANT = new Header(OKAPI_HEADER_TENANT, "organizations-test");
  public static final String PATH_SEPARATOR = "/";
  public static final String OKAPI_HEADER_PERMISSIONS = "X-Okapi-Permissions";

  @BeforeAll
  public static void globalSetUp() throws InterruptedException, ExecutionException, TimeoutException {
    if (!isInitialized) {
      TestSuite.globalSetUp();
    }
  }

  @AfterAll
  public static void globalTearDown() {
    MockServer.release();
    if (isInitialized) {
      TestSuite.globalTearDown();
    }
  }

  @BeforeEach
  public void setUp() {
    MockServer.resetRequests();
  }

  @AfterEach
  public void tearDown() {
    MockServer.resetRequests();
  }

  public Response verifyPostRequest(String url, String body, Headers headers, String expectedContentType, int expectedHttpCode) {
    return RestAssured.with()
      .headers(headers)
      .contentType(APPLICATION_JSON)
      .body(body)
      .post(url)
      .then()
      .log()
      .all()
      .statusCode(expectedHttpCode)
      .contentType(expectedContentType)
      .extract()
      .response();
  }

  public Response verifyPostRequest(String url, String body, String expectedContentType, int expectedHttpCode) {
    return verifyPostRequest(url, body, Headers.headers(X_OKAPI_TENANT, X_OKAPI_URL), expectedContentType, expectedHttpCode);
  }

  public Response verifyPostRequest(String url, String body) {
    return verifyPostRequest(url, body, APPLICATION_JSON, HttpStatus.HTTP_CREATED.toInt());
  }

  public Response verifyGetRequest(String url, Headers headers, String expectedContentType, int expectedHttpCode) {
    return RestAssured.with()
      .headers(headers)
      .contentType(APPLICATION_JSON)
      .get(url)
      .then()
      .log()
      .all()
      .statusCode(expectedHttpCode)
      .contentType(expectedContentType)
      .extract()
      .response();
  }

  public Response verifyGetRequest(String url, String expectedContentType, int expectedHttpCode) {
    return verifyGetRequest(url, Headers.headers(X_OKAPI_URL, X_OKAPI_TENANT), expectedContentType, expectedHttpCode);
  }


  public Response verifyPutRequest(String url, Object body, Headers headers, String expectedContentType, int expectedHttpCode) {
    return RestAssured.with()
      .headers(headers)
      .contentType(APPLICATION_JSON)
      .body(body)
      .put(url)
      .then()
      .log()
      .all()
      .statusCode(expectedHttpCode)
      .contentType(expectedContentType)
      .extract()
      .response();
  }

  public Response verifyPutRequest(String url, String body, String expectedContentType, int expectedHttpCode) {
    return verifyPutRequest(url, body, Headers.headers(X_OKAPI_URL, X_OKAPI_TENANT), expectedContentType, expectedHttpCode);
  }

  public Response verifyPutRequest(String url, String body) {
    return verifyPutRequest(url, body, Headers.headers(X_OKAPI_URL, X_OKAPI_TENANT), "", HttpStatus.HTTP_NO_CONTENT.toInt());
  }

  public Response verifyDeleteRequest(String url, Headers headers, String expectedContentType, int expectedHttpCode) {
    return RestAssured.with()
      .headers(headers)
      .contentType(APPLICATION_JSON)
      .delete(url)
      .then()
      .log()
      .all()
      .statusCode(expectedHttpCode)
      .contentType(expectedContentType)
      .extract()
      .response();
  }

  public Response verifyDeleteRequest(String url, String expectedContentType, int expectedHttpCode) {
    return verifyDeleteRequest(url, Headers.headers(X_OKAPI_URL, X_OKAPI_TENANT), expectedContentType, expectedHttpCode);
  }

  public Response verifyDeleteRequest(String url) {
    return verifyDeleteRequest(url, Headers.headers(X_OKAPI_URL, X_OKAPI_TENANT), "", HttpStatus.HTTP_NO_CONTENT.toInt());
  }

}
