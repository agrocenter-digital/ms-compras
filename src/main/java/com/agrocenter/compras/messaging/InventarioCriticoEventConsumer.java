package com.agrocenter.compras.messaging;

import com.agrocenter.compras.messaging.event.InventarioCriticoEvent;
import com.agrocenter.compras.service.ReabastecimientoAutomaticoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioCriticoEventConsumer {

    private final ReabastecimientoAutomaticoService reabastecimientoService;

    @KafkaListener(topics = KafkaTopics.INVENTARIO_CRITICO)
    public void onInventarioCritico(InventarioCriticoEvent event) {
        if (event == null || event.productoId() == null) {
            log.warn("Alerta de inventario critico invalida ignorada: {}", event);
            return;
        }

        log.info(
                "Alerta de inventario critico recibida productoId={} stockActual={} stockMinimo={}",
                event.productoId(),
                event.stockActual(),
                event.stockMinimo()
        );

        try {
            reabastecimientoService.procesarAlerta(event);
        } catch (RuntimeException exception) {
            log.error(
                    "No fue posible generar la orden de reabastecimiento productoId={}",
                    event.productoId(),
                    exception
            );
        }
    }
}
