package com.bosbase.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class ResultList<T extends JsonNode> {
    public final int page;
    public final int perPage;
    public final int totalItems;
    public final List<T> items;
    public final JsonNode raw;

    public ResultList(int page, int perPage, int totalItems, java.util.List<T> items, JsonNode raw) {
        this.page = page;
        this.perPage = perPage;
        this.totalItems = totalItems;
        this.items = items;
        this.raw = raw;
    }
}
