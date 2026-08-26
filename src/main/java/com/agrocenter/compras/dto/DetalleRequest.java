package com.agrocenter.compras.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DetalleRequest(
    @NotNull Long productoId,
    @NotNull @Positive Integer cantidad,
    @NotNull @Positive BigDecimal precioUnitario
) {}