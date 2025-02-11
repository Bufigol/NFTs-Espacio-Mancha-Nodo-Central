package com.bufigol.nftsespaciomanchanodocentral.model.entities;

import java.util.Collections;

public class Artista extends Usuario{
    private String pseudonimo;
    private List<NFT> obrasCreadas;
    private byte[] imagenPerfil; // IPFS hash
    private String biografia;
    private String redesSociales;

    public Artista(String nombre, String email, String telefono, String pseudonimo) {
        super(nombre, email, telefono);
        this.pseudonimo = pseudonimo;
        this.obrasCreadas = new ArrayList<>();
    }

    public void crearNFT(String nombre, String descripcion, byte[] contenido) {
        NFT nft = new NFT(nombre, this, contenido);
        obrasCreadas.add(nft);
    }

    // Getters y setters específicos de Artista
    public String getPseudonimo() { return pseudonimo; }
    public void setPseudonimo(String pseudonimo) { this.pseudonimo = pseudonimo; }
    public List<NFT> getObrasCreadas() { return Collections.unmodifiableList(obrasCreadas); }
    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }
}
