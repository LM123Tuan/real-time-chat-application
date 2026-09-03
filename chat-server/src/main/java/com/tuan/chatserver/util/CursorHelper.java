package com.tuan.chatserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tuan.chatserver.dto.PageCursor;
import org.springframework.stereotype.Component;

@Component
public class CursorHelper {

    private final CursorCodec cursorCodec;

    public CursorHelper(CursorCodec cursorCodec) {
        this.cursorCodec = cursorCodec;
    }

    public long extractPageNumber(String cursor) {
        if (cursor == null) {
            return 0;
        }
        PageCursor<Long> cursorData = cursorCodec.decode(cursor, new TypeReference<PageCursor<Long>>() {});
        return cursorData.getPageNumber();
    }
}