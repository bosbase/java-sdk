package com.bosbase.sdk.services;

import com.bosbase.sdk.BosBase;

public class BaseService {
    protected final BosBase client;

    public BaseService(BosBase client) {
        this.client = client;
    }
}
