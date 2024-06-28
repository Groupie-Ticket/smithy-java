/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import java.nio.file.Paths;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.java.codegen.server.JavaServerCodegenPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.sparrowhawk.codegen.SparrowhawkCodegenPlugin;


/**
 * Simple wrapper class used to execute the test Java codegen plugin for integration tests.
 */
public final class TestServerJavaCodegenRunner {
    private TestServerJavaCodegenRunner() {
        // Utility class does not have constructor
    }

    public static void main(String[] args) {
        JavaServerCodegenPlugin plugin = new JavaServerCodegenPlugin();
        SparrowhawkCodegenPlugin sparrowhawkPlugin = new SparrowhawkCodegenPlugin();
        Model model = Model.assembler(TestServerJavaCodegenRunner.class.getClassLoader())
            .discoverModels(TestServerJavaCodegenRunner.class.getClassLoader())
            .assemble()
            .unwrap();

        System.out.println("WRITING TO : " + System.getenv("output"));
        String serviceEnv = System.getenv("service");
        String[] services = serviceEnv.split(",");
        for (String service : services) {
            PluginContext context = PluginContext.builder()
                .fileManifest(FileManifest.create(Paths.get(System.getenv("output"))))
                .settings(
                    ObjectNode.builder()
                        .withMember("service", service)
                        .withMember("namespace", service.split("#")[0])
                        .withMember("useInstantForTimestamp", true)
                        .build()
                )
                .model(model)
                .build();
            plugin.execute(context);
            sparrowhawkPlugin.execute(context);
        }
    }
}
