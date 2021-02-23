package org.folio.service.protection;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.acq.model.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.AcquisitionsUnitMembershipCollection;

import io.vertx.core.Context;

public interface AcquisitionsUnitsService {
  CompletableFuture<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, String lang, Context context, Map<String, String> headers);
  CompletableFuture<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit, String lang, Context context, Map<String, String> headers);
  CompletableFuture<String> buildAcqUnitsCqlClause(String query, int offset, int limit, String lang, Context context, Map<String, String> headers);
}
