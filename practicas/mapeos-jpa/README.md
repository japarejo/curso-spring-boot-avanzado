# Laboratorio de mapeos JPA

Material del **ejercicio 8 del módulo 6**. Una entidad, o pareja de entidades, por cada
diapositiva de mapeo, con pruebas que las ejercitan. Nada de controladores ni servicios: aquí
solo importa **qué tablas y columnas** sale de cada anotación.

| Paquete | Diapositiva | Qué muestra |
|---|---|---|
| `unoauno` | 155, 166 | 1:1 bidireccional `Pedido`–`Factura`; clave con `SEQUENCE` y `allocationSize` |
| `unoamuchos` | 156 | 1:N `Departamento`–`Empleado` con `mappedBy`, cascada y `orphanRemoval` |
| `muchosamuchos` | 157 | N:M `Desarrollador`–`Tarea`; lado propietario frente a lado inverso |
| `herencia` | 158 | `SINGLE_TABLE` con columna discriminadora; polimorfismo |
| `jointable` | 167–168 | Dos N:M entre `Articulo` e `Investigador` (autores y revisores) |
| `reflexivas` | 169–170 | Árbol de `Carpeta`; directores y doctorandos entre `Investigador` |

Cada clase lleva en su Javadoc la explicación, y las **erratas** de la diapositiva
correspondiente.

## Cómo se usa

Lleva su propio Maven embebido. Desde esta carpeta:

```bash
./mvnw test              # Windows: .\mvnw test
```

Después, abre **`target/ddl-generado.sql`**: es el DDL que Hibernate ha generado a partir de
las anotaciones. Ese fichero es el protagonista del laboratorio.

Con la consola abierta se ven además las sentencias SQL de cada prueba.

## Ejercicios

El enunciado completo está en
[`modulos/06-spring-data-jpa/ejercicios.md`](../../modulos/06-spring-data-jpa/ejercicios.md),
ejercicio 8. En resumen: romper cada mapeo a propósito, ejecutar y explicar el cambio en el
DDL y en el SQL.
