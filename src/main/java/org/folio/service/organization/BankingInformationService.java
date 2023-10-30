package org.folio.service.organization;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.BankingInformation;
import org.folio.rest.jaxrs.model.BankingInformationCollection;

import java.util.Map;

public interface BankingInformationService {

  /**
   * This method creates {@link BankingInformation}
   *
   * @param bankingInformation bankingInformation
   * @param context            Vert.X context
   * @param headers            OKAPI headers
   * @return created {@link BankingInformation}
   */
  Future<BankingInformation> createBankingInformation(BankingInformation bankingInformation, Context context, Map<String, String> headers);

  /**
   * This method returns {@link BankingInformation} by ID
   *
   * @param id bankingInformation's id
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return {@link BankingInformation}
   */
  Future<BankingInformation> getBankingInformationById(String id, Context context, Map<String, String> headers);

  /**
   * This method returns {@link BankingInformationCollection} by query
   *
   * @param offset offset
   * @param limit limit
   * @param query query
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return collection of bankingInformation {@link BankingInformationCollection}
   */
  Future<BankingInformationCollection> getBankingInformationCollection(int offset, int limit, String query,
      Context context, Map<String, String> headers);

  /**
   * This method updates {@link BankingInformation} by ID
   * @param id updated bankingInformation's id
   * @param entity updated {@link BankingInformation} entity
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return void future
   */
  Future<Void> updateBankingInformation (String id, BankingInformation entity, Context context, Map<String, String> headers);

  /**
   * This method deletes {@link BankingInformation} by ID
   * @param id deleted bankingInformation's id
   * @param context Vert.X context
   * @param headers OKAPI headers
   * @return void future
   */
  Future<Void> deleteBankingInformation (String id, Context context, Map<String, String> headers);
}
