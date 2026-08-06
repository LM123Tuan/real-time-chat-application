package com.tuan.chatserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    @NotNull(message = "ChatBoxId cannot null!")
    private Long chatBoxId;
    @NotBlank(message = "Message content cannot blank!")
    private String content;
}
