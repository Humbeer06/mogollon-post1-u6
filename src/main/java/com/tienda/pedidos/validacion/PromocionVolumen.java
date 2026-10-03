package com.tienda.pedidos.validacion;

import org.springframework.stereotype.Component;

// Nuevo eslabon: volumen -- necesita el total de unidades del pedido, que recalcula
// el mismo desde el request porque el contexto todavia no lo expone
@Component
public class PromocionVolumen extends ValidadorPedido {
    @Override
    protected void ejecutar(ContextoPedido contexto) {
        int totalUnidades = contexto.getRequest().getItems().stream()
            .mapToInt(item -> item.getCantidad()).sum();
        if (totalUnidades > 20) {
            contexto.aplicarDescuentoCampana(0.12);
        }
    }
}
