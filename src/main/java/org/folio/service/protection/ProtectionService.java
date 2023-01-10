package org.folio.service.protection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.Organization;

import io.vertx.core.Context;

public interface ProtectionService {
  Future<Void> checkOperationsRestrictions(List<String> unitIds, Set<ProtectedOperationType> operations, String lang, Context context, Map<String, String> headers);
  Future<Void> validateAcqUnitsOnUpdate(Organization updatedOrg, Organization currentOrg, String lang, Context context, Map<String, String> headers);
}
