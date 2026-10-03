package com.tienda.pedidos.validacion;

// Eslabon de la cadena: cada validador decide si el pedido continua o se rechaza.
// Se usa Chain of Responsibility porque las validaciones tienen una dependencia
// real de orden y de corte anticipado (ver Decision con justificacion en el README).
public abstract class ValidadorPedido {
    private ValidadorPedido siguiente;

    public ValidadorPedido encadenar(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void validar(ContextoPedido contexto) {
        ejecutar(contexto);
        if (!contexto.isRechazado() && siguiente != null) {
            siguiente.validar(contexto);
        }
    }

    protected abstract void ejecutar(ContextoPedido contexto);
}
