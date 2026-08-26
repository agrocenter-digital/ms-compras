package com.agrocenter.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrdenCompraRequest(
    @NotNull Long proveedorId,
    @NotEmpty @Valid List<DetalleRequest> detalles
) {}