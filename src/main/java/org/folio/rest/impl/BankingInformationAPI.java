package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.folio.rest.jaxrs.model.BankingInformation;
import org.folio.rest.jaxrs.resource.OrganizationsBankingInformation;

import javax.ws.rs.core.Response;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.folio.service.organization.BankingInformationService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.config.Constants.OKAPI_URL;

public class BankingInformationAPI extends BaseApi implements OrganizationsBankingInformation {

  private static final Logger logger = LogManager.getLogger(BankingInformationAPI.class);
  private static final String BANKING_INFORMATION_LOCATION_PREFIX = "/organizations/banking-information/%s";

  @Autowired
  private BankingInformationService bankingInformationService;

  public BankingInformationAPI() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getOrganizationsBankingInformation(String query, String totalRecords, int offset, int limit, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    bankingInformationService.getBankingInformationCollection(offset, limit, query, vertxContext, okapiHeaders)
      .onSuccess(bankingInformation -> asyncResultHandler.handle(succeededFuture(buildOkResponse(bankingInformation))))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void postOrganizationsBankingInformation(BankingInformation entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to create banking information with id: {}", entity.getId());
    bankingInformationService.createBankingInformation(entity, vertxContext, okapiHeaders)
      .onSuccess(bankingInformation -> asyncResultHandler.handle(succeededFuture(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL),
        String.format(BANKING_INFORMATION_LOCATION_PREFIX, bankingInformation.getId()), bankingInformation))))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void putOrganizationsBankingInformationById(String id, BankingInformation entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to update banking information with id: {}", id);
    bankingInformationService.updateBankingInformation(id, entity, vertxContext, okapiHeaders)
      .onSuccess(vVoid -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void deleteOrganizationsBankingInformationById(String id, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to delete banking information by id: {}", id);
    bankingInformationService.deleteBankingInformation(id, vertxContext, okapiHeaders)
      .onSuccess(vVoid -> asyncResultHandler.handle(succeededFuture(buildNoContentResponse())))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }

  @Override
  public void getOrganizationsBankingInformationById(String id, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Trying to get banking information with id: {}", id);
    bankingInformationService.getBankingInformationById(id, vertxContext, okapiHeaders)
      .onSuccess(bankingInformation -> asyncResultHandler.handle(succeededFuture(buildOkResponse(bankingInformation))))
      .onFailure(t -> handleErrorResponse(asyncResultHandler, t));
  }
}
