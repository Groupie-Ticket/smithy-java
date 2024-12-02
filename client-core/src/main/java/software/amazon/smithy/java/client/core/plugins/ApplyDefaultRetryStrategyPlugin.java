/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.core.plugins;

import software.amazon.smithy.java.client.core.ClientConfig;
import software.amazon.smithy.java.client.core.ClientPlugin;
import software.amazon.smithy.java.logging.InternalLogger;
import software.amazon.smithy.java.retries.api.RetryStrategy;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Uses the AWS SDK default retry strategy if no strategy is specified and it can be found on the classpath.
 *
 * <p>Otherwise defaults to no retries.
 *
 * <p>This plugin is applied by default by {@link DefaultPlugin}.
 */
@SmithyInternalApi
public final class ApplyDefaultRetryStrategyPlugin implements ClientPlugin {

    public static final ApplyDefaultRetryStrategyPlugin INSTANCE = new ApplyDefaultRetryStrategyPlugin();
    private static final InternalLogger LOGGER = InternalLogger.getLogger(ApplyDefaultRetryStrategyPlugin.class);

    @Override
    public void configureClient(ClientConfig.Builder config) {
        if (config.retryStrategy() == null) {
            applyDefault(config);
        }
    }

    private void applyDefault(ClientConfig.Builder config) {
        try {
            var standardRetryStrategy = Class.forName("software.amazon.awssdk.retries.StandardRetryStrategy");
            var sdkRetryStrategy = Class.forName("software.amazon.smithy.java.retries.sdkadapter.SdkRetryStrategy");
            var builder = standardRetryStrategy.getMethod("builder").invoke(null);
            var strategy = builder.getClass().getMethod("build").invoke(builder);
            var retryStrategyClass = Class.forName("software.amazon.awssdk.retries.api.RetryStrategy");
            var adapted = sdkRetryStrategy.getMethod("of", retryStrategyClass).invoke(null, strategy);
            config.retryStrategy((RetryStrategy) adapted);
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("AWS SDK not on classpath, falling back to no retries");
            config.retryStrategy(RetryStrategy.noRetries());
        }
    }
}
