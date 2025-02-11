package com.bufigol.nftsespaciomanchanodocentral.model.storage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.io.IOException;

public class IPFSNode {
    // Configuración básica
    private final String nodeId;
    private final Path baseDirectory;
    private final int maxChunkSize;

    // Almacenamiento en memoria
    private final ConcurrentHashMap<String, byte[]> contentStore;
    private final ConcurrentHashMap<String, Set<String>> pinned;
    private final ConcurrentHashMap<String, FileMetadata> metadata;

    // Control de almacenamiento
    private long totalStorageUsed;
    private final long maxStorageSize;
    private final Set<String> connectedPeers;

    // Estadísticas
    private int totalFiles;
    private int totalPins;
    private LocalDateTime lastGarbageCollection;

    private static class FileMetadata {
        String name;
        String mimeType;
        long size;
        LocalDateTime createdAt;
        LocalDateTime lastAccessed;
        List<String> chunks;
        Map<String, String> customMetadata;
        boolean isPinned;
        int references;

        FileMetadata(String name, String mimeType, long size) {
            this.name = name;
            this.mimeType = mimeType;
            this.size = size;
            this.createdAt = LocalDateTime.now();
            this.lastAccessed = this.createdAt;
            this.chunks = new ArrayList<>();
            this.customMetadata = new HashMap<>();
            this.isPinned = false;
            this.references = 0;
        }
    }

    // Constructor
    public IPFSNode(String nodeId, Path baseDirectory, int maxChunkSize, long maxStorageSize) {
        this.nodeId = nodeId;
        this.baseDirectory = baseDirectory;
        this.maxChunkSize = maxChunkSize;
        this.maxStorageSize = maxStorageSize;

        this.contentStore = new ConcurrentHashMap<>();
        this.pinned = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
        this.connectedPeers = ConcurrentHashMap.newKeySet();

        this.totalStorageUsed = 0;
        this.totalFiles = 0;
        this.totalPins = 0;

        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el almacenamiento IPFS", e);
        }
    }

    // Métodos principales
    public String addFile(byte[] content, String name, String mimeType) throws IOException {
        if (content.length + totalStorageUsed > maxStorageSize) {
            throw new IOException("Espacio de almacenamiento insuficiente");
        }

        // Dividir en chunks
        List<byte[]> chunks = splitIntoChunks(content);
        List<String> chunkHashes = new ArrayList<>();

        // Almacenar chunks
        for (byte[] chunk : chunks) {
            String chunkHash = calculateHash(chunk);
            contentStore.put(chunkHash, chunk);
            chunkHashes.add(chunkHash);
        }

        // Crear metadata
        String fileHash = calculateHash(content);
        FileMetadata fileMetadata = new FileMetadata(name, mimeType, content.length);
        fileMetadata.chunks = chunkHashes;
        metadata.put(fileHash, fileMetadata);

        // Actualizar estadísticas
        totalStorageUsed += content.length;
        totalFiles++;

        return fileHash;
    }

    public byte[] getFile(String hash) throws IOException {
        FileMetadata meta = metadata.get(hash);
        if (meta == null) {
            throw new IOException("Archivo no encontrado");
        }

        // Actualizar último acceso
        meta.lastAccessed = LocalDateTime.now();

        // Reconstruir archivo desde chunks
        List<byte[]> chunks = new ArrayList<>();
        for (String chunkHash : meta.chunks) {
            byte[] chunk = contentStore.get(chunkHash);
            if (chunk == null) {
                throw new IOException("Chunk perdido: " + chunkHash);
            }
            chunks.add(chunk);
        }

        return mergeChunks(chunks);
    }

    public void pinFile(String hash) {
        FileMetadata meta = metadata.get(hash);
        if (meta != null) {
            meta.isPinned = true;
            Set<String> pinnedChunks = pinned.computeIfAbsent(hash, k -> ConcurrentHashMap.newKeySet());
            pinnedChunks.addAll(meta.chunks);
            totalPins++;
        }
    }

    public void unpinFile(String hash) {
        FileMetadata meta = metadata.get(hash);
        if (meta != null) {
            meta.isPinned = false;
            pinned.remove(hash);
            totalPins--;
        }
    }

    // Métodos de gestión de chunks
    private List<byte[]> splitIntoChunks(byte[] content) {
        List<byte[]> chunks = new ArrayList<>();
        int offset = 0;

        while (offset < content.length) {
            int chunkSize = Math.min(maxChunkSize, content.length - offset);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(content, offset, chunk, 0, chunkSize);
            chunks.add(chunk);
            offset += chunkSize;
        }

        return chunks;
    }

    private byte[] mergeChunks(List<byte[]> chunks) {
        int totalSize = chunks.stream().mapToInt(chunk -> chunk.length).sum();
        byte[] result = new byte[totalSize];
        int offset = 0;

        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }

        return result;
    }

    private String calculateHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando hash", e);
        }
    }

    // Métodos de mantenimiento
    public void garbageCollect() {
        Set<String> referencedChunks = new HashSet<>();

        // Recolectar todos los chunks referenciados
        metadata.forEach((hash, meta) -> {
            if (meta.isPinned || meta.references > 0) {
                referencedChunks.addAll(meta.chunks);
            }
        });

        // Eliminar chunks no referenciados
        Set<String> allChunks = new HashSet<>(contentStore.keySet());
        allChunks.removeAll(referencedChunks);

        for (String chunk : allChunks) {
            contentStore.remove(chunk);
        }

        lastGarbageCollection = LocalDateTime.now();
    }

    public void addPeer(String peerId) {
        connectedPeers.add(peerId);
    }

    public void removePeer(String peerId) {
        connectedPeers.remove(peerId);
    }

    // Getters y estadísticas
    public long getTotalStorageUsed() { return totalStorageUsed; }
    public int getTotalFiles() { return totalFiles; }
    public int getTotalPins() { return totalPins; }
    public Set<String> getConnectedPeers() {
        return new HashSet<>(connectedPeers);
    }

    public Map<String, Object> getNodeStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("nodeId", nodeId);
        stats.put("totalStorageUsed", totalStorageUsed);
        stats.put("maxStorageSize", maxStorageSize);
        stats.put("totalFiles", totalFiles);
        stats.put("totalPins", totalPins);
        stats.put("connectedPeers", connectedPeers.size());
        stats.put("lastGarbageCollection", lastGarbageCollection);
        return stats;
    }
}