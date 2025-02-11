package com.bufigol.nftsespaciomanchanodocentral.model.entities;

import java.util.UUID;
import java.time.LocalDateTime;

public class Sale {
    // Identificación
    private final UUID id;
    private final NFT nft;

    // Participantes
    private final Usuario vendedor;
    private Usuario comprador;
    private final String direccionWalletVendedor;
    private String direccionWalletComprador;

    // Datos de la venta
    private double precioInicial;
    private double precioFinal;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaFinalizacion;
    private String hashTransaccion;
    private SaleStatus estado;

    // Datos adicionales
    private String comentarios;
    private boolean requiereIntermedario;
    private double comisionPlataforma;
    private String motivoCancelacion;

    public enum SaleStatus {
        PENDING("Pendiente"),
        COMPLETED("Completada"),
        CANCELLED("Cancelada"),
        FAILED("Fallida");

        private final String descripcion;

        SaleStatus(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    // Constructor para nueva venta
    public Sale(NFT nft, Usuario vendedor, double precioInicial) {
        this.id = UUID.randomUUID();
        this.nft = nft;
        this.vendedor = vendedor;
        this.direccionWalletVendedor = vendedor.getWallet().getAddress();
        this.precioInicial = precioInicial;
        this.fechaCreacion = LocalDateTime.now();
        this.estado = SaleStatus.PENDING;
        this.comisionPlataforma = calcularComision(precioInicial);
    }

    // Métodos de negocio
    public void completarVenta(Usuario comprador, double precioFinal, String hashTransaccion) {
        if (this.estado != SaleStatus.PENDING) {
            throw new IllegalStateException("La venta no está en estado pendiente");
        }

        this.comprador = comprador;
        this.direccionWalletComprador = comprador.getWallet().getAddress();
        this.precioFinal = precioFinal;
        this.hashTransaccion = hashTransaccion;
        this.fechaFinalizacion = LocalDateTime.now();
        this.estado = SaleStatus.COMPLETED;

        // Transferir el NFT al comprador
        this.nft.transferirPropiedad(comprador, precioFinal, hashTransaccion);
    }

    public void cancelarVenta(String motivo) {
        if (this.estado != SaleStatus.PENDING) {
            throw new IllegalStateException("La venta no está en estado pendiente");
        }

        this.estado = SaleStatus.CANCELLED;
        this.motivoCancelacion = motivo;
        this.fechaFinalizacion = LocalDateTime.now();

        // Liberar el NFT para otras ventas
        this.nft.retirarDeVenta();
    }

    public void marcarComoFallida(String motivo) {
        this.estado = SaleStatus.FAILED;
        this.motivoCancelacion = motivo;
        this.fechaFinalizacion = LocalDateTime.now();
        this.nft.retirarDeVenta();
    }

    private double calcularComision(double precio) {
        // Lógica para calcular la comisión de la plataforma
        // Por ejemplo, 2.5% del precio
        return precio * 0.025;
    }

    // Getters
    public UUID getId() { return id; }
    public NFT getNft() { return nft; }
    public Usuario getVendedor() { return vendedor; }
    public Usuario getComprador() { return comprador; }
    public String getDireccionWalletVendedor() { return direccionWalletVendedor; }
    public String getDireccionWalletComprador() { return direccionWalletComprador; }
    public double getPrecioInicial() { return precioInicial; }
    public double getPrecioFinal() { return precioFinal; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaFinalizacion() { return fechaFinalizacion; }
    public String getHashTransaccion() { return hashTransaccion; }
    public SaleStatus getEstado() { return estado; }
    public double getComisionPlataforma() { return comisionPlataforma; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public boolean isRequiereIntermedario() { return requiereIntermedario; }

    // Setters limitados (solo para propiedades que pueden modificarse)
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
    public void setRequiereIntermedario(boolean requiereIntermedario) {
        this.requiereIntermedario = requiereIntermedario;
    }

    // Método para validar si una venta puede ser completada
    public boolean puedeSerCompletada() {
        return estado == SaleStatus.PENDING
                && nft.isDisponibleParaVenta()
                && nft.getPropietarioActual().equals(vendedor);
    }

    // Método para obtener el tiempo transcurrido desde la creación
    public long getDuracionEnHoras() {
        if (fechaFinalizacion == null) {
            return LocalDateTime.now().until(fechaCreacion).toHours();
        }
        return fechaCreacion.until(fechaFinalizacion).toHours();
    }
}