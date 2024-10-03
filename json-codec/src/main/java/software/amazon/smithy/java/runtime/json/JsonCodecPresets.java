/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.json;

import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.serde.CodecFactory;

/**
 * JSON codec preset configurations.
 */
public final class JsonCodecPresets {

    private JsonCodecPresets() {}

    /**
     * SPI implementation for the "json" codec.
     */
    public static final class Json implements CodecFactory {
        @Override
        public Codec createCodec() {
            return JsonCodec.create();
        }

        @Override
        public String getName() {
            return "json";
        }
    }

    /**
     * SPI implementation for the "json+binding" codec.
     */
    public static final class JsonBinding implements CodecFactory {
        @Override
        public Codec createCodec() {
            return JsonCodec.createWithBindings();
        }

        @Override
        public String getName() {
            return "json+binding";
        }
    }
}
