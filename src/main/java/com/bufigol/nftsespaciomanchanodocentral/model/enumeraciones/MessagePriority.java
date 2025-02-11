package com.bufigol.nftsespaciomanchanodocentral.model.enumeraciones;

public enum MessagePriority {
    HIGH(1, "Alta"),
    MEDIUM(2, "Media"),
    LOW(3, "Baja");

    private final int value;
    private final String descripcion;

    MessagePriority(int value, String descripcion) {
        this.value = value;
        this.descripcion = descripcion;
    }

    public int getValue() { return value; }
    public String getDescripcion() { return descripcion; }
}
