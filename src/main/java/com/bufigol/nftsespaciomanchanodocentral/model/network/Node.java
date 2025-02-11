package com.bufigol.nftsespaciomanchanodocentral.model.network;
import com.bufigol.nftsespaciomanchanodocentral.model.enumeraciones.NodeCapability;

import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class Node {
    // Identificación del nodo
    private final String nodeId;
    private final String host;
    private final int port;
    private final NodeRole role;

    // Estado del nodo
    private NodeStatus status;
    private LocalDateTime lastHeartbeat;
    private int failedHeartbeats;

    // Conexiones y peers
    private final Map<String, Node> connectedPeers;
    private final Set<String> blacklistedPeers;
    private Node parentNode; // Para nodos cliente
    private final List<Node> childNodes; // Para nodo central

    // Capacidades y recursos
    private final Set<NodeCapability> capabilities;
    private double resourceScore;
    private int maxConnections;

    public enum NodeRole {
        CENTRAL("Central"),
        CLIENT("Cliente"),
        VALIDATOR("Validador"),
        STORAGE("Almacenamiento");

        private final String descripcion;

        NodeRole(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum NodeStatus {
        ACTIVE("Activo"),
        INACTIVE("Inactivo"),
        SYNCING("Sincronizando"),
        SUSPENDED("Suspendido"),
        MAINTENANCE("Mantenimiento");

        private final String descripcion;

        NodeStatus(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }


    // Constructor
    public Node(String host, int port, NodeRole role) {
        this.nodeId = generateNodeId();
        this.host = host;
        this.port = port;
        this.role = role;

        this.status = NodeStatus.INACTIVE;
        this.lastHeartbeat = LocalDateTime.now();
        this.failedHeartbeats = 0;

        this.connectedPeers = new ConcurrentHashMap<>();
        this.blacklistedPeers = ConcurrentHashMap.newKeySet();
        this.childNodes = new ArrayList<>();
        this.capabilities = EnumSet.noneOf(NodeCapability.class);

        this.maxConnections = calculateMaxConnections();
        this.resourceScore = calculateResourceScore();

        initializeCapabilities();
    }

    private String generateNodeId() {
        return UUID.randomUUID().toString();
    }

    private void initializeCapabilities() {
        switch (role) {
            case CENTRAL:
                capabilities.add(NodeCapability.VALIDATION);
                capabilities.add(NodeCapability.RELAY);
                capabilities.add(NodeCapability.API_GATEWAY);
                break;
            case CLIENT:
                capabilities.add(NodeCapability.MINING);
                break;
            case VALIDATOR:
                capabilities.add(NodeCapability.VALIDATION);
                capabilities.add(NodeCapability.RELAY);
                break;
            case STORAGE:
                capabilities.add(NodeCapability.STORAGE);
                break;
        }
    }

    // Métodos de conexión
    public boolean connect(Node peer) {
        if (canConnectToPeer(peer)) {
            connectedPeers.put(peer.getNodeId(), peer);
            if (role == NodeRole.CLIENT && peer.getRole() == NodeRole.CENTRAL) {
                parentNode = peer;
            }
            if (role == NodeRole.CENTRAL && peer.getRole() == NodeRole.CLIENT) {
                childNodes.add(peer);
            }
            return true;
        }
        return false;
    }

    public void disconnect(String peerId) {
        Node peer = connectedPeers.remove(peerId);
        if (peer != null) {
            if (peer == parentNode) {
                parentNode = null;
            }
            childNodes.remove(peer);
        }
    }

    private boolean canConnectToPeer(Node peer) {
        return !blacklistedPeers.contains(peer.getNodeId()) &&
                connectedPeers.size() < maxConnections &&
                !peer.getNodeId().equals(nodeId) &&
                isCompatibleRole(peer.getRole());
    }

    private boolean isCompatibleRole(NodeRole peerRole) {
        if (role == NodeRole.CLIENT) {
            return peerRole == NodeRole.CENTRAL;
        }
        if (role == NodeRole.CENTRAL) {
            return peerRole == NodeRole.CLIENT || peerRole == NodeRole.VALIDATOR;
        }
        return true;
    }

    // Gestión de estado
    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
        this.failedHeartbeats = 0;
    }

    public void incrementFailedHeartbeat() {
        this.failedHeartbeats++;
        if (failedHeartbeats > 3) {
            this.status = NodeStatus.INACTIVE;
        }
    }

    public void setStatus(NodeStatus newStatus) {
        this.status = newStatus;
        notifyStatusChange();
    }

    private void notifyStatusChange() {
        // Notificar a los peers del cambio de estado
        connectedPeers.values().forEach(peer ->
                peer.handlePeerStatusChange(this.nodeId, this.status)
        );
    }

    public void handlePeerStatusChange(String peerId, NodeStatus newStatus) {
        Node peer = connectedPeers.get(peerId);
        if (peer != null) {
            if (newStatus == NodeStatus.INACTIVE) {
                disconnect(peerId);
            }
        }
    }

    // Cálculos y métricas
    private int calculateMaxConnections() {
        switch (role) {
            case CENTRAL:
                return 100;
            case VALIDATOR:
                return 50;
            case STORAGE:
                return 30;
            case CLIENT:
                return 5;
            default:
                return 10;
        }
    }

    private double calculateResourceScore() {
        // Implementar cálculo basado en recursos disponibles
        return 1.0;
    }

    // Gestión de capacidades
    public void addCapability(NodeCapability capability) {
        capabilities.add(capability);
    }

    public void removeCapability(NodeCapability capability) {
        capabilities.remove(capability);
    }

    public boolean hasCapability(NodeCapability capability) {
        return capabilities.contains(capability);
    }

    // Getters
    public String getNodeId() { return nodeId; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public NodeRole getRole() { return role; }
    public NodeStatus getStatus() { return status; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public int getFailedHeartbeats() { return failedHeartbeats; }
    public Node getParentNode() { return parentNode; }
    public double getResourceScore() { return resourceScore; }

    public Map<String, Node> getConnectedPeers() {
        return new HashMap<>(connectedPeers);
    }

    public List<Node> getChildNodes() {
        return new ArrayList<>(childNodes);
    }

    public Set<NodeCapability> getCapabilities() {
        return EnumSet.copyOf(capabilities);
    }

    // Método para obtener estadísticas del nodo
    public Map<String, Object> getNodeStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("nodeId", nodeId);
        stats.put("role", role);
        stats.put("status", status);
        stats.put("connectedPeers", connectedPeers.size());
        stats.put("capabilities", capabilities);
        stats.put("resourceScore", resourceScore);
        stats.put("lastHeartbeat", lastHeartbeat);
        return stats;
    }
}