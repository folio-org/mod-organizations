package org.folio.rest.impl;

import static org.folio.rest.impl.MockServer.ACQ_UNIT_FULL_PROTECTED_ID;
import static org.folio.rest.impl.MockServer.ACQ_UNIT_READ_ONLY_ID;
import static org.folio.rest.impl.MockServer.FULL_PROTECTED_USER_ID;
import static org.folio.rest.impl.MockServer.READ_ONLY_USER_ID;

import io.vertx.core.json.JsonObject;
import org.folio.rest.acq.model.AcquisitionsUnit;
import org.folio.rest.acq.model.AcquisitionsUnitCollection;
import org.folio.rest.acq.model.AcquisitionsUnitMembership;
import org.folio.rest.acq.model.AcquisitionsUnitMembershipCollection;

import java.util.Collections;
import java.util.UUID;

public enum MockAcqUnits {
  READ_ONLY(ACQ_UNIT_READ_ONLY_ID, READ_ONLY_USER_ID, false, true, false, true, true),
  FULL_PROTECTED(ACQ_UNIT_FULL_PROTECTED_ID, FULL_PROTECTED_USER_ID, false, true, true, true, true);

  String acqUnitId;
  String userId;
  boolean deleted;
  boolean protectCreate;
  boolean protectRead;
  boolean protectUpdate;
  boolean protectDelete;

  MockAcqUnits(String acqUnitId, String userId, boolean deleted, boolean protectCreate, boolean protectRead, boolean protectUpdate, boolean protectDelete) {
    this.acqUnitId = acqUnitId;
    this.userId = userId;
    this.deleted = deleted;
    this.protectCreate = protectCreate;
    this.protectRead = protectRead;
    this.protectUpdate = protectUpdate;
    this.protectDelete = protectDelete;
  }

  public String getAcqUnitsCollection() {
    return JsonObject.mapFrom(
      new AcquisitionsUnitCollection()
        .withAcquisitionsUnits(Collections.singletonList(
          new AcquisitionsUnit()
            .withId(acqUnitId)
            .withIsDeleted(deleted)
            .withProtectCreate(protectCreate)
            .withProtectRead(protectRead)
            .withProtectUpdate(protectUpdate)
            .withProtectDelete(protectDelete)))
        .withTotalRecords(1))
      .encode();
  }

  public static String getEmptyAcqUnitMembershipCollection() {
    return JsonObject.mapFrom(new AcquisitionsUnitMembershipCollection().withTotalRecords(0)).encode();
  }

  public String getAcqUnitMembershipCollection() {
    return JsonObject.mapFrom(
      new AcquisitionsUnitMembershipCollection()
        .withAcquisitionsUnitMemberships(Collections.singletonList(
          new AcquisitionsUnitMembership()
            .withId(UUID.randomUUID().toString())
            .withAcquisitionsUnitId(acqUnitId)
            .withUserId(userId)))
        .withTotalRecords(1))
      .encode();
  }
}
