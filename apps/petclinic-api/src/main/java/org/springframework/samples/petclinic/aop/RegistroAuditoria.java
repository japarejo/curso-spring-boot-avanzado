package org.springframework.samples.petclinic.aop;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Registro de auditoria en memoria, para poder verlo en las pruebas.
 *
 * En una aplicacion real esto iria a una tabla, a un fichero aparte o a un
 * sistema de eventos. Solo guarda las ultimas {@value #MAXIMO} entradas.
 */
@Component
public class RegistroAuditoria {

	static final int MAXIMO = 500;

	private final Deque<EntradaAuditoria> entradas = new ArrayDeque<>();

	public synchronized void registrar(EntradaAuditoria entrada) {
		if (entradas.size() == MAXIMO) {
			entradas.removeFirst();
		}
		entradas.addLast(entrada);
	}

	/** Entradas de la mas antigua a la mas reciente. */
	public synchronized List<EntradaAuditoria> entradas() {
		return List.copyOf(entradas);
	}

	public synchronized void limpiar() {
		entradas.clear();
	}

}
