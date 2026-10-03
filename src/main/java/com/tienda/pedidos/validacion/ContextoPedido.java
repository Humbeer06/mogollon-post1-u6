package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.PedidoRequest;

// Contexto mutable que viaja a traves de la cadena de validacion. El campo
// descuentoCampana (usado por el Golden Hammer de la Parte 2) se elimino
// por completo al corregir -- las campañas ahora son EstrategiaDescuento,
// que no necesitan escribir en el contexto de validacion.
public class ContextoPedido {
    private final PedidoRequest request;
    private String tipoCliente;
    private double subtotal;
    private boolean rechazado = false;
    private String motivoRechazo;

    public ContextoPedido(PedidoRequest request) { this.request = request; }

    public PedidoRequest getRequest() { return request; }
    public String getTipoCliente() { return tipoCliente; }
    public void setTipoCliente(String tipoCliente) { this.tipoCliente = tipoCliente; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public boolean isRechazado() { return rechazado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void rechazar(String motivo) { this.rechazado = true; this.motivoRechazo = motivo; }
}
