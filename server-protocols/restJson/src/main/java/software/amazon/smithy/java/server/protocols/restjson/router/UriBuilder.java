/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.java.server.protocols.restjson.router.PathPattern.Segment;

class UriBuilder {
    private static final UrlEncoder urlEncoder = new UrlEncoder();

    private final PathPattern pathPattern;
    private final QueryPattern queryPattern;
    private final LabelValues labelValues = new LabelValues();
    private final Map<String, List<String>> unlabelledMap = new HashMap<String, List<String>>();

    UriBuilder(PathPattern pathPattern, QueryPattern queryPattern) {
        if (pathPattern == null) {
            throw new IllegalArgumentException();
        }

        this.pathPattern = pathPattern;
        this.queryPattern = queryPattern;
    }

    public void addQueryMapValue(String key, String value) {
        unlabelledMap.computeIfAbsent(key, k -> new ArrayList<String>()).add(value);
    }

    public UriBuilder addLabel(String name, String value) {
        Segment segment = pathPattern.getSegmentForLabel(name);
        if (segment != null) {
            if (!segment.isGreedyLabel()) {
                // if it's a path label, make sure it has at most one value
                if (labelValues.getLabelValues(name) != null) {
                    throw new IllegalArgumentException(
                        "Labels occurring in the path pattern may have at most one value"
                    );
                }
            }
            labelValues.addUriPathLabelValue(name, value);
        } else if (queryPattern == null || queryPattern.getKeyForLabel(name) == null) {
            // the label isn't defined anywhere in either the path or query patterns
            throw new IllegalArgumentException("Label " + name + " is not defined for this uri pattern");
        } else {
            labelValues.addQueryParamLabelValue(name, value);
        }

        return this;
    }

    public CharSequence newUri() {
        StringBuilder sb = new StringBuilder();

        appendPath(sb);
        appendQuery(sb);

        return sb;
    }

    private void appendPath(StringBuilder sb) {
        boolean isFirst = true;
        for (Segment s : pathPattern.getSegments()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append('/');
            }

            if (s.isLabel()) {
                List<String> values = labelValues.getLabelValues(String.valueOf(s.getContent()));
                // TODO: switch over to tring in PathPattern to eliminate the conversion here?
                if (values == null) {
                    throw new IllegalStateException("No value found for label " + s.getContent());
                }

                for (Iterator<String> i = values.iterator(); i.hasNext();) {
                    sb.append(urlEncoder.encode(i.next()));
                    if (i.hasNext()) {
                        sb.append('/');
                    }
                }
            } else {
                sb.append(s.getContent());
            }
        }
    }

    private void appendQuery(StringBuilder sb) {
        if (queryPattern == null && unlabelledMap.isEmpty()) {
            return;
        }

        sb.append("?");

        QueryStringBuilder qsb = new QueryStringBuilder();

        if (queryPattern != null) {
            // add required fields
            for (String key : queryPattern.getRequiredLiteralKeys()) {
                qsb.addKeyValue(key, queryPattern.getRequiredLiteralValue(key));
            }

            // add labelled fields
            for (String label : queryPattern.getLabels()) {
                List<String> values = labelValues.getLabelValues(label);
                if (values == null) {
                    continue;
                }

                CharSequence key = queryPattern.getKeyForLabel(label);
                for (CharSequence value : values) {
                    qsb.addKeyValue(key, value);
                }
            }
        }

        // add unlabelled fields
        qsb.addKeyValuesMap(unlabelledMap);

        sb.append(qsb.getQueryString());
    }
}
