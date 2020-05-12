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
import static org.folio.config.Constants.ID;
import static org.folio.rest.impl.ApiTestBase.PATH_SEPARATOR;
import static org.folio.rest.impl.ApiTestBase.X_OKAPI_TENANT;
import static org.folio.service.BaseService.SEARCH_PARAMS;
import static org.folio.service.BaseService.buildQuery;
import static org.folio.util.ResourcePathResolver.ORGANIZATIONS;
import static org.folio.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.util.ResourcePathResolver.resourcesPath;

import java.util.Objects;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MockServer {

  public static final String ID_EXPECTED = "1614c7e5-7b3e-4cb3-8fb9-48fe85c7b47f";
  public static final String ID_NOT_FOUND = "f394664e-849d-4213-96c4-2795f772ae3a";
  public static final String ID_INTERNAL_SERVER_ERROR = "96e79e5a-c379-4a5a-8244-d6df0342e21c";
  public static final String ISE_X_OKAPI_TENANT = "ISE";
  private static final Logger logger = LoggerFactory.getLogger(MockServer.class);
  public static WireMockServer wireMockServer;

  public static void init(int mockPort) {

    if (Objects.isNull(wireMockServer)) {
      wireMockServer = new WireMockServer(mockPort);
      wireMockServer.start();
    }

    for (TestEntities e : TestEntities.values()) {

      JsonObject expected = e.getSample()
        .copy();
      expected.put(ID, ID_EXPECTED);

      wireMockServer.stubFor(post(urlEqualTo(resourcesPath(e.getResource()))).willReturn(aResponse().withBody(expected.encode())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(201)));

      wireMockServer.stubFor(
          post(urlEqualTo(resourcesPath(e.getResource()))).withHeader(X_OKAPI_TENANT.getName(), containing(ISE_X_OKAPI_TENANT))
            .willReturn(aResponse().withHeader(CONTENT_TYPE, TEXT_PLAIN)
              .withStatus(500)));

      wireMockServer
        .stubFor(get(urlEqualTo(resourceByIdPath(e.getResource(), ID_EXPECTED))).willReturn(aResponse().withBody(expected.encode())
          .withHeader(CONTENT_TYPE, TEXT_PLAIN)
          .withStatus(200)));

      wireMockServer.stubFor(get(urlEqualTo(resourceByIdPath(e.getResource(), ID_NOT_FOUND)))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
          .withStatus(404)));

      wireMockServer.stubFor(get(urlEqualTo(resourceByIdPath(ORGANIZATIONS, ID_INTERNAL_SERVER_ERROR)))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
          .withStatus(500)));

      wireMockServer.stubFor(get(urlEqualTo(
          String.format(resourcesPath(e.getResource()) + SEARCH_PARAMS, 0, 1, buildQuery("id==" + ID_EXPECTED, logger), "en")))
            .willReturn(aResponse().withBody(e.getCollection()
              .encode())
              .withHeader(CONTENT_TYPE, APPLICATION_JSON)
              .withStatus(200)));

      wireMockServer.stubFor(get(urlEqualTo(String.format(resourcesPath(e.getResource()) + SEARCH_PARAMS, 0, 1,
          buildQuery("id==" + ID_INTERNAL_SERVER_ERROR, logger), "en")))
            .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
              .withStatus(500)));

      expected.put(e.getUpdatedFieldName(), e.getUpdatedFieldValue());

      wireMockServer.stubFor(put(urlEqualTo(resourcesPath(e.getResource()) + PATH_SEPARATOR + ID_EXPECTED)).willReturn(aResponse()
        .withBody(JsonObject.mapFrom(expected)
          .encode())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(204)));

      wireMockServer.stubFor(put(urlEqualTo(resourcesPath(e.getResource()) + PATH_SEPARATOR + ID_NOT_FOUND))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
          .withStatus(404)));

      wireMockServer.stubFor(put(urlEqualTo(resourcesPath(e.getResource()) + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR))
        .willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON)
          .withStatus(500)));

      wireMockServer.stubFor(delete(urlEqualTo(resourcesPath(e.getResource()) + PATH_SEPARATOR + ID_EXPECTED))
        .willReturn(aResponse().withStatus(204)));

      wireMockServer.stubFor(delete(urlEqualTo(resourcesPath(e.getResource()) + PATH_SEPARATOR + ID_NOT_FOUND))
        .willReturn(aResponse().withStatus(404)));

      wireMockServer.stubFor(delete(urlEqualTo(resourcesPath(e.getResource()) + PATH_SEPARATOR + ID_INTERNAL_SERVER_ERROR))
        .willReturn(aResponse().withStatus(500)));
    }
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
}
