# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción

Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de
Software. Un único proyecto Spring Boot (`pedidos-service/`) con dos
partes: diagnóstico y refactorización de un antipatrón combinado en
`GestorPedidos`, y diagnóstico y corrección de un segundo antipatrón
introducido al hacer crecer el mismo proyecto con tres campañas de
descuento.

## Diagnóstico — Parte 1: `GestorPedidos`

`GestorPedidos.procesarPedido()` (línea 26, 134 líneas en total en la
clase) mezcla **seis responsabilidades** distintas en un único método,
sin que ninguna esté aislada del resto:

1. **Validación de stock** (líneas 31-44): una consulta SQL por cada
   ítem del pedido, embebida directamente en el método.
2. **Validación de cliente y mora** (líneas 46-65): una segunda
   consulta SQL, con una regla de excepción por horario anidada dentro
   del `if` de morosidad (hasta 3 niveles: tipo de cliente → moroso →
   deuda pendiente → horario de corte).
3. **Cálculo de subtotal** (líneas 67-73): una tercera consulta SQL,
   una por ítem, dentro del cálculo de precio.
4. **Cálculo de descuento** (líneas 75-93): un bloque condicional de
   hasta 3 niveles de anidamiento (tipo de cliente → rango de
   subtotal, o tipo de cliente → conteo de pedidos previos con una
   cuarta consulta SQL).
5. **Persistencia directa vía JDBC** (líneas 98-113): dos `INSERT` y
   un `UPDATE` ejecutados directamente desde el método, sin pasar por
   un Repository ni una transacción explícita.
6. **Notificación** (líneas 115-131): construcción del cuerpo del
   correo con un `StringBuilder` embebido en el mismo método, con el
   envío real delegado a `EmailService`.

**Esto es God Object y Spaghetti Code combinados**: `GestorPedidos` es
un God Object porque concentra responsabilidades que no tienen
relación funcional entre sí (persistencia, cálculo de negocio,
formato de texto, acceso a datos) en una sola clase; es además
Spaghetti Code porque, dentro de esa única clase, el método mezcla
niveles de abstracción muy distintos en la misma secuencia de líneas
(SQL crudo, reglas de negocio, formato de correo), sin ninguna
separación visual ni estructural entre ellos.

**Impacto concreto de agregar un nuevo tipo de cliente:** para agregar
un tipo `"CORPORATIVO"` con su propia regla de descuento, habría que
modificar el bloque de líneas 75-93 agregando un tercer `else if`
dentro del mismo método — sin poder probar esa regla de forma aislada
del resto del flujo (stock, mora, persistencia), porque todo vive en
el mismo método y comparte las mismas variables locales (`subtotal`,
`tipoCliente`, `descuento`).

## Decisiones de diseño

*(Se completa con el diagnóstico de la Parte 2 más abajo.)*

## Herramientas utilizadas

- Java 17, Spring Boot 3.2, Spring JDBC, H2 Database, Maven
- Git, GitHub
