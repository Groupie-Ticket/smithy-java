/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

import java.util.ServiceLoader;
import java.util.StringJoiner;

/**
 * Used to find codec implementations for a given name.
 */
public interface CodecFactory {
    /**
     * Returns the name of the codec.
     *
     * @return codec name.
     */
    String getName();

    /**
     * Creates a codec for this name.
     *
     * @return the created codec.
     */
    Codec createCodec();

    /**
     * Attempt to find a codec registered with the given name.
     *
     * @param name Name of the codec to resolve.
     * @return the created codec.
     * @throws UnsupportedOperationException if the codec cannot be found.
     */
    static Codec resolve(String name) {
        var loader = ServiceLoader.load(CodecFactory.class);
        for (var codec : loader) {
            if (codec.getName().equals(name)) {
                return codec.createCodec();
            }
        }

        StringJoiner names = new StringJoiner(", ");
        for (var codec : loader) {
            names.add(codec.getName());
        }
        throw new UnsupportedOperationException("Codec not found for '" + name + "'. Available codecs: " + names);
    }
}
