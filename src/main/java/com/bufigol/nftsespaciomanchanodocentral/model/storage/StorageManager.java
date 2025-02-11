package com.bufigol.nftsespaciomanchanodocentral.model.storage;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

public class StorageManager {
    // Configuración de almacenamiento
    private final Path storageDirectory;
    private final IPFSNode ipfsNode;
    private final Map<String, String> fileIndex;

    // Cache y gestión de memoria
    private final ConcurrentHashMap<String, byte[]> cache;
    private final int maxCacheSize;
    private long currentCacheSize;

    // Control de archivos
    private final Map<String, FileMetadata> fileMetadata;
    private final Set<String> temporaryFiles;

    // Estadísticas y monitoreo
    private int totalUploads;
    private int totalDownloads;
    private long totalBytesTransferred;

    private static class FileMetadata {
        String originalName;
        String mimeType;
        long size;
        String ipfsHash;
        LocalDateTime uploadDate;
        LocalDateTime lastAccessed;
        String uploadedBy;
        Map<String, String> tags;
        boolean isTemporary;
        int accessCount;

        FileMetadata(String originalName, String mimeType, long size, String ipfsHash, String uploadedBy) {
            this.originalName = originalName;
            this.mimeType = mimeType;
            this.size = size;
            this.ipfsHash = ipfsHash;
            this.uploadDate = LocalDateTime.now();
            this.lastAccessed = this.uploadDate;
            this.uploadedBy = uploadedBy;
            this.tags = new HashMap<>();
            this.isTemporary = false;
            this.accessCount = 0;
        }
    }

    // Constructor
    public StorageManager(Path storageDirectory, IPFSNode ipfsNode, int maxCacheSize) {
        this.storageDirectory = storageDirectory;
        this.ipfsNode = ipfsNode;
        this.maxCacheSize = maxCacheSize;

        this.fileIndex = new ConcurrentHashMap<>();
        this.cache = new ConcurrentHashMap<>();
        this.fileMetadata = new ConcurrentHashMap<>();
        this.temporaryFiles = ConcurrentHashMap.newKeySet();

        this.currentCacheSize = 0;
        this.totalUploads = 0;
        this.totalDownloads = 0;
        this.totalBytesTransferred = 0;

        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(storageDirectory);
            loadFileIndex();
        } catch (IOException e) {
            throw new RuntimeException("Error inicializando StorageManager", e);
        }
    }

    private void loadFileIndex() throws IOException {
        Path indexPath = storageDirectory.resolve("file_index.json");
        if (Files.exists(indexPath)) {
            // Implementar carga del índice desde JSON
        }
    }

    // Métodos principales de almacenamiento
    public String storeFile(byte[] content, String fileName, String mimeType, String uploadedBy) throws IOException {
        // Almacenar en IPFS
        String ipfsHash = ipfsNode.addFile(content, fileName, mimeType);

        // Crear metadata
        FileMetadata metadata = new FileMetadata(fileName, mimeType, content.length, ipfsHash, uploadedBy);
        fileMetadata.put(ipfsHash, metadata);

        // Actualizar índice
        fileIndex.put(fileName, ipfsHash);

        // Actualizar estadísticas
        totalUploads++;
        totalBytesTransferred += content.length;

        // Añadir al cache si hay espacio
        addToCache(ipfsHash, content);

        return ipfsHash;
    }

    public byte[] retrieveFile(String fileIdentifier) throws IOException {
        String ipfsHash = fileIndex.getOrDefault(fileIdentifier, fileIdentifier);

        // Intentar obtener del cache
        byte[] cachedContent = cache.get(ipfsHash);
        if (cachedContent != null) {
            updateAccessStatistics(ipfsHash);
            return cachedContent;
        }

        // Obtener de IPFS
        byte[] content = ipfsNode.getFile(ipfsHash);

        // Actualizar estadísticas
        FileMetadata metadata = fileMetadata.get(ipfsHash);
        if (metadata != null) {
            metadata.lastAccessed = LocalDateTime.now();
            metadata.accessCount++;
        }

        totalDownloads++;
        totalBytesTransferred += content.length;

        // Añadir al cache
        addToCache(ipfsHash, content);

        return content;
    }

    public void deleteFile(String fileIdentifier) throws IOException {
        String ipfsHash = fileIndex.get(fileIdentifier);
        if (ipfsHash != null) {
            // Eliminar del índice y metadata
            fileIndex.remove(fileIdentifier);
            fileMetadata.remove(ipfsHash);

            // Eliminar del cache
            removeFromCache(ipfsHash);

            // Desanclar de IPFS
            ipfsNode.unpinFile(ipfsHash);
        }
    }

    // Gestión de cache
    private void addToCache(String hash, byte[] content) {
        if (currentCacheSize + content.length > maxCacheSize) {
            clearCacheSpace(content.length);
        }

        if (currentCacheSize + content.length <= maxCacheSize) {
            cache.put(hash, content);
            currentCacheSize += content.length;
        }
    }

    private void removeFromCache(String hash) {
        byte[] content = cache.remove(hash);
        if (content != null) {
            currentCacheSize -= content.length;
        }
    }

    private void clearCacheSpace(long required) {
        // Implementar política de reemplazo LRU
        List<Map.Entry<String, FileMetadata>> sortedEntries =
                fileMetadata.entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getValue().lastAccessed))
                        .collect(Collectors.toList());

        for (Map.Entry<String, FileMetadata> entry : sortedEntries) {
            if (currentCacheSize + required <= maxCacheSize) {
                break;
            }
            removeFromCache(entry.getKey());
        }
    }

    // Métodos de gestión de archivos temporales
    public String createTemporaryFile(byte[] content, String fileName, String mimeType, String uploadedBy)
            throws IOException {
        String hash = storeFile(content, fileName, mimeType, uploadedBy);
        temporaryFiles.add(hash);
        FileMetadata metadata = fileMetadata.get(hash);
        if (metadata != null) {
            metadata.isTemporary = true;
        }
        return hash;
    }

    public void cleanupTemporaryFiles() {
        LocalDateTime cleanupThreshold = LocalDateTime.now().minusHours(24);
        Set<String> toRemove = temporaryFiles.stream()
                .filter(hash -> {
                    FileMetadata metadata = fileMetadata.get(hash);
                    return metadata != null &&
                            metadata.uploadDate.isBefore(cleanupThreshold);
                })
                .collect(Collectors.toSet());

        toRemove.forEach(hash -> {
            try {
                deleteFile(hash);
                temporaryFiles.remove(hash);
            } catch (IOException e) {
                // Manejar error
            }
        });
    }

    // Métodos de utilidad y estadísticas
    private void updateAccessStatistics(String ipfsHash) {
        FileMetadata metadata = fileMetadata.get(ipfsHash);
        if (metadata != null) {
            metadata.lastAccessed = LocalDateTime.now();
            metadata.accessCount++;
        }
    }

    public Map<String, Object> getStorageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiles", fileIndex.size());
        stats.put("totalUploads", totalUploads);
        stats.put("totalDownloads", totalDownloads);
        stats.put("totalBytesTransferred", totalBytesTransferred);
        stats.put("cacheSize", currentCacheSize);
        stats.put("maxCacheSize", maxCacheSize);
        stats.put("temporaryFiles", temporaryFiles.size());
        return stats;
    }

    // Getters
    public Map<String, String> getFileIndex() {
        return new HashMap<>(fileIndex);
    }

    public Map<String, FileMetadata> getFileMetadata() {
        return new HashMap<>(fileMetadata);
    }

    public long getCurrentCacheSize() {
        return currentCacheSize;
    }

    public int getTotalUploads() {
        return totalUploads;
    }

    public int getTotalDownloads() {
        return totalDownloads;
    }

    public long getTotalBytesTransferred() {
        return totalBytesTransferred;
    }
}