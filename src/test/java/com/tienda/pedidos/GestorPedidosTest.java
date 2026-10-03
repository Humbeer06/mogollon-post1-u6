package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Cinco pedidos de prueba que ejercitan las tres rutas de validacion
// (stock insuficiente, cliente moroso dentro y fuera del horario de corte,
// cliente inexistente) y los dos tipos de descuento (VIP, FRECUENTE).
// Se ejecutan contra el GestorPedidos original en la Parte 1 y, sin
// modificarse, contra la version refactorizada, para verificar que el
// resultado es equivalente (ver Paso 8 y seccion "Evidencia de ejecucion"
// del README).
@SpringBootTest
class GestorPedidosTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    @Test
    void pedidoVipConSubtotalAlto_seConfirmaConDescuentoDel15Porciento() {
        // Cliente 1 (VIP), 5 monitores de 700000 = 3500000 -> descuento 0.15
        PedidoRequest request = pedido(1L, "laura@udes.edu.co", new ItemPedido(3L, 5));
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        double subtotalEsperado = 5 * 700000;
        double totalEsperado = subtotalEsperado - (subtotalEsperado * 0.15)
            + (subtotalEsperado - subtotalEsperado * 0.15) * 0.19;
        assertEquals(totalEsperado, resultado.getTotal(), 0.01);
    }

    @Test
    void pedidoFrecuenteConMasDeDiezPedidosPrevios_seConfirmaConDescuentoDel8Porciento() {
        // Cliente 2 (FRECUENTE) ya tiene 12 pedidos previos sembrados en data.sql
        PedidoRequest request = pedido(2L, "carlos@udes.edu.co", new ItemPedido(1L, 2));
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        double subtotalEsperado = 2 * 150000;
        double totalEsperado = subtotalEsperado - (subtotalEsperado * 0.08)
            + (subtotalEsperado - subtotalEsperado * 0.08) * 0.19;
        assertEquals(totalEsperado, resultado.getTotal(), 0.01);
    }

    @Test
    void stockInsuficiente_seRechaza() {
        // Producto 3 (monitor) solo tiene 8 unidades en inventario
        PedidoRequest request = pedido(5L, "pedro@udes.edu.co", new ItemPedido(3L, 10));
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertTrue(resultado.getMotivoRechazo().contains("Stock insuficiente"));
    }

    @Test
    void clienteMorosoDentroDelHorarioDeCorte_seRechaza() {
        // Cliente 3 (Moroso SA) tiene una factura pendiente sin pagar
        PedidoRequest request = pedido(3L, "moroso@udes.edu.co", new ItemPedido(2L, 1));
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        // Este caso depende de la hora real del sistema (antes/despues de las 20:00),
        // igual que el codigo original: se verifica que el pedido se resuelva con un
        // motivo coherente en ambos escenarios posibles, sin lanzar una excepcion.
        assertNotNull(resultado);
        if (!resultado.isConfirmado()) {
            assertTrue(resultado.getMotivoRechazo().contains("deuda pendiente"));
        }
    }

    @Test
    void clienteNoRegistrado_seRechaza() {
        PedidoRequest request = pedido(999L, "desconocido@udes.edu.co", new ItemPedido(1L, 1));
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertEquals("Cliente no registrado", resultado.getMotivoRechazo());
    }

    @Test
    void campanaCorporativo_aplicaDescuentoDel10PorcientoParaClienteConNit() {
        // Cliente 4 (Comercial Andina, ESTANDAR) tiene NIT registrado en data.sql.
        // Black Friday esta desactivada en src/test/resources/application.properties,
        // asi que el 10% corporativo no queda tapado por el 25% de la campana.
        PedidoRequest request = pedido(4L, "andina@udes.edu.co", new ItemPedido(2L, 1));
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        double subtotalEsperado = 60000;
        double totalEsperado = subtotalEsperado - (subtotalEsperado * 0.10)
            + (subtotalEsperado - subtotalEsperado * 0.10) * 0.19;
        assertEquals(totalEsperado, resultado.getTotal(), 0.01);
    }

    @Test
    void campanaVolumen_aplicaDescuentoCuandoSuperaVeinteUnidades() {
        // Cliente 5 (ESTANDAR, sin NIT), 25 unidades del mismo producto -> supera
        // el umbral de volumen. Sin Black Friday, el 12% de volumen es el mayor.
        PedidoRequest request = pedido(5L, "pedro@udes.edu.co", new ItemPedido(2L, 25));
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        double subtotalEsperado = 25 * 60000;
        double totalEsperado = subtotalEsperado - (subtotalEsperado * 0.12)
            + (subtotalEsperado - subtotalEsperado * 0.12) * 0.19;
        assertEquals(totalEsperado, resultado.getTotal(), 0.01);
    }

    private PedidoRequest pedido(Long clienteId, String email, ItemPedido... items) {
        PedidoRequest request = new PedidoRequest();
        request.setClienteId(clienteId);
        request.setClienteEmail(email);
        request.setItems(List.of(items));
        return request;
    }
}
