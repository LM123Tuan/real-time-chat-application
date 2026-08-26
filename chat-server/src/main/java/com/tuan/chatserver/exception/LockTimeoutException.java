package com.tuan.chatserver.exception;

import com.tuan.chatserver.enums.EntityType;

public class LockTimeoutException extends RuntimeException {

    public LockTimeoutException(EntityType entityName, Long id) {
        super(String.format("Timed out waiting for lock on %s with id=%d", entityName, id));
    }
}