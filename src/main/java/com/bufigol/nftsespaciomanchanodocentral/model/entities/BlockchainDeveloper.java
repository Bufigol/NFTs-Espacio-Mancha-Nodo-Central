package com.bufigol.nftsespaciomanchanodocentral.model.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockchainDeveloper extends Usuario {
    private String nivelAcceso;
    private List<String> permisos;
    private LocalDateTime ultimoAcceso;

    public BlockchainDeveloper(String nombre, String email, String telefono, String nivelAcceso) {
        super(nombre, email, telefono);
        this.nivelAcceso = nivelAcceso;
        this.permisos = new ArrayList<>();
        this.ultimoAcceso = LocalDateTime.now();
    }

    public void modificarBlockchain(String operacion) {
        // Lógica para modificar parámetros de la blockchain
        this.ultimoAcceso = LocalDateTime.now();
    }

    public void agregarPermiso(String permiso) {
        if (!permisos.contains(permiso)) {
            permisos.add(permiso);
        }
    }

    // Getters y setters específicos de BlockchainDeveloper
    public String getNivelAcceso() { return nivelAcceso; }
    public void setNivelAcceso(String nivelAcceso) { this.nivelAcceso = nivelAcceso; }
    public List<String> getPermisos() { return Collections.unmodifiableList(permisos); }
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
}
