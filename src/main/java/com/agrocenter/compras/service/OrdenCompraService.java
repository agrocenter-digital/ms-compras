package com.agrocenter.compras.service;

import com.agrocenter.compras.dto.*;
import com.agrocenter.compras.model.*;
import com.agrocenter.compras.repository.OrdenCompraRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrdenCompraService {

    private final OrdenCompraRepository repository;
    private final RestTemplate restTemplate;
    private final String inventarioUrl;

    public OrdenCompraService(OrdenCompraRepository repository, 
                              RestTemplate restTemplate, 
                              @Value("${services.inventario.url}") String inventarioUrl) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.inventarioUrl = inventarioUrl;
    }

    @Transactional
    public OrdenCompra crearOrden(OrdenCompraRequest request) {
        BigDecimal totalCalculado = BigDecimal.ZERO;

        List<DetalleOrdenCompra> detalles = request.detalles().stream().map(d -> {
            BigDecimal subtotal = d.precioUnitario().multiply(BigDecimal.valueOf(d.cantidad()));
            return DetalleOrdenCompra.builder()
                    .productoId(d.productoId())
                    .cantidad(d.cantidad())
                    .precioUnitario(d.precioUnitario())
                    .subtotal(subtotal)
                    .build();
        }).toList();

        for (DetalleOrdenCompra detalle : detalles) {
            totalCalculado = totalCalculado.add(detalle.getSubtotal());
        }

        OrdenCompra orden = OrdenCompra.builder()
                .proveedorId(request.proveedorId())
                .estado(EstadoOrden.COMPLETADA)
                .detalles(detalles)
                .total(totalCalculado)
                .build();

        OrdenCompra ordenGuardada = repository.save(orden);

        // Notificar reabastecimiento a ms-inventario
        notificarIngresoInventario(ordenGuardada);

        return ordenGuardada;
    }

    private void notificarIngresoInventario(OrdenCompra orden) {
        for (DetalleOrdenCompra detalle : orden.getDetalles()) {
            MovimientoStockRequest movimiento = new MovimientoStockRequest(
                    detalle.getProductoId(),
                    detalle.getCantidad(),
                    "ENTRADA",
                    "COMPRA_ORDEN_" + orden.getId()
            );
            try {
                restTemplate.postForEntity(inventarioUrl + "/api/inventario/movimientos", movimiento, Void.class);
            } catch (Exception e) {
                // Manejo de resiliencia o reintentos
                System.err.println("Error notificando a ms-inventario: " + e.getMessage());
            }
        }
    }

    public List<OrdenCompra> listarTodas() {
        return repository.findAll();
    }

    public OrdenCompra obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden de compra no encontrada con ID: " + id));
    }
}