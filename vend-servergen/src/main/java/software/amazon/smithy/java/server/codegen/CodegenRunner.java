/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.codegen;

import java.nio.file.Path;
import java.nio.file.Paths;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.java.codegen.server.JavaServerCodegenPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.sparrowhawk.codegen.SparrowhawkCodegenPlugin;


public final class CodegenRunner {
    private CodegenRunner() {
        // Utility class does not have constructor
    }

    public static void main(String[] args) {
        JavaServerCodegenPlugin plugin = new JavaServerCodegenPlugin();
        SparrowhawkCodegenPlugin sparrowhawkPlugin = new SparrowhawkCodegenPlugin();
        Model model = new ModelAssembler()
            .discoverModels()
            .addImport(args[0])
            .assemble()
            .unwrap();

        Path output = Paths.get(args[2]);
        System.out.println("WRITING TO : " + output);
        String serviceEnv = args[1];
        String[] services = serviceEnv.split(",");
        for (String service : services) {
            PluginContext context = PluginContext.builder()
                .fileManifest(FileManifest.create(output))
                .settings(
                    ObjectNode.builder()
                        .withMember("service", service)
                        .withMember("namespace", service.split("#")[0])
                        .withMember("headerString", """
                            /*\s
                             * Copyright 2024 Amazon.com, Inc. or its affiliates. All rights reserved.\s
                             *\s
                             * AMAZON PROPRIETARY/CONFIDENTIAL\s
                             */
                            """)
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
