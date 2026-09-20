package org.springframework.samples.petclinic.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApi30Config {

  private final String moduleName;
  private final String apiVersion;

  public OpenApi30Config(
      @Value("${spring.application.name:monolith}") String moduleName,
      @Value("${api.version:1.0.0}") String apiVersion) {
    this.moduleName = moduleName;
    this.apiVersion = apiVersion;
  }

  @Bean
  public OpenAPI customOpenAPI() {
    final String apiTitle = String.format("%s API", StringUtils.capitalize(moduleName));
    return new OpenAPI()
        .info(new Info().title(apiTitle).version(apiVersion));
  }
}