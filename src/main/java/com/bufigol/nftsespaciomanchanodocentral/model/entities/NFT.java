package com.bufigol.nftsespaciomanchanodocentral.model.entities;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDateTime;

public class NFT {
    // Identificadores y metadata básica
    private final UUID id;
    private String nombre;
    private String descripcion;
    private List<String> tags;

    // Relaciones con otros objetos
    private final Artista creador;
    private Usuario propietarioActual;
    private List<OwnershipRecord> historialPropietarios;

    // Datos de almacenamiento y contenido
    private String ipfsHash;
    private String contenidoOriginalHash;
    private int tamanoBytes;
    private String formatoArchivo;

    // Datos de mercado
    private boolean disponibleParaVenta;
    private double precioActual;
    private int numeroEdicion;
    private int edicionesMaximas;

    // Datos temporales
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimaTransferencia;

    public NFT(String nombre, Artista creador, byte[] contenidoOriginal) {
        this.id = UUID.randomUUID();
        this.nombre = nombre;
        this.creador = creador;
        this.propietarioActual = creador; // Inicialmente el propietario es el creador
        this.historialPropietarios = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.fechaCreacion = LocalDateTime.now();
        this.fechaUltimaTransferencia = this.fechaCreacion;
        this.disponibleParaVenta = false;
        this.numeroEdicion = 1;
        this.edicionesMaximas = 1; // Por defecto es única

        // Registrar la propiedad inicial
        OwnershipRecord registroInicial = new OwnershipRecord(
                this,
                creador.getWallet().getAddress(),
                fechaCreacion,
                0.0, // Precio inicial 0 por ser acuñación
                null, // No hay hash de transacción en la acuñación
                OwnershipType.MINTED
        );
        this.historialPropietarios.add(registroInicial);

        // Procesar el contenido original
        procesarContenido(contenidoOriginal);
    }

    private void procesarContenido(byte[] contenido) {
        // Calcular el hash del contenido original
        this.contenidoOriginalHash = calcularHash(contenido);
        this.tamanoBytes = contenido.length;
        this.formatoArchivo = determinarFormato(contenido);

        // Almacenar en IPFS y obtener el hash
        this.ipfsHash = almacenarEnIPFS(contenido);
    }

    private String calcularHash(byte[] contenido) {
        // Implementar cálculo de hash SHA-256
        return "hash-placeholder";
    }

    private String determinarFormato(byte[] contenido) {
        // Implementar detección de formato basado en magic numbers
        return "formato-placeholder";
    }

    private String almacenarEnIPFS(byte[] contenido) {
        // Implementar almacenamiento en IPFS
        return "ipfs-hash-placeholder";
    }

    public void transferirPropiedad(Usuario nuevoPropietario, double precio, String transactionHash) {
        // Registrar la nueva propiedad
        OwnershipRecord nuevoRegistro = new OwnershipRecord(
                this,
                nuevoPropietario.getWallet().getAddress(),
                LocalDateTime.now(),
                precio,
                transactionHash,
                OwnershipType.PURCHASED
        );

        this.historialPropietarios.add(nuevoRegistro);
        this.propietarioActual = nuevoPropietario;
        this.fechaUltimaTransferencia = LocalDateTime.now();
        this.disponibleParaVenta = false; // Resetear estado de venta
    }

    public void ponerEnVenta(double precio) {
        if (precio > 0) {
            this.precioActual = precio;
            this.disponibleParaVenta = true;
        } else {
            throw new IllegalArgumentException("El precio debe ser mayor que 0");
        }
    }

    public void retirarDeVenta() {
        this.disponibleParaVenta = false;
    }

    // Getters
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public Artista getCreador() { return creador; }
    public Usuario getPropietarioActual() { return propietarioActual; }
    public List<OwnershipRecord> getHistorialPropietarios() {
        return Collections.unmodifiableList(historialPropietarios);
    }
    public String getIpfsHash() { return ipfsHash; }
    public boolean isDisponibleParaVenta() { return disponibleParaVenta; }
    public double getPrecioActual() { return precioActual; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public List<String> getTags() { return Collections.unmodifiableList(tags); }
    public int getNumeroEdicion() { return numeroEdicion; }
    public int getEdicionesMaximas() { return edicionesMaximas; }

    // Setters
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }
    public void removeTag(String tag) { tags.remove(tag); }
}
