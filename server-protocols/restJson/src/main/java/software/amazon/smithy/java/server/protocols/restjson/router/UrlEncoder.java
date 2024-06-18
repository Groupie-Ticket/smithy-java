/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson.router;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Pattern;

public final class UrlEncoder {

    private static final String ENCODING = "UTF-8";

    private static final Charset CHARSET = Charset.forName(ENCODING);

    private static final Pattern ENCODE_REPLACE_PATTERN = Pattern.compile("[+*]|%7E");

    public UrlEncoder() {
    }

    public String encode(CharSequence plaintext) {
        String in = plaintext.toString();
        try {
            String out = java.net.URLEncoder.encode(in, ENCODING);

            java.util.regex.Matcher matcher = ENCODE_REPLACE_PATTERN.matcher(out);
            if (matcher.find()) {
                StringBuffer result = new StringBuffer(out.length());

                do {
                    char chr = out.charAt(matcher.start());
                    String replace;
                    switch (chr) {
                        case '+':
                            replace = "%20";
                            break;
                        case '*':
                            replace = "%2A";
                            break;
                        case '%':
                            // Assume the full match is %7E as its the only match
                            // in the pattern with a %
                            replace = "~";
                            break;
                        default:
                            throw new RuntimeException(
                                "Got match that should not be possible. " +
                                    "Check that the pattern mathes the code"
                            );
                    }
                    matcher.appendReplacement(result, replace);
                } while (matcher.find());
                matcher.appendTail(result);

                out = result.toString();
            }

            return out;
        } catch (Throwable t) {
            throw new UrlEncoderException("Unable to encode URL '" + in + "':" + t.getMessage(), t);
        }
    }

    /**
     * Convert from a hex digit to its int value. This function supports base upper
     * and lower case characters. If the character is not one of [0-9a-fA-F] then
     * this function returns -1.
     */
    private static int parseHexDigit(char chr) {
        if (chr < '0') {
            return -1;
        } else if (chr <= '9') {
            return chr - '0';
        } else if (chr < 'A') {
            return -1;
        } else if (chr <= 'F') {
            return chr - 'A' + 10;
        } else if (chr < 'a') {
            return -1;
        } else if (chr <= 'f') {
            return chr - 'a' + 10;
        } else {
            return -1;
        }
    }

    /**
     * Parse a two byte hex string starting at position start. Using this instead
     * of Integer.parseInt saves having to create a string for the two characters
     * and is a little more efficient because we can assume only hex chars
     * and don't have to worry about negative values.
     */
    private static byte parseHexChar(CharSequence encoded, int start) {
        // Assume encoded is long enough
        int v1 = parseHexDigit(encoded.charAt(start));
        int v2 = parseHexDigit(encoded.charAt(start + 1));

        if (v1 == -1 || v2 == -1) {
            throw new UrlEncoderException(
                "Illegal hex character in escape % pattern: %" + encoded.subSequence(start, start + 2)
            );
        }

        return (byte) (v1 * 16 + v2);
    }


    /**
     * This method is marked deprecated to remove code that decodes + to spaces.
     * RFC3986 https://tools.ietf.org/html/rfc3986\#section-3.3 does not indicate any requirement
     * for +s in either path or query param to be decoded to spaces
     * Please use decodeUriComponent method instead.
     */
    @Deprecated
    public String decode(CharSequence encoded) {
        return decodeDefault(encoded, false);
    }

    public String decodeUriComponent(CharSequence encoded) {
        return decodeDefault(encoded, true);
    }

    private String decodeDefault(CharSequence encoded, boolean skipPlusDecoding) {
        StringBuilder builder = null;
        byte[] bytes = null;

        boolean wasChanged = false;
        int numChars = encoded.length();
        int i = 0;

        builder = new StringBuilder(numChars);

        while (i < numChars) {
            char chr = encoded.charAt(i);
            switch (chr) {
                case '+':
                    if (skipPlusDecoding) {
                        builder.append('+');
                    } else {
                        // Else block is for backwards compatibility
                        builder.append(' ');
                        wasChanged = true;
                    }
                    i++;
                    break;
                case '%':
                    /*
                     * Convert all consecutive % encoded bytes into one array and
                     * transcode that. That allows us to properly handle multibyte
                     * characters.
                     */
                    if (bytes == null) {
                        // Most of the time there aren't a lot of encoded
                        // characters in a row so allocate a small array
                        bytes = new byte[8];
                    }

                    int bytePos = 0;
                    while ((i + 2) < numChars && chr == '%') {
                        if (bytePos >= bytes.length) {
                            // If we needed more than the base size then the input is probably
                            // not using ascii and most of it will be encoded. In that case
                            // size the array to fit all of the remaining characters
                            int newSize = bytes.length + ((numChars - i) / 3);
                            bytes = Arrays.copyOf(bytes, newSize);
                        }
                        bytes[bytePos] = parseHexChar(encoded, i + 1);
                        bytePos++;
                        i += 3;

                        if (i < numChars) {
                            chr = encoded.charAt(i);
                        }
                    }

                    if (chr == '%' && i < numChars) {
                        throw new UrlEncoderException("Incomplete trailing escape % sequence");
                    }

                    // This could be done more efficiently if the string is large
                    // but that's a lot more complex than I want to handle and might
                    // make the short case worse.
                    builder.append(new String(bytes, 0, bytePos, CHARSET));
                    wasChanged = true;
                    break;
                default:
                    builder.append(chr);
                    i++;
            }
        }

        if (wasChanged) {
            return builder.toString();
        } else {
            return encoded.toString();
        }
    }

    public boolean requiresDecoding(CharSequence encoded) {
        for (int i = 0; i < encoded.length(); i++) {
            switch (encoded.charAt(i)) {
                case '%':
                case '+':
                    return true;
            }
        }
        return false;
    }
}
