package com.bosbase.sdk;

import java.util.Date;

public class CookieOptions {
    public final boolean secure;
    public final boolean sameSite;
    public final boolean httpOnly;
    public final String path;
    public final Date expires;

    public CookieOptions() {
        this(true, true, true, "/", null);
    }

    public CookieOptions(boolean secure, boolean sameSite, boolean httpOnly, String path, Date expires) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.httpOnly = httpOnly;
        this.path = path;
        this.expires = expires;
    }

    public CookieOptions withExpires(Date newExpires) {
        return new CookieOptions(secure, sameSite, httpOnly, path, newExpires);
    }
}
