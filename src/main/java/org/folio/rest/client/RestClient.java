package org.folio.rest.client;

import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.config.Constants.OKAPI_URL;
import static org.folio.util.RestUtils.ID;
import static org.folio.util.RestUtils.SUCCESS_RESPONSE_PREDICATE;

import java.util.Map;
import java.util.concurrent.CompletionException;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.WebClientFactory;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

public class RestClient {
   /**
   * A common method to create a new entry in the storage based on the Json Object.
   *
   * @param recordData json to post
   * @return future holding id of newly created entity Record or an exception if process failed
   */
  public <T> Future<T> post(T recordData, String endpoint, Class<T> responseType,
                            RequestContext requestContext, Logger logger) {
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.getHeaders());
      if (logger.isDebugEnabled()) {
        logger.debug("Trying to create object by endpoint '{}' and body '{}'", endpoint, JsonObject.mapFrom(recordData).encodePrettily());
      }
      return getVertxWebClient(requestContext.getContext())
        .postAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint)).putHeaders(caseInsensitiveHeader)
        .expect(SUCCESS_RESPONSE_PREDICATE)
        .sendJson(recordData)
        .map(bufferHttpResponse -> {
          var id = verifyAndExtractRecordId(bufferHttpResponse);
          return bufferHttpResponse.bodyAsJsonObject()
            .put(ID, id)
            .mapTo(responseType);
        });
  }

  /**
   * A common method to get an organization from the storage based on the Json Object.
   *
   * @return future jsonObject of created entity Record or an exception if failed
   */
  public Future<JsonObject> get(String endpoint, RequestContext requestContext, Logger logger) {
    Promise<JsonObject> promise = Promise.promise();
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.getHeaders());
      if(logger.isDebugEnabled()) {
        logger.debug("Trying to get object by endpoint '{}'", endpoint);
      }
      return getVertxWebClient(requestContext.getContext()).getAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
        .putHeaders(caseInsensitiveHeader)
        .expect(SUCCESS_RESPONSE_PREDICATE)
        .send()
        .map(HttpResponse::bodyAsJsonObject)
        .compose(jsonObject -> {
          if (logger.isDebugEnabled()) {
            logger.debug("Successfully retrieved: {}", jsonObject.encodePrettily());
          }
          promise.complete(jsonObject);
          return promise.future();
        })
        .onFailure(t -> {
          logger.error("Error getting object by endpoint '{}'", endpoint);
          promise.fail(new CompletionException(t));
        });
  }

  /**
   * A common method to update an entry in the storage
   *
   * @param recordData json to use for update operation
   * @param endpoint   endpoint
   */
  public Future<Void> put(String endpoint, JsonObject recordData, Logger logger, RequestContext context) {
    Promise<Void> promise = Promise.promise();
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(context.getHeaders());
      if(logger.isDebugEnabled()) {
        logger.debug("Trying to update object by endpoint '{}' and body '{}'", endpoint, recordData.encodePrettily());
      }
      return getVertxWebClient(context.getContext())
        .putAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
        .putHeaders(caseInsensitiveHeader)
        .expect(SUCCESS_RESPONSE_PREDICATE)
        .sendJson(recordData)
        .compose(response -> {
          if(logger.isDebugEnabled()) {
            logger.debug("Object was successfully updated. Record with '{}' id has been updated", endpoint);
          }
          promise.complete();
          return promise.future();
        })
        .onFailure(t -> {
          promise.fail(new CompletionException(t));
          logger.error("Object could not be updated with using endpoint: {} and body: {}", endpoint, recordData.encodePrettily(), t);
        });
  }

  /**
   * A common method to delete an entry in the storage
   *
   * @param endpoint endpoint
   */
  public Future<Void> delete(String endpoint, RequestContext requestContext, Logger logger) {
    Promise<Void> promise = Promise.promise();
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.getHeaders());
      if(logger.isDebugEnabled()) {
        logger.debug("Trying to delete object with endpoint: {}", endpoint);
      }
      getVertxWebClient(requestContext.getContext())
        .deleteAbs(buildAbsEndpoint(caseInsensitiveHeader, endpoint))
        .putHeaders(caseInsensitiveHeader)
        .expect(SUCCESS_RESPONSE_PREDICATE)
        .send()
        .compose(res -> {
          promise.complete();
        return promise.future();
        })
        .onFailure(t -> {
          logger.error("Object cannot be deleted with using endpoint: {}", endpoint, t);
          promise.fail(new CompletionException(t));

        });
    return promise.future();
  }

  private String verifyAndExtractRecordId(HttpResponse<Buffer> response) {
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

  private MultiMap convertToCaseInsensitiveMap(Map<String, String> okapiHeaders) {
    return MultiMap.caseInsensitiveMultiMap()
      .addAll(okapiHeaders)
      // set default Accept header
      .add("Accept", APPLICATION_JSON + ", " + TEXT_PLAIN);
  }

  private WebClient getVertxWebClient(Context context) {
    WebClientOptions options = new WebClientOptions();
    options.setLogActivity(true);
    options.setKeepAlive(true);
    options.setConnectTimeout(2000);
    options.setIdleTimeout(5000);

    return WebClientFactory.getWebClient(context.owner(), options);
  }

  private String buildAbsEndpoint(MultiMap okapiHeaders, String endpoint) {
    var okapiURL = okapiHeaders.get(OKAPI_URL);
    return okapiURL + endpoint;
  }

}
