-- Clientes de prueba: uno por cada ruta que ejercitan los pedidos de prueba
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (1, 'Laura Vega', 'VIP', NULL);
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (2, 'Carlos Pena', 'FRECUENTE', NULL);
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (3, 'Moroso SA', 'MOROSO', NULL);
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (4, 'Comercial Andina', 'ESTANDAR', '900123456-7');
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (5, 'Pedro Ruiz', 'ESTANDAR', NULL);

INSERT INTO productos (id, nombre, precio) VALUES (1, 'Teclado mecanico', 150000);
INSERT INTO productos (id, nombre, precio) VALUES (2, 'Mouse inalambrico', 60000);
INSERT INTO productos (id, nombre, precio) VALUES (3, 'Monitor 24 pulgadas', 700000);

INSERT INTO inventario (producto_id, stock) VALUES (1, 50);
INSERT INTO inventario (producto_id, stock) VALUES (2, 100);
INSERT INTO inventario (producto_id, stock) VALUES (3, 8);

-- Cliente 3 (Moroso SA) tiene una factura pendiente
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (3, 250000, false);

-- Cliente 2 (Carlos Pena) tiene 12 pedidos previos -> califica para el descuento FRECUENTE mas alto
INSERT INTO pedidos (id, cliente_id, subtotal, descuento, impuesto, total, fecha, estado)
SELECT 100 + x, 2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO'
FROM SYSTEM_RANGE(1, 12);
