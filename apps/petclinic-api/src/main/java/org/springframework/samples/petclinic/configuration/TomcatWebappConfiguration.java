package org.springframework.samples.petclinic.configuration;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatWebappConfiguration {

	@Bean
	WebServerFactoryCustomizer<TomcatServletWebServerFactory> webappDocumentRootCustomizer(
			@Value("${petclinic.webapp.document-root:/app/webapp}") String documentRoot) {

		return factory -> {
			File root = new File(documentRoot);
			if (root.isDirectory()) {
				factory.setDocumentRoot(root);
			}
		};
	}

}
