/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core.http;

import java.util.Locale;

public final class HttpMethod {
    public static final HttpMethod OPTIONS = new HttpMethod("OPTIONS");
    public static final HttpMethod GET = new HttpMethod("GET");
    public static final HttpMethod HEAD = new HttpMethod("HEAD");
    public static final HttpMethod POST = new HttpMethod("POST");
    public static final HttpMethod PUT = new HttpMethod("PUT");
    public static final HttpMethod PATCH = new HttpMethod("PATCH");
    public static final HttpMethod DELETE = new HttpMethod("DELETE");

    private final String name;

    private HttpMethod(String name) {
        this.name = name.toUpperCase(Locale.ENGLISH);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof HttpMethod other) {
            return other.name.equals(name);
        }

        return false;
    }

    public static HttpMethod valueOf(String name) {
        return switch (name) {
            case "OPTIONS" -> HttpMethod.OPTIONS;
            case "GET" -> HttpMethod.GET;
            case "HEAD" -> HttpMethod.HEAD;
            case "POST" -> HttpMethod.POST;
            case "PUT" -> HttpMethod.PUT;
            case "PATCH" -> HttpMethod.PATCH;
            case "DELETE" -> HttpMethod.DELETE;
            default -> new HttpMethod(name);
        };
    }
}
