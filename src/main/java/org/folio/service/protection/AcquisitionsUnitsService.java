package org.folio.service.protection;

import java.util.Map;

import io.vertx.core.Future;
import org.folio.rest.acq.model.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.AcquisitionsUnitMembershipCollection;

import io.vertx.core.Context;

public interface AcquisitionsUnitsService {
  Future<AcquisitionsUnitCollection> getAcquisitionsUnits(String query, int offset, int limit, Context context, Map<String, String> headers);
  Future<AcquisitionsUnitMembershipCollection> getAcquisitionsUnitsMemberships(String query, int offset, int limit, Context context, Map<String, String> headers);
  Future<String> buildAcqUnitsCqlClause(String query, int offset, int limit, Context context, Map<String, String> headers);
}
