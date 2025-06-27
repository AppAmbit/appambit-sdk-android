package com.appambit.sdk.core.models.logs;

import com.appambit.sdk.core.utils.JsonKey;

public class LogResponse {
    @JsonKey("id")
    private int id;

    @JsonKey("hash")
    private String hash;

    @JsonKey("consumers")
    private int consumers;

    @JsonKey("occurrences")
    private int occurrences;

    @JsonKey("classFQN")
    private String classFQN;

    @JsonKey("message")
    private String message;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getConsumers() {
        return consumers;
    }

    public void setConsumers(int consumers) {
        this.consumers = consumers;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

    public String getClassFQN() {
        return classFQN;
    }

    public void setClassFQN(String classFQN) {
        this.classFQN = classFQN;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
