/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.rpcv2.sparrowhawk;

import software.amazon.smithy.utils.Pair;

public class RpcV2PathParser {


    public static Pair<String, String> parseRpcV2Path(String path) {
        // serviceNameStart must be nonnegative for any of these offsets
        // to be considered valid
        int pos = path.length() - 1;
        int serviceNameStart = -1, serviceNameEnd;
        int operationNameStart = 0, operationNameEnd;
        int namespaceIdx = -1;
        int term = pos + 1;
        operationNameEnd = term;

        for (; pos >= 0; pos--) {
            if (path.charAt(pos) == '/') {
                operationNameStart = pos + 1;
                break;
            }
        }

        // we could do the same check above the first for loop if we wanted to
        // fail if we went all the way to the start of the path or if the first
        // character encountered is a "/" (e.g. in "/service/foo/operation/"
        if (operationNameStart == 0 || operationNameStart == term) {
            throw new IllegalStateException();
        }

        if (!findOperationPrefix(path, pos)) {
            throw new IllegalStateException();
        }

        // seek pos to the character before "/operation", pos is currently on the "n"
        serviceNameEnd = (pos -= 11) + 1;
        for (; pos >= 0; pos--) {
            int c = path.charAt(pos);
            if (c == '/') {
                serviceNameStart = pos + 1;
                break;
            } else if (c == '.' && namespaceIdx < 0) {
                namespaceIdx = pos;
            }
        }

        // still need "/service"
        // serviceNameStart < 0 means we never found a "/"
        // serviceNameStart == serviceNameEnd means we had a zero-width name, "/service//"
        if (serviceNameStart < 0 || serviceNameStart == serviceNameEnd) {
            throw new IllegalStateException();
        }

        if (!findServicePrefix(path, pos)) {
            throw new IllegalStateException();
        }

        String serviceName;

        if (namespaceIdx > 0) {
            serviceName = path.substring(namespaceIdx + 1, serviceNameEnd);
        } else {
            serviceName = path.substring(serviceNameStart, serviceNameEnd);
        }


        return Pair.of(serviceName, path.substring(operationNameStart, operationNameEnd));
    }

    private static boolean findOperationPrefix(String uri, int pos) {
        // need 10 chars: "/operation/", pos points to "/"
        // then need another 9 chars for "/service/"
        return pos >= 19 &&
            ((uri.charAt(pos - 10) == '/') &&
                (uri.charAt(pos - 9) == 'o') &&
                (uri.charAt(pos - 8) == 'p') &&
                (uri.charAt(pos - 7) == 'e') &&
                (uri.charAt(pos - 6) == 'r') &&
                (uri.charAt(pos - 5) == 'a') &&
                (uri.charAt(pos - 4) == 't') &&
                (uri.charAt(pos - 3) == 'i') &&
                (uri.charAt(pos - 2) == 'o') &&
                (uri.charAt(pos - 1) == 'n'));

    }

    private static boolean findServicePrefix(String uri, int pos) {
        // need 8 chars: "/service/", pos points to "/"
        return pos >= 8 &&
            ((uri.charAt(pos - 8) == '/') &&
                (uri.charAt(pos - 7) == 's') &&
                (uri.charAt(pos - 6) == 'e') &&
                (uri.charAt(pos - 5) == 'r') &&
                (uri.charAt(pos - 4) == 'v') &&
                (uri.charAt(pos - 3) == 'i') &&
                (uri.charAt(pos - 2) == 'c') &&
                (uri.charAt(pos - 1) == 'e'));
    }


    private RpcV2PathParser() {
    }
}
