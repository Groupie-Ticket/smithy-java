/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import software.amazon.smithy.codegen.core.Property;
import software.amazon.smithy.codegen.core.Symbol;

public class InteropSymbolProperties {

    private InteropSymbolProperties() {}

    public final static Property<Symbol> SMITHY_SYMBOL = Property.named("smithy-symbol");
}
