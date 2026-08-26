package com.agrocenter.compras.client;

import com.agrocenter.compras.dto.MovimientoStockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Define el nombre del servicio objetivo y su URL configurada
@FeignClient(name = "ms-inventario", url = "${services.inventario.url}")
public interface InventarioFeignClient {

    // Método declarativo que hace el POST automático a ms-inventario
    @PostMapping("/api/inventario/movimientos")
    void registrarMovimiento(@RequestBody MovimientoStockRequest movimiento);
}