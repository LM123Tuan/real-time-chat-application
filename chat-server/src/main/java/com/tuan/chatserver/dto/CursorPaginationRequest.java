package com.tuan.chatserver.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CursorPaginationRequest{
    private String cursor;
    @Min(1)
    @Max(100)
    private Integer size = 20;
}
