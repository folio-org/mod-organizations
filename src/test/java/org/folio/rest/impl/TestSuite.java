package org.folio.rest.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;

@ExtendWith(VertxExtension.class)
public class TestSuite {

  public static final int mockPort = NetworkUtils.nextFreePort();
  private static final int okapiPort = NetworkUtils.nextFreePort();
  public static boolean isInitialized = false;
  private static Vertx vertx;

  @BeforeAll
  public static void globalSetUp() throws InterruptedException, ExecutionException, TimeoutException {

    RestAssured.baseURI = "http://localhost:" + okapiPort;
    RestAssured.port = okapiPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    if (Objects.isNull(vertx)) {
      vertx = Vertx.vertx();
    }

    MockServer.init(mockPort);

    final JsonObject conf = new JsonObject();
    conf.put("http.port", okapiPort);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(conf);
    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();
    vertx.deployVerticle(RestVerticle.class.getName(), opt, res -> {
      if (res.succeeded()) {
        deploymentComplete.complete(res.result());
      } else {
        deploymentComplete.completeExceptionally(res.cause());
      }
    });
    deploymentComplete.get(60, TimeUnit.SECONDS);
    isInitialized = true;
  }

  @AfterAll
  public static void globalTearDown() {
    MockServer.destroy();
    if (Objects.nonNull(vertx)) {
      vertx.close();
    }
    isInitialized = false;
  }

  @Nested
  class TestOrganizationApi extends OrganizationApiTest {
  }

}
