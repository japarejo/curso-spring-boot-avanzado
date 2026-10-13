package com.idexaformacion.cursos.mapeos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Laboratorio de mapeos. No tiene controladores: se trabaja con las pruebas de
 * src/test y leyendo target/ddl-generado.sql.
 *
 * Cada subpaquete corresponde a una diapositiva del modulo 6:
 *   unoauno        155  Pedido 1 - 1 Factura (+ clave con SEQUENCE, 166)
 *   unoamuchos     156  Departamento 1 - N Empleado
 *   muchosamuchos  157  Desarrollador N - M Tarea
 *   herencia       158  Trabajador, TrabajadorFijo, TrabajadorPorHoras
 *   jointable      167-168  Articulo con autores y revisores (dos N-M a la misma entidad)
 *   reflexivas     169-170  Carpeta (arbol) e Investigador (directores de tesis)
 */
@SpringBootApplication
public class MapeosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MapeosApplication.class, args);
	}

}
