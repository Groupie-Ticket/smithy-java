/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson.router;

/**
 * Matches a full URI by matching both the path and query components. This uses the BasicPathMatcher which does NOT
 * allow empty path segments in the URI.
 */
class UriMatcher implements Matcher {

    private final Matcher pathMatcher;
    private final Matcher queryMatcher;

    public UriMatcher(PathPattern pathPattern, QueryPattern queryPattern) {
        this(new BasicPathMatcher(pathPattern), getQueryMatcher(queryPattern));
    }

    UriMatcher(Matcher pathMatcher, Matcher queryMatcher) {
        if (pathMatcher == null) {
            throw new IllegalArgumentException();
        }

        this.pathMatcher = pathMatcher;
        this.queryMatcher = queryMatcher;
    }

    @Override
    public int getRank() {
        return pathMatcher.getRank() + (queryMatcher == null ? 0 : queryMatcher.getRank());
    }

    @Override
    public Match match(String uri) {
        String path = QueryStringParser.getPath(uri);
        String query = QueryStringParser.getQuery(uri);
        return match(path, query);
    }

    protected Match match(String path, String query) {
        Match pathMatch = pathMatcher.match(path);
        if (pathMatch == null) {
            return null;
        }

        if (queryMatcher == null) {
            return pathMatch;
        }

        Match queryMatch = queryMatcher.match(query);
        if (queryMatch == null) {
            return null;
        }

        return new CompositeMatch(pathMatch, queryMatch);
    }

    protected static Matcher getQueryMatcher(QueryPattern queryPattern) {
        if (queryPattern == null) {
            return null;
        } else {
            return new QueryStringMatcher(queryPattern);
        }
    }
}
