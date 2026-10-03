package com.tienda.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Implementacion minima para no depender de un servidor SMTP real: imprime
// el correo en consola/log. No forma parte del diagnostico de esta unidad.
@Service
public class ConsoleEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        log.info("[EMAIL] Para: {} | Asunto: {}\n{}", destinatario, asunto, cuerpo);
    }
}
