/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core.attributes;

import java.net.URI;
import java.net.http.HttpHeaders;
import software.amazon.smithy.java.runtime.core.Context;

public final class HttpAttributes {

    private HttpAttributes() {
    }

    public static final Context.Key<HttpHeaders> HTTP_HEADERS = Context.key("http-headers");

    public static final Context.Key<URI> HTTP_URI = Context.key("http-uri");
}
