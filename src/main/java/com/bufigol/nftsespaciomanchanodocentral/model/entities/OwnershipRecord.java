package com.bufigol.nftsespaciomanchanodocentral.model.entities;

import java.util.UUID;
import java.time.LocalDateTime;

public class OwnershipRecord {
    // Identificación
    private final UUID id;
    private final NFT nft;

    // Datos de propiedad
    private final String direccionWalletPropietario;
    private final LocalDateTime fechaAdquisicion;
    private final double precioAdquisicion;
    private final String hashTransaccion;
    private final OwnershipType tipoAdquisicion;

    // Metadatos adicionales
    private final String plataformaAdquisicion;
    private final String verificadorTransaccion;
    private final int numeroTransferenciaNFT; // Número secuencial de transferencia para este NFT

    public enum OwnershipType {
        MINTED("Acuñado"),
        PURCHASED("Comprado"),
        TRANSFERRED("Transferido"),
        AWARDED("Otorgado"); // Para premios o regalos

        private final String descripcion;

        OwnershipType(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    // Constructor principal
    public OwnershipRecord(NFT nft,
                           String direccionWalletPropietario,
                           LocalDateTime fechaAdquisicion,
                           double precioAdquisicion,
                           String hashTransaccion,
                           OwnershipType tipoAdquisicion) {
        this(nft,
                direccionWalletPropietario,
                fechaAdquisicion,
                precioAdquisicion,
                hashTransaccion,
                tipoAdquisicion,
                "Espacio Mancha", // Plataforma por defecto
                null); // Sin verificador específico
    }

    // Constructor completo
    public OwnershipRecord(NFT nft,
                           String direccionWalletPropietario,
                           LocalDateTime fechaAdquisicion,
                           double precioAdquisicion,
                           String hashTransaccion,
                           OwnershipType tipoAdquisicion,
                           String plataformaAdquisicion,
                           String verificadorTransaccion) {
        this.id = UUID.randomUUID();
        this.nft = nft;
        this.direccionWalletPropietario = direccionWalletPropietario;
        this.fechaAdquisicion = fechaAdquisicion;
        this.precioAdquisicion = precioAdquisicion;
        this.hashTransaccion = hashTransaccion;
        this.tipoAdquisicion = tipoAdquisicion;
        this.plataformaAdquisicion = plataformaAdquisicion;
        this.verificadorTransaccion = verificadorTransaccion;

        // Calcular el número de transferencia basado en el historial del NFT
        this.numeroTransferenciaNFT = calcularNumeroTransferencia();
    }

    private int calcularNumeroTransferencia() {
        // Obtener el número de transferencias previas + 1
        return nft.getHistorialPropietarios().size() + 1;
    }

    // Métodos de validación
    public boolean esTransaccionValida() {
        return hashTransaccion != null &&
                verificarHash(hashTransaccion) &&
                verificarDireccionWallet(direccionWalletPropietario);
    }

    private boolean verificarHash(String hash) {
        // Implementar verificación de hash
        return hash != null && !hash.isEmpty();
    }

    private boolean verificarDireccionWallet(String direccion) {
        // Implementar verificación de dirección de wallet
        return direccion != null && !direccion.isEmpty();
    }

    // Getters (sin setters para mantener inmutabilidad)
    public UUID getId() { return id; }

    public NFT getNft() { return nft; }

    public String getDireccionWalletPropietario() {
        return direccionWalletPropietario;
    }

    public LocalDateTime getFechaAdquisicion() {
        return fechaAdquisicion;
    }

    public double getPrecioAdquisicion() {
        return precioAdquisicion;
    }

    public String getHashTransaccion() {
        return hashTransaccion;
    }

    public OwnershipType getTipoAdquisicion() {
        return tipoAdquisicion;
    }

    public String getPlataformaAdquisicion() {
        return plataformaAdquisicion;
    }

    public String getVerificadorTransaccion() {
        return verificadorTransaccion;
    }

    public int getNumeroTransferenciaNFT() {
        return numeroTransferenciaNFT;
    }

    // Método toString para representación en texto
    @Override
    public String toString() {
        return String.format(
                "OwnershipRecord[id=%s, nft=%s, owner=%s, date=%s, type=%s]",
                id,
                nft.getId(),
                direccionWalletPropietario,
                fechaAdquisicion,
                tipoAdquisicion
        );
    }

    // Métodos equals y hashCode basados en el ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OwnershipRecord)) return false;
        OwnershipRecord that = (OwnershipRecord) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}