package com.bufigol.nftsespaciomanchanodocentral.model.blockchain;

import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class BlockChain {
    // Estructura principal
    private final List<Block> chain;
    private final List<Transaction> pendingTransactions;

    // Configuración
    private int difficulty;
    private final double minerReward;
    private final int maxTransactionsPerBlock;

    // Control de estado
    private final ReentrantLock chainLock;
    private boolean isProcessingBlock;
    private LocalDateTime lastBlockTime;
    private int targetBlockTimeSeconds;

    // Métricas y estadísticas
    private long totalTransactions;
    private double totalFeesCollected;
    private Map<String, Integer> minerStats;

    // Constructor
    public BlockChain(int initialDifficulty) {
        this.chain = new CopyOnWriteArrayList<>();
        this.pendingTransactions = new ArrayList<>();
        this.difficulty = initialDifficulty;
        this.minerReward = 100.0; // Recompensa inicial
        this.maxTransactionsPerBlock = 1000;
        this.chainLock = new ReentrantLock();
        this.isProcessingBlock = false;
        this.targetBlockTimeSeconds = 60;
        this.minerStats = new HashMap<>();

        // Crear bloque génesis
        createGenesisBlock();
    }

    private void createGenesisBlock() {
        Block genesisBlock = new Block(difficulty);
        genesisBlock.mineBlock();
        chain.add(genesisBlock);
        lastBlockTime = LocalDateTime.now();
    }

    // Métodos principales de la blockchain
    public void addTransaction(Transaction transaction) {
        // Validar transacción
        if (!isValidTransaction(transaction)) {
            throw new IllegalArgumentException("Transacción inválida");
        }

        synchronized(pendingTransactions) {
            pendingTransactions.add(transaction);
        }
    }

    public Block minePendingTransactions(String minerAddress) {
        chainLock.lock();
        try {
            if (isProcessingBlock) {
                throw new IllegalStateException("Ya se está procesando un bloque");
            }
            isProcessingBlock = true;

            // Seleccionar transacciones pendientes
            List<Transaction> transactionsToMine = selectTransactionsForBlock();

            // Crear nuevo bloque
            Block newBlock = new Block(
                    chain.size(),
                    getLatestBlock().getHash(),
                    transactionsToMine,
                    minerAddress,
                    calculateMinerReward(),
                    difficulty
            );

            // Minar el bloque
            newBlock.mineBlock();

            // Validar y añadir el bloque
            if (isValidBlock(newBlock)) {
                addBlock(newBlock);
                updateMinerStats(minerAddress);
                removeMinedTransactions(transactionsToMine);
                adjustDifficulty();
                return newBlock;
            } else {
                throw new IllegalStateException("Bloque minado inválido");
            }
        } finally {
            isProcessingBlock = false;
            chainLock.unlock();
        }
    }

    private List<Transaction> selectTransactionsForBlock() {
        synchronized(pendingTransactions) {
            // Ordenar por prioridad y fee
            pendingTransactions.sort((t1, t2) -> {
                if (t1.getPriority() != t2.getPriority()) {
                    return Integer.compare(t2.getPriority(), t1.getPriority());
                }
                return Double.compare(t2.getFee(), t1.getFee());
            });

            // Seleccionar transacciones hasta el límite
            List<Transaction> selected = new ArrayList<>();
            Iterator<Transaction> iterator = pendingTransactions.iterator();
            while (iterator.hasNext() && selected.size() < maxTransactionsPerBlock) {
                Transaction tx = iterator.next();
                if (isValidTransaction(tx)) {
                    selected.add(tx);
                }
            }
            return selected;
        }
    }

    private void removeMinedTransactions(List<Transaction> minedTransactions) {
        synchronized(pendingTransactions) {
            pendingTransactions.removeAll(minedTransactions);
        }
    }

    private double calculateMinerReward() {
        // Calcular recompensa base + fees
        double totalFees = pendingTransactions.stream()
                .mapToDouble(Transaction::getFee)
                .sum();
        return minerReward + totalFees;
    }

    // Métodos de validación
    private boolean isValidBlock(Block block) {
        // Validar hash del bloque anterior
        if (!block.getPreviousHash().equals(getLatestBlock().getHash())) {
            return false;
        }

        // Validar hash del bloque actual
        if (!block.getHash().equals(block.calculateHash())) {
            return false;
        }

        // Validar proof of work
        String target = new String(new char[difficulty]).replace('\0', '0');
        if (!block.getHash().substring(0, difficulty).equals(target)) {
            return false;
        }

        // Validar transacciones
        return block.getTransactions().stream().allMatch(this::isValidTransaction);
    }

    private boolean isValidTransaction(Transaction transaction) {
        // Implementar validaciones específicas
        return true; // Placeholder
    }

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            // Validar hash actual
            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                return false;
            }

            // Validar conexión con bloque anterior
            if (!currentBlock.getPreviousHash().equals(previousBlock.getHash())) {
                return false;
            }
        }
        return true;
    }

    // Métodos de ajuste y mantenimiento
    private void adjustDifficulty() {
        LocalDateTime now = LocalDateTime.now();
        long secondsSinceLastBlock = java.time.Duration.between(lastBlockTime, now).getSeconds();

        if (secondsSinceLastBlock < targetBlockTimeSeconds / 2) {
            difficulty++;
        } else if (secondsSinceLastBlock > targetBlockTimeSeconds * 2) {
            difficulty = Math.max(1, difficulty - 1);
        }

        lastBlockTime = now;
    }

    private void updateMinerStats(String minerAddress) {
        minerStats.merge(minerAddress, 1, Integer::sum);
        totalTransactions += getLatestBlock().getTransactions().size();
        totalFeesCollected += getLatestBlock().getTransactions().stream()
                .mapToDouble(Transaction::getFee)
                .sum();
    }

    // Métodos de consulta
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public Block getBlockByHash(String hash) {
        return chain.stream()
                .filter(block -> block.getHash().equals(hash))
                .findFirst()
                .orElse(null);
    }

    public List<Transaction> getPendingTransactions() {
        synchronized(pendingTransactions) {
            return new ArrayList<>(pendingTransactions);
        }
    }

    public List<Block> getChain() {
        return new ArrayList<>(chain);
    }

    // Getters para estadísticas
    public int getDifficulty() { return difficulty; }
    public long getTotalTransactions() { return totalTransactions; }
    public double getTotalFeesCollected() { return totalFeesCollected; }
    public Map<String, Integer> getMinerStats() {
        return new HashMap<>(minerStats);
    }

    public Map<String, Object> getBlockchainStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBlocks", chain.size());
        stats.put("totalTransactions", totalTransactions);
        stats.put("difficulty", difficulty);
        stats.put("pendingTransactions", pendingTransactions.size());
        stats.put("lastBlockTime", lastBlockTime);
        stats.put("totalFeesCollected", totalFeesCollected);
        stats.put("minerStats", new HashMap<>(minerStats));
        return stats;
    }
}