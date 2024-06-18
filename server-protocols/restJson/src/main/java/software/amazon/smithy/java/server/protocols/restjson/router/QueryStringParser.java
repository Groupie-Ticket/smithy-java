/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class QueryStringParser {
    private static final UrlEncoder urlEncoder = new UrlEncoder();
    private static final Pattern REGEX_STRING_OF_SLASHES = Pattern.compile("[/]+");

    public interface Visitor {
        boolean onParameter(String key, String value);
    }

    public static boolean parse(String queryString, Visitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException();
        }

        if (queryString == null) {
            return true;
        }

        int start = 0;

        for (int i = 0; i < queryString.length(); i++) {
            char c = queryString.charAt(i);

            if (c == '&' || c == ';') {
                if (!handleParam(queryString, start, i, visitor)) {
                    return false;
                }

                start = i + 1;
            }
        }

        return handleParam(queryString, start, queryString.length(), visitor);
    }

    public static Map<String, String> toMapOfStrings(String queryString) {
        final Map<String, String> map = new TreeMap<>();

        parse(queryString, (key, value) -> {
            String strKey = String.valueOf(key);
            String strValue = null;
            if (value != null) {
                strValue = value;
            }

            map.put(strKey, strValue);

            return true;
        });

        return map;
    }

    public static Map<String, List<String>> toMapOfLists(String queryString) {
        final Map<String, List<String>> map = new TreeMap<>();

        parse(queryString, (key, value) -> {
            String strValue = null;
            if (value != null) {
                strValue = value;
            }

            List<String> values = map.computeIfAbsent(key, k -> new ArrayList<>());
            values.add(strValue);

            return true;
        });

        return map;
    }

    private static boolean handleParam(String queryString, int start, int end, Visitor visitor) {
        String param = queryString.substring(start, end);

        if (param.isEmpty()) {
            return true;
        }

        String key = urlEncoder.decodeUriComponent(getKey(param));
        String value = getValue(param);
        value = urlEncoder.decodeUriComponent(value);

        return visitor.onParameter(key, value);
    }

    private static String getKey(String keyValuePair) {
        int separator = getKeyValueSeparator(keyValuePair);
        if (separator == -1) {
            return keyValuePair;
        } else {
            return keyValuePair.substring(0, separator);
        }
    }

    private static String getValue(String keyValuePair) {
        int separator = getKeyValueSeparator(keyValuePair);
        if (separator == -1) {
            return "";
        } else {
            return keyValuePair.substring(separator + 1);
        }
    }

    private static int getKeyValueSeparator(String keyValuePair) {
        for (int i = 0; i < keyValuePair.length(); i++) {
            char c = keyValuePair.charAt(i);
            if (c == '=') {
                return i;
            }
        }
        return -1;
    }

    public static String getPath(String uri) {
        return getPath(uri, false);
    }

    public static String getRawPath(String uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }

        int i = 0;
        // Remove leading slashes
        while (i < uri.length() - 1 && uri.charAt(i) == '/') {
            i++;
        }
        int j = getQuerySeparator(uri);
        if (j < 0) {
            j = uri.length();
        }
        // Remove trailing slashes
        while (j > i && uri.charAt(j - 1) == '/') {
            j--;
        }
        return uri.substring(i, j);
    }

    public static String getPath(String uri, boolean allowEmptyPathSegments) {
        uri = getRawPath(uri);
        if (allowEmptyPathSegments) {
            return uri;
        }
        return REGEX_STRING_OF_SLASHES.matcher(uri).replaceAll("/");
    }

    public static String getQuery(String uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }

        int questionMark = getQuerySeparator(uri);
        if (questionMark < 0) {
            return null;
        } else {
            return uri.substring(questionMark + 1);
        }
    }

    private static int getQuerySeparator(String uri) {
        for (int i = 0; i < uri.length(); i++) {
            if (uri.charAt(i) == '?') {
                return i;
            }
        }
        return -1;
    }
}
