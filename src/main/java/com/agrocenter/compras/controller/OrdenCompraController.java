package com.agrocenter.compras.controller;

import com.agrocenter.compras.dto.OrdenCompraRequest;
import com.agrocenter.compras.model.OrdenCompra;
import com.agrocenter.compras.service.OrdenCompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class OrdenCompraController {

    private final OrdenCompraService service;

    public OrdenCompraController(OrdenCompraService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrdenCompra> crearOrden(@Valid @RequestBody OrdenCompraRequest request) {
        OrdenCompra nuevaOrden = service.crearOrden(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaOrden);
    }

    @GetMapping
    public ResponseEntity<List<OrdenCompra>> listarOrdenes() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompra> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }
}