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

### Parte 1 — `GestorPedidos`

**Antipatrón identificado:** God Object y Spaghetti Code combinados
(ver evidencia citada arriba).

**Patrón aplicado:** Chain of Responsibility para las validaciones de
stock y cliente (`ValidadorStock` → `ValidadorCliente`) y Strategy
para el cálculo de descuento por tipo de cliente
(`EstrategiaDescuento` con `DescuentoVip`, `DescuentoFrecuente`,
`DescuentoEstandar`, seleccionados por `SelectorEstrategiaDescuento`).

**Por qué Chain of Responsibility para las validaciones:** las dos
validaciones tienen una dependencia real de orden y de corte
anticipado — si `ValidadorStock` rechaza el pedido, `ValidadorCliente`
ni siquiera debe ejecutarse (consultar su mora sería una consulta SQL
desperdiciada sobre un pedido que ya no va a proceder). Alternativa
descartada: un método `validarTodo()` con una lista de
`Predicate<ContextoPedido>`, que evaluaría ambos predicados aunque el
primero ya hubiera fallado, sin ofrecer un mecanismo real de corte
anticipado.

**Por qué Strategy y no un eslabón más de la cadena para el
descuento:** a diferencia de las validaciones, las reglas de
descuento no dependen de un orden de evaluación entre sí ni necesitan
"cortar" el flujo — siempre se aplica exactamente una regla,
determinada por el tipo de cliente. Modelarlo como un eslabón más de
la cadena habría mezclado dos responsabilidades con propiedades
distintas (decidir si el pedido continúa vs. calcular cuánto
descuento recibe) en la misma jerarquía de clases.

Con esta separación, `GestorPedidos` pasa de 134 líneas con SQL,
reglas de negocio y formato de texto mezclados en un único método, a
un orquestador de ~70 líneas que delega cada responsabilidad a su
propia clase (`PedidoRepository` para persistencia,
`NotificacionPedidoService` para notificación).

### Parte 2 — Crecimiento del proyecto: tres campañas de descuento

**Contexto:** dos semanas después de cerrada la Parte 1, se agregaron
tres campañas de descuento (`PromocionBlackFriday`,
`PromocionCorporativo`, `PromocionVolumen`) como tres eslabones
nuevos de la misma cadena `ValidadorPedido` que ya tenía
`ValidadorStock` y `ValidadorCliente`.

**Antipatrón identificado:** Golden Hammer. La evidencia concreta:

- Ninguna de las tres clases nuevas tiene una dependencia de orden
  real entre sí, ni con `ValidadorStock`/`ValidadorCliente`: evaluar
  `PromocionVolumen` antes que `PromocionCorporativo` no cambia el
  resultado — a diferencia de `ValidadorStock`, que sí debe
  ejecutarse antes que `ValidadorCliente` para evitar una consulta de
  mora sobre un pedido que ya iba a rechazarse por falta de stock.
- `ValidadorPedido` tiene un contrato claro ("decidir si el pedido
  continúa o se rechaza"), pero las tres clases nuevas nunca llaman a
  `contexto.rechazar(...)` — solo escriben en el campo compartido
  `descuentoCampana`. Extender una clase pensada para rechazar un
  pedido con clases que nunca rechazan nada es la señal más directa
  de que se reutilizó la herramienta equivocada.
- Si dos campañas necesitaran combinarse (sumar en vez de competir
  por el máximo), el diseño actual no lo permite sin ambigüedad: las
  tres escriben sobre el mismo campo `descuentoCampana` con
  `aplicarDescuentoCampana`, que solo conserva el valor más alto.
- La razón real de la elección no fue evaluar la forma del problema,
  sino que "`ValidadorStock` y `ValidadorCliente` funcionaron muy
  bien como `Chain of Responsibility`" — Golden Hammer es
  exactamente esto: aplicar una solución conocida a un problema con
  una forma distinta, sin evaluar si corresponde.

**Patrón aplicado en la corrección:** Strategy, extendiendo
`SelectorEstrategiaDescuento` con un nuevo `CalculadorDescuentoFinal`
que combina el descuento por tipo de cliente con el de campañas (ver
código y commits siguientes). Las tres clases `PromocionBlackFriday`,
`PromocionCorporativo`, `PromocionVolumen` y el campo
`descuentoCampana` se eliminan por completo del código — no se dejan
comentadas, porque hacerlo reintroduciría el riesgo de Lava Flow
descrito en la guía de esta unidad: nadie se atreve a borrar código
comentado "por si acaso", y su función deja de estar clara con el
tiempo. Su historial queda documentado únicamente en los commits de
este repositorio.

## Herramientas utilizadas

- Java 17, Spring Boot 3.2, Spring JDBC, H2 Database, Maven
- Git, GitHub
