package com.tuan.chatserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.tuan.chatserver.dto.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler(WrongPasswordOrInactiveAccountException.class)
    public ResponseEntity<ErrorResponse> handleWrongPasswordOrInactiveAccountException(WrongPasswordOrInactiveAccountException e){
        logger.warn("Handled WrongPasswordOrInactiveAccountException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(UsernameOrEmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameOrEmailAlreadyExistsException(UsernameOrEmailAlreadyExistsException e){
        logger.warn("Handled UsernameOrEmailAlreadyExistsException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(DataAccessFailureException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessFailureException(DataAccessFailureException e){
        logger.error("Handled DataAccessFailureException: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ChatBoxAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleChatBoxAlreadyExistsException(ChatBoxAlreadyExistsException e){
        logger.warn("Handled ChatBoxAlreadyExistsException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ChatBoxNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatBoxNotFoundException(ChatBoxNotFoundException e){
        logger.warn("Handled ChatBoxNotFoundException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(EmptyMessageContentException.class)
    public ResponseEntity<ErrorResponse> handleEmptyMessageContentException(EmptyMessageContentException e){
        logger.warn("Handled EmptyMessageContentException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidChatBoxOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidChatBoxOperationException(InvalidChatBoxOperationException e){
        logger.warn("Handled InvalidChatBoxOperationException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MessageAlreadyRecallException.class)
    public ResponseEntity<ErrorResponse> handleMessageAlreadyRecallException(MessageAlreadyRecallException e){
        logger.warn("Handled MessageAlreadyRecallException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MessageAlreadySeenException.class)
    public ResponseEntity<ErrorResponse> handleMessageAlreadySeenException(MessageAlreadySeenException e){
        logger.warn("Handled MessageAlreadySeenException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MessageNotExistsException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotExistsException(MessageNotExistsException e){
        logger.warn("Handled MessageNotExistsException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(NotEnoughMembersException.class)
    public ResponseEntity<ErrorResponse> handleNotEnoughMembersException(NotEnoughMembersException e){
        logger.warn("Handled NotEnoughMembersException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(UserAlreadyInChatBoxException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyInChatBoxException(UserAlreadyInChatBoxException e){
        logger.warn("Handled UserAlreadyInChatBoxException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e){
        logger.warn("Handled UserNotFoundException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UserNotInChatBoxException.class)
    public ResponseEntity<ErrorResponse> handleUserNotInChatBoxException(UserNotInChatBoxException e){
        logger.warn("Handled UserNotInChatBoxException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshTokenException(InvalidRefreshTokenException e){
        logger.warn("Handled InvalidRefreshTokenException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(RefreshTokenExpiredOrNotExistsException.class)
    public ResponseEntity<ErrorResponse> handleRefreshTokenExpiredException(RefreshTokenExpiredOrNotExistsException e){
        logger.warn("Handled RefreshTokenExpiredException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(InvalidExchangeTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidExchangeTokenException(InvalidExchangeTokenException e){
        logger.warn("Handled InvalidExchangeTokenException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(InvalidResetPasswordTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidResetPasswordTokenException(InvalidResetPasswordTokenException e){
        logger.warn("Handled InvalidResetPasswordTokenException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationTokenException(InvalidVerificationTokenException e){
        logger.warn("Handled InvalidVerificationTokenException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(EmailSendingFailureException.class)
    public ResponseEntity<ErrorResponse> handleEmailSendingFailureException(EmailSendingFailureException e){
        logger.error("Handled EmailSendingFailureException: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
