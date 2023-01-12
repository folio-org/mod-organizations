package org.folio.util;

import io.vertx.ext.web.client.predicate.ErrorConverter;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.folio.exception.HttpException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.util.ResourcePathResolver.*;

public class RestUtils {
  public static final String SEARCH_PARAMS = "?limit=%s&offset=%s%s&lang=%s";
  public static final String ID = "id";
  public static final String ACQUISITIONS_UNIT_ID = "acquisitionsUnitId";
  public static final String IS_DELETED_PROP = "isDeleted";
  public static final String ALL_UNITS_CQL = IS_DELETED_PROP + "=*";
  public static final String ACTIVE_UNITS_CQL = IS_DELETED_PROP + "==false";
  public static final String ACQUISITIONS_UNIT_IDS = "acqUnitIds";
  public static final String NO_ACQ_UNIT_ASSIGNED_CQL = "cql.allRecords=1 not " + ACQUISITIONS_UNIT_IDS + " <> []";
  public static final String GET_UNITS_BY_QUERY = resourcesPath(ACQUISITIONS_UNITS) + SEARCH_PARAMS;
  public static final String GET_UNITS_MEMBERSHIPS_BY_QUERY = resourcesPath(ACQUISITIONS_MEMBERSHIPS) + SEARCH_PARAMS;

  private RestUtils() {}

  public static final ErrorConverter ERROR_CONVERTER = ErrorConverter.createFullBody(
    result -> new HttpException(result.response().statusCode(), result.response().bodyAsString()));
  public static final ResponsePredicate SUCCESS_RESPONSE_PREDICATE = ResponsePredicate.create(ResponsePredicate.SC_SUCCESS, ERROR_CONVERTER);

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
   * Transform list of id's to CQL query using 'or' operation
   * @param ids list of id's
   * @return String representing CQL query to get records by id's
   */
  public static String convertIdsToCqlQuery(Collection<String> ids) {
    return convertIdsToCqlQuery(ids, ID, true);
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

  public static String convertIdsToCqlQuery(Collection<String> values, String fieldName, boolean strictMatch) {
    String prefix = fieldName + (strictMatch ? "==(" : "=(");
    return StreamEx.of(values).joining(" or ", prefix, ")");
  }
}
