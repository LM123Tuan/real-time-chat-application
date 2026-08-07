package com.tuan.chatserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CursorPaginationResponse<T, C>{
    private T data;
    private LocalDateTime nextTimestamp;
    private C nextCursor;
    private boolean hasNext;
}
