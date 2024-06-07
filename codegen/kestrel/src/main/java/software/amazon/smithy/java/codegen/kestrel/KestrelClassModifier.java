/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;

abstract class KestrelClassModifier {
    List<String> extendsClasses(Symbol symbol) {
        return List.of();
    }

    List<String> implementsClasses(Symbol symbol) {
        return List.of();
    }

    void contributeMethods(JavaWriter writer) {}
}
