package com.bufigol.nftsespaciomanchanodocentral.model.utils;

public enum TransactionStatus {
    PENDING("Pendiente"),
    CONFIRMED("Confirmada"),
    FAILED("Fallida"),
    REVERTED("Revertida");

    private final String descripcion;

    TransactionStatus(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
