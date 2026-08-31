package com.tuan.chatserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PresenceBatchResponse{
    private List<PresenceResponse> responses;
}
