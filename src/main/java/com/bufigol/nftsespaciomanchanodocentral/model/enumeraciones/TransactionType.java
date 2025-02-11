package com.bufigol.nftsespaciomanchanodocentral.model.enumeraciones;

public enum TransactionType {
    NFT_TRANSFER("Transferencia de NFT"),
    NFT_MINT("Acuñación de NFT"),
    PAYMENT("Pago"),
    SMART_CONTRACT("Contrato Inteligente");

    private final String descripcion;

    TransactionType(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
