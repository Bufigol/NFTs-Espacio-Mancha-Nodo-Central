package com.bufigol.nftsespaciomanchanodocentral.model.network;
import com.bufigol.nftsespaciomanchanodocentral.model.enumeraciones.MessagePriority;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.io.Serializable;

public class Message implements Serializable {
    // Identificación del mensaje
    private final UUID id;
    private final String type;
    private final int version;

    // Información de enrutamiento
    private final String senderNodeId;
    private final String receiverNodeId;
    private final MessagePriority priority;

    // Contenido y metadata
    private final Object payload;
    private final Map<String, String> headers;
    private final byte[] signature;

    // Control y seguimiento
    private final LocalDateTime timestamp;
    private final LocalDateTime expirationTime;
    private final String correlationId;
    private final int retryCount;


    // Constructor principal
    public Message(String type, String senderNodeId, String receiverNodeId, Object payload) {
        this(type, senderNodeId, receiverNodeId, payload, null, MessagePriority.MEDIUM);
    }

    // Constructor completo
    public Message(String type,
                   String senderNodeId,
                   String receiverNodeId,
                   Object payload,
                   String correlationId,
                   MessagePriority priority) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.version = 1;
        this.senderNodeId = senderNodeId;
        this.receiverNodeId = receiverNodeId;
        this.payload = payload;
        this.priority = priority;
        this.correlationId = correlationId;
        this.timestamp = LocalDateTime.now();
        this.expirationTime = calculateExpirationTime();
        this.headers = new HashMap<>();
        this.signature = generateSignature();
        this.retryCount = 0;

        initializeHeaders();
    }

    // Constructor para retry
    private Message(Message original, int newRetryCount) {
        this.id = original.id;
        this.type = original.type;
        this.version = original.version;
        this.senderNodeId = original.senderNodeId;
        this.receiverNodeId = original.receiverNodeId;
        this.payload = original.payload;
        this.priority = original.priority;
        this.correlationId = original.correlationId;
        this.timestamp = LocalDateTime.now();
        this.expirationTime = calculateExpirationTime();
        this.headers = new HashMap<>(original.headers);
        this.signature = original.signature;
        this.retryCount = newRetryCount;
    }

    private void initializeHeaders() {
        headers.put("messageId", id.toString());
        headers.put("timestamp", timestamp.toString());
        headers.put("type", type);
        headers.put("version", String.valueOf(version));
        headers.put("priority", priority.name());
    }

    private LocalDateTime calculateExpirationTime() {
        // Tiempo de expiración basado en la prioridad
        switch (priority) {
            case HIGH:
                return timestamp.plusMinutes(5);
            case MEDIUM:
                return timestamp.plusMinutes(15);
            case LOW:
                return timestamp.plusMinutes(30);
            default:
                return timestamp.plusMinutes(15);
        }
    }

    private byte[] generateSignature() {
        // Implementar generación de firma digital
        return new byte[0]; // Placeholder
    }

    // Métodos de validación
    public boolean isValid() {
        return !isExpired() &&
                verifySignature() &&
                validateContent();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    private boolean verifySignature() {
        // Implementar verificación de firma
        return true; // Placeholder
    }

    private boolean validateContent() {
        return type != null &&
                senderNodeId != null &&
                receiverNodeId != null &&
                payload != null;
    }

    // Métodos de utilidad
    public Message createRetry() {
        if (retryCount >= 3) {
            throw new IllegalStateException("Máximo número de reintentos alcanzado");
        }
        return new Message(this, retryCount + 1);
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public boolean hasHeader(String key) {
        return headers.containsKey(key);
    }

    // Cálculo de tamaño aproximado del mensaje
    public long approximateSize() {
        long size = 0;

        // Tamaños básicos
        size += 16; // UUID
        size += type.length() * 2;
        size += 4; // version
        size += senderNodeId.length() * 2;
        size += receiverNodeId.length() * 2;
        size += 8; // timestamp
        size += 8; // expirationTime

        // Headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            size += entry.getKey().length() * 2;
            size += entry.getValue().length() * 2;
        }

        // Payload (estimación básica)
        if (payload != null) {
            size += payload.toString().length() * 2;
        }

        // Signature
        if (signature != null) {
            size += signature.length;
        }

        return size;
    }

    // Getters
    public UUID getId() { return id; }
    public String getType() { return type; }
    public int getVersion() { return version; }
    public String getSenderNodeId() { return senderNodeId; }
    public String getReceiverNodeId() { return receiverNodeId; }
    public MessagePriority getPriority() { return priority; }
    public Object getPayload() { return payload; }
    public Map<String, String> getHeaders() { return new HashMap<>(headers); }
    public byte[] getSignature() { return signature.clone(); }
    public LocalDateTime getTimestamp() { return timestamp; }
    public LocalDateTime getExpirationTime() { return expirationTime; }
    public String getCorrelationId() { return correlationId; }
    public int getRetryCount() { return retryCount; }

    @Override
    public String toString() {
        return String.format(
                "Message[id=%s, type=%s, sender=%s, receiver=%s, priority=%s]",
                id,
                type,
                senderNodeId,
                receiverNodeId,
                priority
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return id.equals(message.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}