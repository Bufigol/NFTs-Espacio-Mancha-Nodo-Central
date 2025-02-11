package com.bufigol.nftsespaciomanchanodocentral.model.network;

import java.util.*;
import java.util.concurrent.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkManager {
    // Configuración de red
    private final ServerSocket serverSocket;
    private final Map<String, Node> connectedNodes;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final Node localNode;
    private final int maxConnections;

    // Control de estado
    private final AtomicBoolean isRunning;
    private final Queue<Message> messageQueue;
    private final Map<String, List<Message>> undeliveredMessages;

    // Gestión de mensajes
    private final Map<String, MessageHandler> messageHandlers;
    private final Set<String> processedMessageIds;
    private final int messageTimeout;

    // Métricas y monitoreo
    private int totalMessagesSent;
    private int totalMessagesReceived;
    private final Map<String, NodeMetrics> nodeMetrics;

    private static class NodeMetrics {
        int messagesSent;
        int messagesReceived;
        int failedDeliveries;
        LocalDateTime lastActivity;
        long totalBytesTransferred;
        long latency; // en milisegundos

        NodeMetrics() {
            this.messagesSent = 0;
            this.messagesReceived = 0;
            this.failedDeliveries = 0;
            this.lastActivity = LocalDateTime.now();
            this.totalBytesTransferred = 0;
            this.latency = 0;
        }
    }

    // Constructor
    public NetworkManager(Node localNode, int port, int maxConnections) throws IOException {
        this.localNode = localNode;
        this.maxConnections = maxConnections;
        this.serverSocket = new ServerSocket(port);
        this.connectedNodes = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);

        this.isRunning = new AtomicBoolean(false);
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.undeliveredMessages = new ConcurrentHashMap<>();
        this.messageHandlers = new ConcurrentHashMap<>();
        this.processedMessageIds = ConcurrentHashMap.newKeySet();
        this.messageTimeout = 30000; // 30 segundos

        this.nodeMetrics = new ConcurrentHashMap<>();

        initializeMessageHandlers();
        startMaintenanceTasks();
    }

    private void initializeMessageHandlers() {
        // Registrar handlers para diferentes tipos de mensajes
        registerHandler("HEARTBEAT", this::handleHeartbeat);
        registerHandler("NODE_JOIN", this::handleNodeJoin);
        registerHandler("NODE_LEAVE", this::handleNodeLeave);
        registerHandler("NFT_TRANSFER", this::handleNFTTransfer);
        registerHandler("BLOCK_SYNC", this::handleBlockSync);
        registerHandler("TRANSACTION", this::handleTransaction);
    }

    private void startMaintenanceTasks() {
        // Programar tareas de mantenimiento
        scheduledExecutor.scheduleAtFixedRate(this::checkNodeHeartbeats, 0, 10, TimeUnit.SECONDS);
        scheduledExecutor.scheduleAtFixedRate(this::retryUndeliveredMessages, 0, 5, TimeUnit.SECONDS);
        scheduledExecutor.scheduleAtFixedRate(this::cleanupProcessedMessages, 0, 1, TimeUnit.HOURS);
    }

    // Métodos principales
    public void start() {
        isRunning.set(true);
        executorService.submit(this::acceptConnections);
        executorService.submit(this::processMessageQueue);
    }

    public void stop() {
        isRunning.set(false);
        executorService.shutdown();
        scheduledExecutor.shutdown();
        try {
            serverSocket.close();
        } catch (IOException e) {
            // Manejar error
        }
    }

    private void acceptConnections() {
        while (isRunning.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (connectedNodes.size() < maxConnections) {
                    handleNewConnection(clientSocket);
                } else {
                    clientSocket.close();
                }
            } catch (IOException e) {
                if (isRunning.get()) {
                    // Manejar error
                }
            }
        }
    }

    private void handleNewConnection(Socket socket) {
        executorService.submit(() -> {
            try {
                // Implementar protocolo de handshake
                // Verificar autenticación
                // Establecer conexión segura
            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        });
    }

    // Gestión de mensajes
    public void sendMessage(Message message) {
        if (!connectedNodes.containsKey(message.getReceiverNodeId())) {
            storeUndeliveredMessage(message);
            return;
        }

        try {
            // Enviar mensaje
            messageQueue.offer(message);
            updateMetrics(message, true);
        } catch (Exception e) {
            storeUndeliveredMessage(message);
        }
    }

    public void broadcast(Message message) {
        connectedNodes.values().forEach(node -> {
            Message broadcastMessage = new Message(
                    message.getType(),
                    localNode.getNodeId(),
                    node.getNodeId(),
                    message.getPayload()
            );
            sendMessage(broadcastMessage);
        });
    }

    private void processMessageQueue() {
        while (isRunning.get()) {
            Message message = messageQueue.poll();
            if (message != null) {
                processMessage(message);
            }
        }
    }

    private void processMessage(Message message) {
        if (processedMessageIds.contains(message.getId())) {
            return;
        }

        MessageHandler handler = messageHandlers.get(message.getType());
        if (handler != null) {
            try {
                handler.handle(message);
                processedMessageIds.add(message.getId());
                updateMetrics(message, false);
            } catch (Exception e) {
                // Manejar error
            }
        }
    }

    // Handlers de mensajes específicos
    private void handleHeartbeat(Message message) {
        String nodeId = message.getSenderNodeId();
        Node node = connectedNodes.get(nodeId);
        if (node != null) {
            node.updateHeartbeat();
            nodeMetrics.get(nodeId).lastActivity = LocalDateTime.now();
        }
    }

    private void handleNodeJoin(Message message) {
        // Implementar lógica de unión de nodo
    }

    private void handleNodeLeave(Message message) {
        // Implementar lógica de salida de nodo
    }

    private void handleNFTTransfer(Message message) {
        // Implementar lógica de transferencia de NFT
    }

    private void handleBlockSync(Message message) {
        // Implementar lógica de sincronización de bloques
    }

    private void handleTransaction(Message message) {
        // Implementar lógica de transacción
    }

    // Tareas de mantenimiento
    private void checkNodeHeartbeats() {
        LocalDateTime now = LocalDateTime.now();
        connectedNodes.values().forEach(node -> {
            if (now.minusSeconds(30).isAfter(node.getLastHeartbeat())) {
                node.incrementFailedHeartbeat();
                if (node.getFailedHeartbeats() > 3) {
                    disconnectNode(node.getNodeId());
                }
            }
        });
    }

    private void retryUndeliveredMessages() {
        undeliveredMessages.forEach((nodeId, messages) -> {
            if (connectedNodes.containsKey(nodeId)) {
                messages.forEach(this::sendMessage);
                undeliveredMessages.remove(nodeId);
            }
        });
    }

    private void cleanupProcessedMessages() {
        processedMessageIds.clear();
    }

    // Métodos de utilidad
    private void storeUndeliveredMessage(Message message) {
        undeliveredMessages
                .computeIfAbsent(message.getReceiverNodeId(), k -> new ArrayList<>())
                .add(message);
    }

    private void updateMetrics(Message message, boolean isSending) {
        String nodeId = isSending ? message.getReceiverNodeId() : message.getSenderNodeId();
        NodeMetrics metrics = nodeMetrics.computeIfAbsent(nodeId, k -> new NodeMetrics());

        if (isSending) {
            metrics.messagesSent++;
            totalMessagesSent++;
        } else {
            metrics.messagesReceived++;
            totalMessagesReceived++;
        }

        metrics.lastActivity = LocalDateTime.now();
        // Actualizar bytes transferidos basado en el tamaño del mensaje
    }

    public void registerHandler(String messageType, MessageHandler handler) {
        messageHandlers.put(messageType, handler);
    }

    public void disconnectNode(String nodeId) {
        Node node = connectedNodes.remove(nodeId);
        if (node != null) {
            // Limpiar recursos asociados
            nodeMetrics.remove(nodeId);
        }
    }

    // Interface para handlers de mensajes
    @FunctionalInterface
    private interface MessageHandler {
        void handle(Message message) throws Exception;
    }

    // Getters y estadísticas
    public Map<String, Node> getConnectedNodes() {
        return new HashMap<>(connectedNodes);
    }

    public Map<String, NodeMetrics> getNodeMetrics() {
        return new HashMap<>(nodeMetrics);
    }

    public Map<String, Object> getNetworkStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("connectedNodes", connectedNodes.size());
        stats.put("totalMessagesSent", totalMessagesSent);
        stats.put("totalMessagesReceived", totalMessagesReceived);
        stats.put("queueSize", messageQueue.size());
        stats.put("undeliveredMessages", undeliveredMessages.size());
        return stats;
    }
}