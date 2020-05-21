package org.folio.service.protection;

import io.vertx.core.Context;
import org.folio.rest.acq.model.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.AcquisitionsUnitMembershipCollection;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AcquisitionUnitsService {
  CompletableFuture<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, String lang, Context context, Map<String, String> headers);
  CompletableFuture<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit, String lang, Context context, Map<String, String> headers);
}
