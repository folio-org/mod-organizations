package org.folio.config;

import org.folio.service.organization.OrganizationService;
import org.folio.service.organization.OrganizationStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
  @Bean
  public OrganizationService organizationService() {
    return new OrganizationStorageService();
  }
}
