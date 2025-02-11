package com.bufigol.nftsespaciomanchanodocentral.model.entities;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDateTime;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

public class Wallet {
    // Identificación
    private final UUID id;
    private final String address;

    // Claves criptográficas
    private final PublicKey publicKey;
    private final PrivateKey privateKey; // En producción, considerar almacenamiento seguro

    // Datos financieros
    private double balance;
    private double balanceBloqueado; // Para transacciones pendientes
    private List<Transaction> historialTransacciones;

    // Estado y metadata
    private boolean activa;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime ultimaActividad;
    private int nivelVerificacion; // 0: básica, 1: verificada, 2: premium

    public enum WalletStatus {
        ACTIVE("Activa"),
        INACTIVE("Inactiva"),
        BLOCKED("Bloqueada"),
        PENDING_VERIFICATION("Pendiente de Verificación");

        private final String descripcion;

        WalletStatus(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    private WalletStatus estado;

    // Constructor
    public Wallet() {
        this.id = UUID.randomUUID();
        this.fechaCreacion = LocalDateTime.now();
        this.ultimaActividad = this.fechaCreacion;
        this.historialTransacciones = new ArrayList<>();
        this.balance = 0.0;
        this.balanceBloqueado = 0.0;
        this.activa = true;
        this.nivelVerificacion = 0;
        this.estado = WalletStatus.PENDING_VERIFICATION;

        // Generar par de claves
        KeyPair keyPair = generarParClaves();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();

        // Generar dirección de wallet basada en la clave pública
        this.address = generarDireccion(this.publicKey);
    }

    // Métodos de generación de claves y dirección
    private KeyPair generarParClaves() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstanceStrong();
            keyGen.initialize(2048, random);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Error generando par de claves", e);
        }
    }

    private String generarDireccion(PublicKey publicKey) {
        // Implementar generación de dirección basada en la clave pública
        // Ejemplo simplificado - en producción usar algoritmos más robustos
        return "EM" + UUID.nameUUIDFromBytes(publicKey.getEncoded()).toString().substring(0, 16);
    }

    // Métodos de transacción
    public void depositar(double cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        if (!activa) {
            throw new IllegalStateException("La wallet no está activa");
        }

        this.balance += cantidad;
        registrarTransaccion("DEPOSITO", cantidad);
        actualizarUltimaActividad();
    }

    public void retirar(double cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        if (!activa) {
            throw new IllegalStateException("La wallet no está activa");
        }
        if (balance - balanceBloqueado < cantidad) {
            throw new IllegalStateException("Balance insuficiente");
        }

        this.balance -= cantidad;
        registrarTransaccion("RETIRO", -cantidad);
        actualizarUltimaActividad();
    }

    public void bloquearFondos(double cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        if (balance - balanceBloqueado < cantidad) {
            throw new IllegalStateException("Balance disponible insuficiente");
        }

        this.balanceBloqueado += cantidad;
        actualizarUltimaActividad();
    }

    public void desbloquearFondos(double cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        if (balanceBloqueado < cantidad) {
            throw new IllegalStateException("Cantidad bloqueada insuficiente");
        }

        this.balanceBloqueado -= cantidad;
        actualizarUltimaActividad();
    }

    private void registrarTransaccion(String tipo, double cantidad) {
        Transaction transaccion = new Transaction(
                this.address,
                tipo,
                cantidad,
                LocalDateTime.now()
        );
        historialTransacciones.add(transaccion);
    }

    private void actualizarUltimaActividad() {
        this.ultimaActividad = LocalDateTime.now();
    }

    // Métodos de firma y verificación
    public byte[] firmarTransaccion(String datos) {
        // Implementar firma digital
        return new byte[0]; // Placeholder
    }

    public boolean verificarFirma(String datos, byte[] firma) {
        // Implementar verificación de firma
        return true; // Placeholder
    }

    // Getters
    public UUID getId() { return id; }
    public String getAddress() { return address; }
    public double getBalance() { return balance; }
    public double getBalanceDisponible() { return balance - balanceBloqueado; }
    public double getBalanceBloqueado() { return balanceBloqueado; }
    public boolean isActiva() { return activa; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getUltimaActividad() { return ultimaActividad; }
    public int getNivelVerificacion() { return nivelVerificacion; }
    public WalletStatus getEstado() { return estado; }
    public PublicKey getPublicKey() { return publicKey; }

    public List<Transaction> getHistorialTransacciones() {
        return Collections.unmodifiableList(historialTransacciones);
    }

    // Setters controlados
    public void setNivelVerificacion(int nivelVerificacion) {
        if (nivelVerificacion >= 0 && nivelVerificacion <= 2) {
            this.nivelVerificacion = nivelVerificacion;
            if (nivelVerificacion > 0) {
                this.estado = WalletStatus.ACTIVE;
            }
        } else {
            throw new IllegalArgumentException("Nivel de verificación inválido");
        }
    }

    public void setEstado(WalletStatus estado) {
        this.estado = estado;
        this.activa = (estado == WalletStatus.ACTIVE);
    }

    // Clase interna para transacciones
    private static class Transaction {
        private final UUID id;
        private final String walletAddress;
        private final String tipo;
        private final double cantidad;
        private final LocalDateTime fecha;

        public Transaction(String walletAddress, String tipo, double cantidad, LocalDateTime fecha) {
            this.id = UUID.randomUUID();
            this.walletAddress = walletAddress;
            this.tipo = tipo;
            this.cantidad = cantidad;
            this.fecha = fecha;
        }
    }
}