/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.sparrowhawk.codec;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SparrowhawkCodecFactoryIndex {

    private final static Map<String, SparrowhawkCodecFactory> codecFactories = ServiceLoader.load(
        SparrowhawkCodecFactory.class,
        SparrowhawkCodecFactoryIndex.class.getClassLoader()
    )
        .stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toMap(SparrowhawkCodecFactory::serviceName, Function.identity()));


    public static SparrowhawkCodecFactory getCodecFactory(String serviceName) {
        return codecFactories.get(serviceName);
    }
}
