package org.folio.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResourcePathResolver {

  public static final String ACQUISITIONS_UNITS = "acquisitionsUnits";
  public static final String ACQUISITIONS_MEMBERSHIPS = "acquisitionsMemberships";
  public static final Pattern CQL_SORT_BY_PATTERN = Pattern.compile("(.*)(\\ssortBy\\s.*)", Pattern.CASE_INSENSITIVE);
  public static final String ORGANIZATIONS = "organizations";
  public static final String BANKING_INFORMATION = "bankingInformation";
  private static final Map<String, String> SUB_OBJECT_ITEM_APIS;
  private static final Map<String, String> SUB_OBJECT_COLLECTION_APIS;

  static {
    Map<String, String> apis = new HashMap<>();
    apis.put(ORGANIZATIONS, "/organizations-storage/organizations");
    apis.put(BANKING_INFORMATION, "/organizations-storage/banking-information");
    apis.put(ACQUISITIONS_UNITS, "/acquisitions-units-storage/units");
    apis.put(ACQUISITIONS_MEMBERSHIPS, "/acquisitions-units-storage/memberships");

    SUB_OBJECT_COLLECTION_APIS = Collections.unmodifiableMap(apis);
    SUB_OBJECT_ITEM_APIS = Collections.unmodifiableMap(apis.entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() + "/")));
  }

  private ResourcePathResolver() {
  }

  public static String resourcesPath(String field) {
    return SUB_OBJECT_COLLECTION_APIS.get(field);
  }

  public static String resourceByIdPath(String field) {
    return SUB_OBJECT_ITEM_APIS.get(field);
  }

  public static String resourceByIdPath(String field, String id) {
    return SUB_OBJECT_ITEM_APIS.get(field) + id;
  }
}
