package org.folio.rest.client;

import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.config.Constants.OKAPI_URL;
import static org.folio.util.RestUtils.ID;

import java.util.Map;

import org.folio.exception.HttpException;
import org.folio.okapi.common.WebClientFactory;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpResponseExpectation;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RestClient {

  /**
   * A common method to create a new entry in the storage based on the Json Object.
   *
   * @param recordData json to post
   * @return future holding id of newly created entity Record or an exception if process failed
   */
  public <T> Future<T> post(T recordData, String endpoint, Class<T> responseType,
                            RequestContext requestContext) {
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.getHeaders());
    if (log.isDebugEnabled()) {
      log.debug("Trying to create object by endpoint '{}' and body '{}'", endpoint, JsonObject.mapFrom(recordData).encodePrettily());
    }
    return getVertxWebClient(requestContext.getContext())
      .postAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint)).putHeaders(caseInsensitiveHeader)
      .sendJson(recordData)
      .compose(RestClient::convertHttpResponse)
      .map(bufferHttpResponse -> {
        var id = verifyAndExtractRecordId(bufferHttpResponse);
        return bufferHttpResponse.bodyAsJsonObject()
          .put(ID, id)
          .mapTo(responseType);
      })
      .onFailure(t -> log.error("Object could not be created with using endpoint: {} and body: {}", endpoint, JsonObject.mapFrom(recordData).encodePrettily(), t));
  }

  /**
   * A common method to get an organization from the storage based on the Json Object.
   *
   * @return future jsonObject of created entity Record or an exception if failed
   */
  public <T> Future<T> get(String endpoint, Class<T> responseType, RequestContext requestContext) {
    log.debug("Calling GET {}", endpoint);
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.getHeaders());

    return getVertxWebClient(requestContext.getContext())
      .getAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
      .putHeaders(caseInsensitiveHeader)
      .send()
      .compose(RestClient::convertHttpResponse)
      .map(HttpResponse::bodyAsJsonObject)
      .map(jsonObject -> {
        if (log.isDebugEnabled()) {
          log.debug("Successfully retrieved: {}", jsonObject.encodePrettily());
        }
        return jsonObject.mapTo(responseType);
      });
  }

  /**
   * A common method to update an entry in the storage
   *
   * @param dataObject object to use for update operation
   * @param endpoint   endpoint
   */
  public <T> Future<Void> put(String endpoint, T dataObject, RequestContext requestContext) {
    var recordData = JsonObject.mapFrom(dataObject);
    if (log.isDebugEnabled()) {
      log.debug("Sending 'PUT {}' with body: {}", endpoint, recordData.encodePrettily());
    }
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.getHeaders());

    return getVertxWebClient(requestContext.getContext())
      .putAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
      .putHeaders(caseInsensitiveHeader)
      .sendJson(recordData)
      .compose(RestClient::convertHttpResponse)
      .onFailure(log::error)
      .mapEmpty();
  }

  /**
   * A common method to delete an entry in the storage
   *
   * @param endpoint endpoint
   */
  public Future<Void> delete(String endpoint, RequestContext requestContext) {
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.getHeaders());
    if (log.isDebugEnabled()) {
      log.debug("Trying to delete object with endpoint: {}", endpoint);
    }
    return getVertxWebClient(requestContext.getContext())
      .deleteAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
      .putHeaders(caseInsensitiveHeader)
      .send()
      .compose(RestClient::convertHttpResponse)
      .onFailure(t -> log.error("Object cannot be deleted with using endpoint: {}", endpoint, t))
      .mapEmpty();
  }

  private static <T> Future<HttpResponse<T>> convertHttpResponse(HttpResponse<T> response) {
    return HttpResponseExpectation.SC_SUCCESS.test(response)
      ? Future.succeededFuture(response)
      : Future.failedFuture(new HttpException(response.statusCode(), response.bodyAsString()));
  }

  private static String verifyAndExtractRecordId(HttpResponse<Buffer> response) {
    JsonObject body = response.bodyAsJsonObject();
    String id;
    if (body != null && !body.isEmpty() && body.containsKey(ID)) {
      id = body.getString(ID);
    } else {
      String location = response.getHeader(LOCATION);
      id = location.substring(location.lastIndexOf('/') + 1);
    }
    return id;
  }

  private static MultiMap convertToCaseInsensitiveMap(Map<String, String> okapiHeaders) {
    return MultiMap.caseInsensitiveMultiMap()
      .addAll(okapiHeaders)
      // set default Accept header
      .add("Accept", APPLICATION_JSON + ", " + TEXT_PLAIN);
  }

  private static WebClient getVertxWebClient(Context context) {
    WebClientOptions options = new WebClientOptions();
    options.setLogActivity(true);
    options.setKeepAlive(true);
    options.setConnectTimeout(2000);
    options.setIdleTimeout(5000);

    return WebClientFactory.getWebClient(context.owner(), options);
  }

  private static String buildAbsEndpoint(MultiMap okapiHeaders, String endpoint) {
    var okapiURL = okapiHeaders.get(OKAPI_URL);
    return okapiURL + endpoint;
  }

}
