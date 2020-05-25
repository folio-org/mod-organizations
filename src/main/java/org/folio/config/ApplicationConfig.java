package org.folio.config;

import org.folio.service.organization.OrganizationService;
import org.folio.service.organization.OrganizationStorageService;
import org.folio.service.protection.AcquisitionsUnitsServiceImpl;
import org.folio.service.protection.AcquisitionsUnitsService;
import org.folio.service.protection.ProtectionService;
import org.folio.service.protection.ProtectionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
  @Bean
  public OrganizationService organizationService() {
    return new OrganizationStorageService();
  }

  @Bean
  public ProtectionService protectionService() {
    return new ProtectionServiceImpl();
  }

  @Bean
  public AcquisitionsUnitsService acquisitionUnitsService() {
    return new AcquisitionsUnitsServiceImpl();
  }
}
