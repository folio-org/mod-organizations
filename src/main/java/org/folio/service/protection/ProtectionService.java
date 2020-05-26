package org.folio.service.protection;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.Organization;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ProtectionService {
  CompletableFuture<Void> checkOperationsRestrictions(List<String> unitIds, Set<ProtectedOperationType> operations, String lang, Context context, Map<String, String> headers);
  CompletableFuture<Void> validateAcqUnitsOnUpdate(Organization updatedOrg, Organization currentOrg, String lang, Context context, Map<String, String> headers);
}
