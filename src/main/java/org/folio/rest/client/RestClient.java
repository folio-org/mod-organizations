package org.folio.rest.client;

import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.config.Constants.OKAPI_URL;
import static org.folio.util.ResourcePathResolver.ACQUISITIONS_MEMBERSHIPS;
import static org.folio.util.ResourcePathResolver.ACQUISITIONS_UNITS;
import static org.folio.util.ResourcePathResolver.CQL_SORT_BY_PATTERN;
import static org.folio.util.ResourcePathResolver.resourcesPath;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ErrorConverter;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.folio.exception.HttpException;
import org.folio.okapi.common.WebClientFactory;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;
import one.util.streamex.StreamEx;

public class RestClient {
  public static final String SEARCH_PARAMS = "?limit=%s&offset=%s%s&lang=%s";
  private static final String ID = "id";
  public static final String ACQUISITIONS_UNIT_ID = "acquisitionsUnitId";
  public static final String IS_DELETED_PROP = "isDeleted";
  public static final String ALL_UNITS_CQL = IS_DELETED_PROP + "=*";
  public static final String ACTIVE_UNITS_CQL = IS_DELETED_PROP + "==false";
  public static final String ACQUISITIONS_UNIT_IDS = "acqUnitIds";
  public static final String NO_ACQ_UNIT_ASSIGNED_CQL = "cql.allRecords=1 not " + ACQUISITIONS_UNIT_IDS + " <> []";
  public static final String GET_UNITS_BY_QUERY = resourcesPath(ACQUISITIONS_UNITS) + SEARCH_PARAMS;
  public static final String GET_UNITS_MEMBERSHIPS_BY_QUERY = resourcesPath(ACQUISITIONS_MEMBERSHIPS) + SEARCH_PARAMS;

  private static final ErrorConverter ERROR_CONVERTER = ErrorConverter.createFullBody(
    result -> new HttpException(result.response().statusCode(), result.response().bodyAsString()));
  private static final ResponsePredicate SUCCESS_RESPONSE_PREDICATE = ResponsePredicate.create(ResponsePredicate.SC_SUCCESS, ERROR_CONVERTER);

  public static String buildQuery(String query, Logger logger) {
    return isEmpty(query) ? EMPTY : "&query=" + encodeQuery(query, logger);
  }

  /**
   * @param query  string representing CQL query
   * @param logger {@link Logger} to log error if any
   * @return URL encoded string
   */
  public static String encodeQuery(String query, Logger logger) {
    try {
      return URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      logger.error("Error happened while attempting to encode '{}'", query, e);
      throw new CompletionException(e);
    }
  }

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
    try {
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
    } catch (Exception e) {
      logger.error("Error getting object by endpoint '{}'", endpoint);
      promise.fail(new CompletionException(e));
    }
    return promise.future();
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
    try {
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
    } catch (Exception e) {
      logger.error("Error updating object by endpoint: {}, body: {}", endpoint, recordData.encodePrettily(), e);
      promise.fail(new CompletionException(e));
    }
    return promise.future();
  }

  /**
   * A common method to delete an entry in the storage
   *
   * @param endpoint endpoint
   */
  public Future<Void> delete(String endpoint, RequestContext requestContext, Logger logger) {
    Promise<Void> promise = Promise.promise();
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.getHeaders());
    try {
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
    } catch (Exception e) {
      logger.error("Error deleting object by endpoint '{}'", endpoint, e);
      promise.fail(new CompletionException(e));
    }
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

  public static String combineCqlExpressions(String operator, String... expressions) {
    if (ArrayUtils.isEmpty(expressions)) {
      return EMPTY;
    }

    String sorting = EMPTY;

    // Check whether last expression contains sorting query. If it does, extract it to be added in the end of the resulting query
    Matcher matcher = CQL_SORT_BY_PATTERN.matcher(expressions[expressions.length - 1]);
    if (matcher.find()) {
      expressions[expressions.length - 1] = matcher.group(1);
      sorting = matcher.group(2);
    }

    return StreamEx.of(expressions)
      .filter(StringUtils::isNotBlank)
      .joining(") " + operator + " (", "(", ")") + sorting;
  }

  /**
   * Transform list of id's to CQL query using 'or' operation
   * @param ids list of id's
   * @return String representing CQL query to get records by id's
   */
  public static String convertIdsToCqlQuery(Collection<String> ids) {
    return convertIdsToCqlQuery(ids, ID, true);
  }

  public static String convertIdsToCqlQuery(Collection<String> values, String fieldName, boolean strictMatch) {
    String prefix = fieldName + (strictMatch ? "==(" : "=(");
    return StreamEx.of(values).joining(" or ", prefix, ")");
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
