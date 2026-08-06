package com.example.mobilnaaplikacijatim29.data.model;

import java.util.Collections;
import java.util.List;

public class PageResponse<T> {
    private List<T> content;

    public List<T> getContent() {
        return content == null ? Collections.emptyList() : content;
    }
}
