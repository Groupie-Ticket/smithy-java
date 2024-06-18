/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson.router;

/**
 * Matches a full URI by matching both the path and query components. This uses the EmptySegmentPathMatcher which does
 * allow empty path segments in the URI.
 */
class EmptySegmentUriMatcher extends UriMatcher {

    public EmptySegmentUriMatcher(PathPattern pathPattern, QueryPattern queryPattern) {
        super(new EmptySegmentPathMatcher(pathPattern), getQueryMatcher(queryPattern));
    }

    @Override
    public Match match(String uri) {
        String path = QueryStringParser.getPath(uri, true);
        String query = QueryStringParser.getQuery(uri);
        return match(path, query);
    }
}
