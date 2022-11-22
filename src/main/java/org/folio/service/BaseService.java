package org.folio.service;

import static java.util.Objects.nonNull;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.config.Constants.OKAPI_URL;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.util.ResourcePathResolver.ACQUISITIONS_MEMBERSHIPS;
import static org.folio.util.ResourcePathResolver.ACQUISITIONS_UNITS;
import static org.folio.util.ResourcePathResolver.resourcesPath;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.folio.exception.HttpException;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import one.util.streamex.StreamEx;

public abstract class BaseService {

  public static final String SEARCH_PARAMS = "?limit=%s&offset=%s%s&lang=%s";
  private static final String ERROR_MESSAGE = "errorMessage";
  private static final String ID = "id";
  public static final String ACQUISITIONS_UNIT_ID = "acquisitionsUnitId";
  public static final String IS_DELETED_PROP = "isDeleted";
  public static final String ALL_UNITS_CQL = IS_DELETED_PROP + "=*";
  public static final String ACTIVE_UNITS_CQL = IS_DELETED_PROP + "==false";
  private static final Pattern CQL_SORT_BY_PATTERN = Pattern.compile("(.*)(\\ssortBy\\s.*)", Pattern.CASE_INSENSITIVE);
  public static final String ACQUISITIONS_UNIT_IDS = "acqUnitIds";
  public static final String NO_ACQ_UNIT_ASSIGNED_CQL = "cql.allRecords=1 not " + ACQUISITIONS_UNIT_IDS + " <> []";
  public static final String GET_UNITS_BY_QUERY = resourcesPath(ACQUISITIONS_UNITS) + SEARCH_PARAMS;
  public static final String GET_UNITS_MEMBERSHIPS_BY_QUERY = resourcesPath(ACQUISITIONS_MEMBERSHIPS) + SEARCH_PARAMS;

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
   * Some requests do not have body and in happy flow do not produce response body. The Accept header is required for calls to
   * storage
   */
  private static void setDefaultHeaders(HttpClientInterface httpClient) {
    // The RMB's HttpModuleClient2.ACCEPT is in sentence case. Using the same format to avoid duplicates (issues migrating to RMB
    // 27.1.1)
    httpClient.setDefaultHeaders(Collections.singletonMap("Accept", APPLICATION_JSON + ", " + TEXT_PLAIN));
  }

  public HttpClientInterface getHttpClient(Map<String, String> okapiHeaders, boolean setDefaultHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

    HttpClientInterface httpClient = HttpClientFactory.getHttpClient(okapiURL, tenantId);

    // Some requests do not have body and in happy flow do not produce response body. The Accept header is required for calls to
    // storage
    if (setDefaultHeaders) {
      setDefaultHeaders(httpClient);
    }
    return httpClient;
  }

  public HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    return getHttpClient(okapiHeaders, true);
  }

  /**
   * A common method to create a new entry in the storage based on the Json Object.
   *
   * @param recordData json to post
   * @return completable future holding id of newly created entity Record or an exception if process failed
   */
  public CompletableFuture<String> handlePostRequest(JsonObject recordData, String endpoint, HttpClientInterface httpClient,
      Context ctx, Map<String, String> okapiHeaders, Logger logger) {
    CompletableFuture<String> future = new CompletableFuture<>();
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Trying to create object by endpoint '{}' and body '{}'", endpoint, recordData.encodePrettily());
      }
      httpClient.request(HttpMethod.POST, recordData.toBuffer(), endpoint, okapiHeaders)
        .thenApply(this::verifyAndExtractRecordId)
        .thenAccept(id -> {
          future.complete(id);
          logger.debug("Object was successfully created. Record with '{}' id has been created", id);
        })
        .exceptionally(throwable -> {
          future.completeExceptionally(throwable);
          logger.error("Object could not be created with using endpoint: {} and body: {}", endpoint, recordData.encodePrettily(), throwable);
          return null;
        });
    } catch (Exception e) {
      logger.error("Error creating object by endpoint '{}' and body '{}'", endpoint, recordData.encodePrettily());
      future.completeExceptionally(e);
    }
    return future;
  }

  public CompletableFuture<JsonObject> handleGetRequest(String endpoint, HttpClientInterface httpClient,
      Map<String, String> okapiHeaders, Logger logger) {

    CompletableFuture<JsonObject> future = new CompletableFuture<>();
    try {
      logger.debug("Trying to get '{}' object", endpoint);
      httpClient.request(HttpMethod.GET, endpoint, okapiHeaders)
        .thenApply(response -> {
          logger.info("Validating response for get request '{}'", endpoint);
          return verifyAndExtractBody(response);
        })
        .thenAccept(body -> {
          if (logger.isInfoEnabled()) {
            logger.info("The response body for get request '{}', body: {}", endpoint, nonNull(body) ? body.encodePrettily() : null);
          }
          future.complete(body);
        })
        .exceptionally(t -> {
          logger.error("Object could not be retrieved with using endpoint: {}", endpoint, t);
          future.completeExceptionally(t);
          return null;
        });
    } catch (Exception e) {
      logger.error("Error retrieving object by endpoint '{}'", endpoint, e);
      future.completeExceptionally(e);
    }
    return future;
  }

  /**
   * A common method to update an entry in the storage
   *
   * @param recordData json to use for update operation
   * @param endpoint   endpoint
   */
  public CompletableFuture<Void> handlePutRequest(String endpoint, JsonObject recordData, HttpClientInterface httpClient,
       Map<String, String> okapiHeaders, Logger logger) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Trying to update object by endpoint '{}' and body '{}'", endpoint, recordData.encodePrettily());
      }
      httpClient.request(HttpMethod.PUT, recordData.toBuffer(), endpoint, okapiHeaders)
        .thenApply(this::verifyAndExtractBody)
        .thenAccept(response -> {
          logger.info("Object was successfully updated. Record with '{}' id has been updated", endpoint);
          future.complete(null);
        })
        .exceptionally(e -> {
          future.completeExceptionally(e);
          logger.error("Object could not be updated with using endpoint: {} and body: {}", endpoint, recordData.encodePrettily(), e);
          return null;
        });
    } catch (Exception e) {
      logger.error("Error updating object by endpoint: {}, body: {}", endpoint, recordData.encodePrettily(), e);
      future.completeExceptionally(e);
    }
    return future;
  }

  /**
   * A common method to delete an entry in the storage
   *
   * @param endpoint endpoint
   */
  public CompletableFuture<Void> handleDeleteRequest(String endpoint, HttpClientInterface httpClient,
      Map<String, String> okapiHeaders, Logger logger) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    logger.debug("Trying to delete object with endpoint: {}", endpoint);
    try {
      httpClient.request(HttpMethod.DELETE, endpoint, okapiHeaders)
        .thenAccept(this::verifyResponse)
        .thenApply(future::complete)
        .exceptionally(t -> {
          logger.error("Object cannot be deleted with using endpoint: {}", endpoint, t);
          future.completeExceptionally(t);
          return null;
        });
    } catch (Exception e) {
      logger.error("Error deleting object by endpoint '{}'", endpoint, e);
      future.completeExceptionally(e);
    }
    return future;
  }

  public JsonObject verifyAndExtractBody(org.folio.rest.tools.client.Response response) {
    if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
      throw new HttpException(response.getCode(), response.getError()
        .getString(ERROR_MESSAGE));
    }
    return response.getBody();
  }

  public void verifyResponse(org.folio.rest.tools.client.Response response) {
    if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
      throw new CompletionException(new HttpException(response.getCode(), response.getError()
        .getString(ERROR_MESSAGE)));
    }
  }

  private String verifyAndExtractRecordId(org.folio.rest.tools.client.Response response) {
    JsonObject body = verifyAndExtractBody(response);
    String id;
    if (body != null && !body.isEmpty() && body.containsKey(ID)) {
      id = body.getString(ID);
    } else {
      String location = response.getHeaders()
        .get(LOCATION);
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
}
