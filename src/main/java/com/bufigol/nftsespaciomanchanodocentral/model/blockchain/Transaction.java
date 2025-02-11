package com.bufigol.nftsespaciomanchanodocentral.model.blockchain;

import com.bufigol.nftsespaciomanchanodocentral.model.entities.NFT;
import com.bufigol.nftsespaciomanchanodocentral.model.utils.TransactionStatus;
import com.bufigol.nftsespaciomanchanodocentral.model.utils.TransactionType;

import java.util.UUID;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Transaction {
    // Identificación
    private final UUID id;
    private final String hash;
    private final LocalDateTime timestamp;

    // Direcciones
    private final String fromAddress;
    private final String toAddress;

    // Datos de la transacción
    private final NFT nft;
    private final double amount;
    private final byte[] signature;
    private final TransactionType type;

    // Metadatos
    private final long nonce;
    private final double fee;
    private final int priority;
    private final String data; // Datos adicionales en formato JSON

    // Estado
    private TransactionStatus status;
    private int confirmations;
    private String blockHash;



    // Constructor
    public Transaction(String fromAddress, String toAddress, NFT nft,
                       double amount, TransactionType type, byte[] signature,
                       double fee, String data) {
        this.id = UUID.randomUUID();
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.nft = nft;
        this.amount = amount;
        this.type = type;
        this.signature = Arrays.copyOf(signature, signature.length);
        this.fee = fee;
        this.data = data;
        this.timestamp = LocalDateTime.now();
        this.nonce = generateNonce();
        this.priority = calculatePriority();
        this.status = TransactionStatus.PENDING;
        this.confirmations = 0;
        this.hash = calculateHash();
    }

    // Métodos de cálculo y validación
    private String calculateHash() {
        try {
            String dataToHash = fromAddress +
                    toAddress +
                    (nft != null ? nft.getId().toString() : "") +
                    amount +
                    timestamp.toString() +
                    nonce +
                    type.toString();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando hash de transacción", e);
        }
    }

    private long generateNonce() {
        return System.nanoTime();
    }

    private int calculatePriority() {
        // Prioridad basada en fee y tipo de transacción
        int basePriority = (int)(fee * 100);
        switch(type) {
            case NFT_MINT:
                return basePriority + 100;
            case NFT_TRANSFER:
                return basePriority + 75;
            case PAYMENT:
                return basePriority + 50;
            case SMART_CONTRACT:
                return basePriority + 25;
            default:
                return basePriority;
        }
    }

    public boolean verifySignature(byte[] publicKey) {
        try {
            // Implementar verificación de firma con clave pública
            return true; // Placeholder
        } catch (Exception e) {
            return false;
        }
    }

    public void confirm(String blockHash) {
        this.blockHash = blockHash;
        this.status = TransactionStatus.CONFIRMED;
    }

    public void incrementConfirmations() {
        this.confirmations++;
    }

    public void revert(String reason) {
        this.status = TransactionStatus.REVERTED;
        // Implementar lógica de reversión
    }

    // Método para calcular el tamaño aproximado de la transacción
    public long getApproximateSize() {
        long size = 0;
        size += 16; // UUID
        size += hash.length();
        size += 8; // timestamp
        size += fromAddress.length();
        size += toAddress.length();
        size += 8; // amount
        size += signature.length;
        size += 8; // nonce
        size += 8; // fee
        size += 4; // priority
        size += data != null ? data.length() : 0;
        size += blockHash != null ? blockHash.length() : 0;
        return size;
    }

    // Getters
    public UUID getId() { return id; }
    public String getHash() { return hash; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFromAddress() { return fromAddress; }
    public String getToAddress() { return toAddress; }
    public NFT getNft() { return nft; }
    public double getAmount() { return amount; }
    public byte[] getSignature() { return Arrays.copyOf(signature, signature.length); }
    public TransactionType getType() { return type; }
    public long getNonce() { return nonce; }
    public double getFee() { return fee; }
    public int getPriority() { return priority; }
    public String getData() { return data; }
    public TransactionStatus getStatus() { return status; }
    public int getConfirmations() { return confirmations; }
    public String getBlockHash() { return blockHash; }

    @Override
    public String toString() {
        return String.format(
                "Transaction[hash=%s, from=%s, to=%s, type=%s, status=%s]",
                hash.substring(0, 10),
                fromAddress.substring(0, 10),
                toAddress.substring(0, 10),
                type,
                status
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }
}