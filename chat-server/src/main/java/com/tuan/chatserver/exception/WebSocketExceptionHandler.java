package com.tuan.chatserver.exception;

import com.tuan.chatserver.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class WebSocketExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @MessageExceptionHandler(UserNotFoundException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleUserNotFound(UserNotFoundException e) {
        logger.warn("WebSocket error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), 404, LocalDateTime.now());
    }

    @MessageExceptionHandler(ChatBoxNotFoundException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleChatBoxNotFound(ChatBoxNotFoundException e) {
        logger.warn("WebSocket error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), 404, LocalDateTime.now());
    }

    @MessageExceptionHandler(UserNotInChatBoxException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleUserNotInChatBox(UserNotInChatBoxException e) {
        logger.warn("WebSocket error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), 403, LocalDateTime.now());
    }

    @MessageExceptionHandler(MessageNotExistsException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleMessageNotExists(MessageNotExistsException e) {
        logger.warn("WebSocket error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), 404, LocalDateTime.now());
    }

    @MessageExceptionHandler(EmptyMessageContentException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleEmptyMessageContent(EmptyMessageContentException e) {
        logger.warn("WebSocket error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), 400, LocalDateTime.now());
    }

    @MessageExceptionHandler(InvalidMessageStatusException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleInvalidMessageStatus(InvalidMessageStatusException e) {
        logger.warn("WebSocket error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), 409, LocalDateTime.now());
    }

    @MessageExceptionHandler(UserIsNotMessageSenderException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleUserIsNotMessageSender(UserIsNotMessageSenderException e) {
        logger.warn("WebSocket error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), 403, LocalDateTime.now());
    }

    @MessageExceptionHandler(MessageAlreadyRecallException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleMessageAlreadyRecall(MessageAlreadyRecallException e) {
        logger.warn("WebSocket error: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), 409, LocalDateTime.now());
    }

    @MessageExceptionHandler(DataAccessFailureException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleDataAccessFailure(DataAccessFailureException e) {
        logger.error("WebSocket data access error", e);
        return new ErrorResponse("An error occurred while accessing data", 500, LocalDateTime.now());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleGenericException(Exception e) {
        logger.error("Unhandled WebSocket error", e);
        return new ErrorResponse("An unexpected error occurred", 500, LocalDateTime.now());
    }
}