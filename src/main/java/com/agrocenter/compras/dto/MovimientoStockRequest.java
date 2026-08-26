package com.agrocenter.compras.dto;

public record MovimientoStockRequest(
    Long productoId,
    Integer cantidad,
    String tipoMovimiento,
    String referencia
) {}