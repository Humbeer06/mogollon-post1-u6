package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Black Friday se prueba aparte porque, al tomar el mayor descuento, su 25%
// taparia a los demas descuentos en GestorPedidosTest (donde esta desactivada).
@SpringBootTest
@TestPropertySource(properties = "promo.black-friday.activa=true")
class CampanaBlackFridayTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    @Test
    void campanaBlackFriday_aplicaDescuentoDel25PorcientoSobreClienteEstandar() {
        // Cliente 5 (ESTANDAR, sin NIT), Black Friday activa
        PedidoRequest request = new PedidoRequest();
        request.setClienteId(5L);
        request.setClienteEmail("pedro@udes.edu.co");
        request.setItems(List.of(new ItemPedido(1L, 1)));
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        double subtotalEsperado = 150000;
        double totalEsperado = subtotalEsperado - (subtotalEsperado * 0.25)
            + (subtotalEsperado - subtotalEsperado * 0.25) * 0.19;
        assertEquals(totalEsperado, resultado.getTotal(), 0.01);
    }
}
