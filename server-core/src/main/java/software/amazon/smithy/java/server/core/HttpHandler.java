/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;

public class HttpHandler implements SyncHandler {
    @Override
    public void doBefore(Job job) {

    }

    @Override
    public void doAfter(Job job) {
        Reply reply = job.reply();
        Map<String, List<String>> header = new HashMap<>();

        reply.getContext().put(HttpAttributes.HTTP_HEADERS, HttpHeaders.of(header, (k, v) -> true));
    }
}
