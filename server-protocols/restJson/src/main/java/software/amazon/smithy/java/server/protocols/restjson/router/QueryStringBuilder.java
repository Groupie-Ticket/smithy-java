/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson.router;

import java.util.Map;

final class QueryStringBuilder {

    private static final UrlEncoder urlEncoder = new UrlEncoder();

    private final StringBuilder sb = new StringBuilder();
    private final char delimiter;
    private boolean isFirst = true;

    public QueryStringBuilder() {
        this('&');
    }

    public QueryStringBuilder(char delimiter) {
        this.delimiter = delimiter;
    }

    public void addKeyValueMap(Map<? extends CharSequence, ? extends CharSequence> map) {
        for (Map.Entry<? extends CharSequence, ? extends CharSequence> entry : map.entrySet()) {
            addKeyValue(entry.getKey(), entry.getValue());
        }
    }

    public void addKeyValuesMap(Map<? extends CharSequence, ? extends Iterable<? extends CharSequence>> map) {
        for (Map.Entry<? extends CharSequence, ? extends Iterable<? extends CharSequence>> entry : map.entrySet()) {
            addKeyValues(entry.getKey(), entry.getValue());
        }
    }

    public void addKeyValues(CharSequence key, Iterable<? extends CharSequence> values) {
        for (CharSequence value : values) {
            addKeyValue(key, value);
        }
    }

    public void addKey(CharSequence key) {
        addKeyValue(key, null);
    }

    public void addKeyValue(CharSequence key, CharSequence value) {
        if (isFirst) {
            isFirst = false;
        } else {
            sb.append(delimiter);
        }

        sb.append(key);

        if (value != null) {
            sb.append('=').append(urlEncoder.encode(value));
        }
    }

    public CharSequence getQueryString() {
        return sb;
    }
}
