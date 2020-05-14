package org.folio.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourcePathResolver {

  public static final String ORGANIZATIONS = "organizations";
  private static final Map<String, String> SUB_OBJECT_ITEM_APIS;
  private static final Map<String, String> SUB_OBJECT_COLLECTION_APIS;

  static {
    Map<String, String> apis = new HashMap<>();
    apis.put(ORGANIZATIONS, "/organizations-storage/organizations");

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
