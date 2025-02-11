package com.bufigol.nftsespaciomanchanodocentral.model.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Comprador extends Usuario{
    private List<NFT> coleccion;
    private List<Sale> comprasRealizadas;
    private double balanceDisponible;

    public Comprador(String nombre, String email, String telefono) {
        super(nombre, email, telefono);
        this.coleccion = new ArrayList<>();
        this.comprasRealizadas = new ArrayList<>();
        this.balanceDisponible = 0.0;
    }

    public void comprarNFT(NFT nft, double precio) {
        if (balanceDisponible >= precio) {
            // Lógica de compra
            coleccion.add(nft);
            Sale sale = new Sale(nft, this.getWallet().getAddress(), precio);
            comprasRealizadas.add(sale);
        }
    }

    // Getters y setters específicos de Comprador
    public List<NFT> getColeccion() { return Collections.unmodifiableList(coleccion); }
    public List<Sale> getComprasRealizadas() { return Collections.unmodifiableList(comprasRealizadas); }
    public double getBalanceDisponible() { return balanceDisponible; }
}
