/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson.router;

import java.util.Collections;
import java.util.HashSet;
import software.amazon.smithy.model.pattern.InvalidPatternException;

public final class UriPattern {

    private final PathPattern pathPattern;
    private final QueryPattern queryPattern;
    private final Matcher uriMatcher;
    private final boolean allowEmptyPathSegments;

    public UriPattern(String pattern) {
        this(pattern, false);
    }

    public UriPattern(String pattern, boolean allowEmptyPathSegments) {
        this(
            new PathPattern(QueryStringParser.getPath(pattern)),
            getQueryPattern(QueryStringParser.getQuery(pattern)),
            allowEmptyPathSegments
        );
    }

    UriPattern(String pattern, boolean allowEmptyPathSegments, boolean checkForLabelsAfterGreedyLabels) {
        this(
            new PathPattern(QueryStringParser.getPath(pattern), checkForLabelsAfterGreedyLabels),
            getQueryPattern(QueryStringParser.getQuery(pattern)),
            allowEmptyPathSegments
        );
    }

    UriPattern(PathPattern pathPattern, QueryPattern queryPattern) {
        this(pathPattern, queryPattern, false);
    }

    UriPattern(PathPattern pathPattern, QueryPattern queryPattern, boolean allowEmptyPathSegments) {
        if (pathPattern == null) {
            throw new IllegalArgumentException();
        }

        this.pathPattern = pathPattern;
        this.queryPattern = queryPattern;

        validatePatterns();

        this.allowEmptyPathSegments = allowEmptyPathSegments;
        if (allowEmptyPathSegments) {
            this.uriMatcher = new EmptySegmentUriMatcher(pathPattern, queryPattern);
        } else {
            this.uriMatcher = new UriMatcher(pathPattern, queryPattern);
        }
    }

    public static UriPattern forSpecificityRouting(String pattern) {
        return new UriPattern(pattern, false, false);
    }

    public static UriPattern forSpecificityRouting(String pattern, boolean allowEmptyPathSegments) {
        return new UriPattern(pattern, allowEmptyPathSegments, false);
    }

    public boolean getAllowEmptyPathSegments() {
        return allowEmptyPathSegments;
    }

    public Matcher newMatcher() {
        return uriMatcher; // reusable
    }

    public PathPattern getPathPattern() {
        return pathPattern;
    }

    public QueryPattern getQueryPattern() {
        return queryPattern;
    }

    public Iterable<String> getLabels() {
        if (queryPattern == null) {
            return pathPattern.getLabels();
        }

        HashSet<String> labels = new HashSet<>();
        for (String label : pathPattern.getLabels()) {
            labels.add(label);
        }
        for (String label : queryPattern.getLabels()) {
            labels.add(label);
        }
        return labels;
    }

    public UriBuilder newBuilder() {
        return new UriBuilder(pathPattern, queryPattern);
    }

    private void validatePatterns() {
        // check patterns don't both contain the same labels
        if (queryPattern != null) {
            for (String label : queryPattern.getLabels()) {
                if (pathPattern.getSegmentForLabel(label) != null) {
                    throw new InvalidPatternException("Path and query pattern both contain the label: " + label);
                }
            }
        }
    }

    private static QueryPattern getQueryPattern(String pattern) {
        if (pattern == null) {
            return null;
        } else {
            return new QueryPattern(pattern);
        }
    }

    public ConflictType conflictType(UriPattern uriPattern) {
        ConflictType pathConflictType = pathPattern.conflictType(uriPattern.pathPattern);
        if (pathConflictType == ConflictType.NONE) {
            return ConflictType.NONE;
        }

        if (queryPattern == null && uriPattern.queryPattern == null) {
            return pathConflictType;
        }

        if (pathConflictType == ConflictType.EQUIVALENT_CONFLICT) {
            if (queryPattern == null) {
                if (uriPattern.queryPattern.hasRequiredLiteralQueryParams()) {
                    return ConflictType.NONE;
                } else {
                    return ConflictType.EQUIVALENT_CONFLICT;
                }
            } else if (uriPattern.queryPattern == null) {
                if (queryPattern.hasRequiredLiteralQueryParams()) {
                    return ConflictType.NONE;
                } else {
                    return ConflictType.EQUIVALENT_CONFLICT;
                }
            } else if (queryPattern.conflictsWith(uriPattern.queryPattern)) {
                return ConflictType.EQUIVALENT_CONFLICT;
            }
        }

        return ConflictType.NONE;
    }

    public boolean conflictsWith(UriPattern uriPattern) {
        return conflictType(uriPattern) != ConflictType.NONE;
    }

    public Iterable<String> getPathPatternLabels() {
        return pathPattern.getLabels();
    }

    public Iterable<String> getQueryPatternLabels() {
        if (queryPattern != null) {
            return queryPattern.getLabels();
        }

        return Collections.<String>emptyList();
    }

    public enum ConflictType {
        EQUIVALENT_CONFLICT,
        NONE
    }
}
