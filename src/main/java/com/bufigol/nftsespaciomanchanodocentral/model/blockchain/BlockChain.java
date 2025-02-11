package com.bufigol.nftsespaciomanchanodocentral.model.blockchain;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.locks.ReentrantLock;

public class BlockChain {
    // Estructura principal
    private final List<Block> chain;
    private final ConcurrentLinkedQueue<Transaction> pendingTransactions;

    // Configuración
    private volatile int difficulty;
    private final double minerReward;
    private final int maxTransactionsPerBlock;

    // Control de estado
    private final ReentrantLock chainLock;
    private final AtomicBoolean isProcessingBlock;
    private volatile LocalDateTime lastBlockTime;
    private final int targetBlockTimeSeconds;

    // Métricas y estadísticas
    private final AtomicLong totalTransactions;
    private final DoubleAdder totalFeesCollected;
    private final ConcurrentHashMap<String, Integer> minerStats;

    // Constructor
    public BlockChain(int initialDifficulty) {
        this.chain = new CopyOnWriteArrayList<>();
        this.pendingTransactions = new ConcurrentLinkedQueue<>();
        this.difficulty = initialDifficulty;
        this.minerReward = 100.0;
        this.maxTransactionsPerBlock = 1000;
        this.chainLock = new ReentrantLock();
        this.isProcessingBlock = new AtomicBoolean(false);
        this.targetBlockTimeSeconds = 60;
        this.totalTransactions = new AtomicLong(0);
        this.totalFeesCollected = new DoubleAdder();
        this.minerStats = new ConcurrentHashMap<>();

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
        if (!isValidTransaction(transaction)) {
            throw new IllegalArgumentException("Transacción inválida");
        }
        pendingTransactions.offer(transaction);
    }

    public Block minePendingTransactions(String minerAddress) {
        if (!isProcessingBlock.compareAndSet(false, true)) {
            throw new IllegalStateException("Ya se está procesando un bloque");
        }

        try {
            chainLock.lock();
            List<Transaction> transactionsToMine = selectTransactionsForBlock();

            Block newBlock = new Block(
                    chain.size(),
                    getLatestBlock().getHash(),
                    transactionsToMine,
                    minerAddress,
                    calculateMinerReward(),
                    difficulty
            );

            newBlock.mineBlock();

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
            isProcessingBlock.set(false);
            chainLock.unlock();
        }
    }

    @NotNull
    private List<Transaction> selectTransactionsForBlock() {
        List<Transaction> selected = new ArrayList<>();
        PriorityQueue<Transaction> priorityQueue = new PriorityQueue<>((t1, t2) -> {
            if (t1.getPriority() != t2.getPriority()) {
                return Integer.compare(t2.getPriority(), t1.getPriority());
            }
            return Double.compare(t2.getFee(), t1.getFee());
        });

        pendingTransactions.forEach(priorityQueue::offer);

        while (!priorityQueue.isEmpty() && selected.size() < maxTransactionsPerBlock) {
            Transaction tx = priorityQueue.poll();
            if (isValidTransaction(tx)) {
                selected.add(tx);
            }
        }

        return selected;
    }


    private void removeMinedTransactions(List<Transaction> minedTransactions) {
        pendingTransactions.removeAll(minedTransactions);
    }

    private double calculateMinerReward() {
        return minerReward + pendingTransactions.stream()
                .mapToDouble(Transaction::getFee)
                .sum();
    }

    // Métodos de validación
    private boolean isValidBlock(@NotNull Block block) {
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

        int currentDifficulty = this.difficulty; // capturar valor actual
        int newDifficulty;

        if (secondsSinceLastBlock < targetBlockTimeSeconds / 2) {
            newDifficulty = currentDifficulty + 1;
        } else if (secondsSinceLastBlock > targetBlockTimeSeconds * 2L) {
            newDifficulty = Math.max(1, currentDifficulty - 1);
        } else {
            return;
        }

        this.difficulty = newDifficulty;
        lastBlockTime = now;
    }

    private void updateMinerStats(String minerAddress) {
        minerStats.merge(minerAddress, 1, Integer::sum);
        totalTransactions.addAndGet(getLatestBlock().getTransactions().size());
        double fees = getLatestBlock().getTransactions().stream()
                .mapToDouble(Transaction::getFee)
                .sum();
        totalFeesCollected.add(fees);
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
    public long getTotalTransactions() {
        return totalTransactions.get();
    }

    public DoubleAdder getTotalFeesCollected() { return totalFeesCollected; }
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

    private void addBlock(@NotNull Block block) {
        chainLock.lock();
        try {
            // Verificar que el bloque conecta con el último de la cadena
            if (!block.getPreviousHash().equals(getLatestBlock().getHash())) {
                throw new IllegalStateException("El bloque no conecta con la cadena actual");
            }

            // Añadir el bloque a la cadena
            chain.add(block);

            // Actualizar el estado de las transacciones
            for (Transaction tx : block.getTransactions()) {
                tx.confirm(block.getHash());
            }

            // Propagar confirmaciones a bloques anteriores
            for (Block previousBlock : chain) {
                previousBlock.incrementConfirmations();
            }

            // Actualizar timestamp del último bloque
            lastBlockTime = LocalDateTime.now();

            // Actualizar métricas
            totalTransactions.addAndGet(block.getTransactions().size());
            totalFeesCollected.add(block.getTransactions().stream()
                    .mapToDouble(Transaction::getFee)
                    .sum());

        } finally {
            chainLock.unlock();
        }
    }
}