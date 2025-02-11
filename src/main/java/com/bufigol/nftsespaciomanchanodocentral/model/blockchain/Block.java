package com.bufigol.nftsespaciomanchanodocentral.model.blockchain;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class Block {
    // Datos del bloque
    private final int index;
    private final LocalDateTime timestamp;
    private final String previousHash;
    private String hash;
    private final List<Transaction> transactions;
    private int nonce;

    // Metadatos del bloque
    private final String minerAddress;
    private final double minerReward;
    private final int difficulty;
    private long blockSize;
    private String merkleRoot;

    // Estado del bloque
    private boolean isValid;
    private int confirmations;
    private LocalDateTime minedTime;

    // Constructor para génesis block
    public Block(int difficulty) {
        this.index = 0;
        this.timestamp = LocalDateTime.now();
        this.previousHash = "0";
        this.transactions = new ArrayList<>();
        this.minerAddress = "GENESIS";
        this.minerReward = 0;
        this.difficulty = difficulty;
        this.nonce = 0;
        this.isValid = true;
        this.confirmations = 0;
        this.merkleRoot = calculateMerkleRoot();
        this.hash = calculateHash();
    }

    // Constructor para bloques normales
    public Block(int index, String previousHash, List<Transaction> transactions,
                 String minerAddress, double minerReward, int difficulty) {
        this.index = index;
        this.timestamp = LocalDateTime.now();
        this.previousHash = previousHash;
        this.transactions = new ArrayList<>(transactions);
        this.minerAddress = minerAddress;
        this.minerReward = minerReward;
        this.difficulty = difficulty;
        this.nonce = 0;
        this.isValid = false;
        this.confirmations = 0;
        this.merkleRoot = calculateMerkleRoot();
        this.hash = calculateHash();
        this.blockSize = calculateBlockSize();
    }

    // Métodos de minería y validación
    public void mineBlock() {
        String target = new String(new char[difficulty]).replace('\0', '0');
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
        minedTime = LocalDateTime.now();
        isValid = true;
    }

    public String calculateHash() {
        String dataToHash = index +
                timestamp.toString() +
                previousHash +
                merkleRoot +
                nonce +
                minerAddress;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));

            // Convertir byte array a string hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando hash del bloque", e);
        }
    }

    private String calculateMerkleRoot() {
        List<String> transactionHashes = new ArrayList<>();
        for (Transaction transaction : transactions) {
            transactionHashes.add(transaction.getHash());
        }
        return buildMerkleRoot(transactionHashes);
    }

    private String buildMerkleRoot(List<String> hashes) {
        if (hashes.isEmpty()) {
            return "0000000000000000000000000000000000000000000000000000000000000000";
        }
        if (hashes.size() == 1) {
            return hashes.get(0);
        }

        List<String> newHashes = new ArrayList<>();
        for (int i = 0; i < hashes.size() - 1; i += 2) {
            String combinedHash = hashPair(hashes.get(i), hashes.get(i + 1));
            newHashes.add(combinedHash);
        }
        if (hashes.size() % 2 == 1) {
            String combinedHash = hashPair(hashes.get(hashes.size() - 1),
                    hashes.get(hashes.size() - 1));
            newHashes.add(combinedHash);
        }
        return buildMerkleRoot(newHashes);
    }

    private String hashPair(String hash1, String hash2) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = hash1 + hash2;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando hash par Merkle", e);
        }
    }

    private long calculateBlockSize() {
        // Tamaño aproximado en bytes
        long size = 0;
        size += 4; // index
        size += 8; // timestamp
        size += previousHash.length();
        size += hash.length();
        size += 4; // nonce
        size += minerAddress.length();
        size += 8; // minerReward
        size += 4; // difficulty

        for (Transaction tx : transactions) {
            size += tx.getApproximateSize();
        }

        return size;
    }

    // Métodos de validación
    public boolean isValid() {
        if (!isValid) return false;
        if (!hash.equals(calculateHash())) return false;
        if (!merkleRoot.equals(calculateMerkleRoot())) return false;

        String target = new String(new char[difficulty]).replace('\0', '0');
        return hash.substring(0, difficulty).equals(target);
    }

    public void incrementConfirmations() {
        this.confirmations++;
    }

    // Getters
    public int getIndex() { return index; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPreviousHash() { return previousHash; }
    public String getHash() { return hash; }
    public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }
    public int getNonce() { return nonce; }
    public String getMinerAddress() { return minerAddress; }
    public double getMinerReward() { return minerReward; }
    public int getDifficulty() { return difficulty; }
    public long getBlockSize() { return blockSize; }
    public String getMerkleRoot() { return merkleRoot; }
    public int getConfirmations() { return confirmations; }
    public LocalDateTime getMinedTime() { return minedTime; }

    // Para debug y logging
    @Override
    public String toString() {
        return String.format(
                "Block[index=%d, hash=%s, previousHash=%s, transactions=%d, nonce=%d]",
                index, hash.substring(0, 10), previousHash.substring(0, 10),
                transactions.size(), nonce
        );
    }
}