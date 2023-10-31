package org.folio.service.organization;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.exception.HttpException;
import org.folio.rest.client.RequestContext;
import org.folio.rest.client.RestClient;
import org.folio.rest.jaxrs.model.BankingInformation;
import org.folio.rest.jaxrs.model.BankingInformationCollection;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.exception.ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY;
import static org.folio.util.ResourcePathResolver.*;
import static org.folio.util.ResourcePathResolver.resourceByIdPath;
import static org.folio.util.RestUtils.*;

@Service
public class BankingInformationServiceImpl implements BankingInformationService {

  private static final Logger logger = LogManager.getLogger(BankingInformationServiceImpl.class);
  public static final String GET_BANKING_INFORMATION_BY_QUERY = resourcesPath(BANKING_INFORMATION) + SEARCH_PARAMS;

  private final RestClient restClient;

  public BankingInformationServiceImpl(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public Future<BankingInformation> createBankingInformation(BankingInformation bankingInformation, Context context, Map<String, String> headers) {
    logger.debug("createBankingInformation:: Trying to create banking information with id: {}", bankingInformation.getId());
    RequestContext requestContext = new RequestContext(context, headers);
    return restClient.post(bankingInformation, resourcesPath(BANKING_INFORMATION), BankingInformation.class, requestContext);
  }

  @Override
  public Future<BankingInformation> getBankingInformationById(String id, Context context, Map<String, String> headers) {
    logger.debug("getBankingInformationById:: Trying to get banking information by id: {}", id);
    RequestContext requestContext = new RequestContext(context, headers);
    return restClient.get(resourceByIdPath(BANKING_INFORMATION, id), BankingInformation.class, requestContext);
  }

  @Override
  public Future<BankingInformationCollection> getBankingInformationCollection(int offset, int limit, String query, Context context, Map<String, String> headers) {
    logger.debug("getBankingInformationCollection:: Trying to get banking information collection with query: {}, offset: {}, limit: {}", query, offset, limit);
    RequestContext requestContext = new RequestContext(context, headers);
    String endpoint = String.format(GET_BANKING_INFORMATION_BY_QUERY, limit, offset, buildQuery(query));
    return restClient.get(endpoint, BankingInformationCollection.class, requestContext);
  }

  @Override
  public Future<Void> updateBankingInformation(String id, BankingInformation updatedBankingInformation, Context context, Map<String, String> headers) {
    logger.debug("updateBankingInformation:: Trying to update banking information with id: {}", id);
    RequestContext requestContext = new RequestContext(context, headers);
    if (isEmpty(updatedBankingInformation.getId())) {
      updatedBankingInformation.setId(id);
    } else if (!id.equals(updatedBankingInformation.getId())) {
      logger.warn("updateBankingInformation:: Mismatch between id '{}' in path and request body '{}'", id, updatedBankingInformation.getId());
      return Future.failedFuture(new HttpException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY.toInt(), MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.toError()));
    }
    return restClient.put(resourceByIdPath(BANKING_INFORMATION, updatedBankingInformation.getId()), updatedBankingInformation, requestContext);
  }

  @Override
  public Future<Void> deleteBankingInformation(String id, Context context, Map<String, String> headers) {
    logger.debug("deleteBankingInformation:: Trying to banking information by id: {}", id);
    RequestContext requestContext = new RequestContext(context, headers);
    return restClient.delete(resourceByIdPath(BANKING_INFORMATION, id), requestContext);
  }
}
