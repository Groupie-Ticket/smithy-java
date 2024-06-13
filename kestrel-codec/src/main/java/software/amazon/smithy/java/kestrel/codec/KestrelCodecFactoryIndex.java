/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel.codec;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KestrelCodecFactoryIndex {

    private final static Map<String, KestrelCodecFactory> codecFactories = ServiceLoader.load(
        KestrelCodecFactory.class,
        KestrelCodecFactoryIndex.class.getClassLoader()
    )
        .stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toMap(KestrelCodecFactory::serviceName, Function.identity()));


    public static KestrelCodecFactory getCodecFactory(String serviceName) {
        return codecFactories.get(serviceName);
    }
}
