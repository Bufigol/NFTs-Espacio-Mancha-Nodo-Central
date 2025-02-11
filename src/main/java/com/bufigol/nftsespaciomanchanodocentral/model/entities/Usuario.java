package com.bufigol.nftsespaciomanchanodocentral.model.entities;

import java.util.UUID;
import java.time.LocalDateTime;

public abstract class Usuario {

    private UUID id;
    private String nombre;
    private String email;
    private String telefono;
    private Wallet wallet;
    private LocalDateTime fechaRegistro;
    private boolean activo;
    private String password; // Almacenar hash de password

    public Usuario(String nombre, String email, String telefono) {
        this.id = UUID.randomUUID();
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.fechaRegistro = LocalDateTime.now();
        this.activo = true;
        this.wallet = new Wallet(); // Se crea una wallet por defecto
    }

    // Getters y setters
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
