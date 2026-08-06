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
public class RenameGroupChatRequest {
    @NotNull(message = "groupChatId cannot blank!")
    private Long groupChatId;
    @NotBlank(message = "group new name cannot blank!")
    private String newName;
}
