package org.springframework.samples.petclinic.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MODULO 2 - Conversion de tipos en la frontera web.
 *
 * Registrar el {@link GenericIdToEntityConverter} en el FormatterRegistry hace
 * que cualquier controlador pueda declarar directamente un parametro de tipo
 * entidad y recibirlo ya cargado de la base de datos a partir de su id, sin
 * repetir la busqueda en cada metodo.
 *
 * Nota: aqui NO se declara ningun InternalResourceViewResolver. El prefijo y el
 * sufijo de las JSP se configuran con spring.mvc.view.prefix / .suffix en
 * application.properties. Tenerlo en los dos sitios a la vez (como ocurria en
 * el proyecto anterior) es duplicidad: quien lee el codigo no sabe cual manda.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final GenericIdToEntityConverter idToEntityConverter;

	public WebConfig(GenericIdToEntityConverter idToEntityConverter) {
		this.idToEntityConverter = idToEntityConverter;
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(idToEntityConverter);
	}

}
