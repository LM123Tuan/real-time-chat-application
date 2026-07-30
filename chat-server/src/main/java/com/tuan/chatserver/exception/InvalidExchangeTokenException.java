package com.tuan.chatserver.exception;

public class InvalidExchangeTokenException extends RuntimeException {
    public InvalidExchangeTokenException(String exchangeToken) {
        super("Invalid exchange token, exchangeToken: "+exchangeToken);
    }
}
