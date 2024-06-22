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
import software.amazon.smithy.kestrel.codegen.KestrelCodegenPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.ObjectNode;


public final class CodegenRunner {
    private CodegenRunner() {
        // Utility class does not have constructor
    }

    public static void main(String[] args) {
        JavaServerCodegenPlugin plugin = new JavaServerCodegenPlugin();
        KestrelCodegenPlugin kestrelPlugin = new KestrelCodegenPlugin();
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
                        .withMember("useInstantForTimestamp", true)
                        .build()
                )
                .model(model)
                .build();
            plugin.execute(context);
            kestrelPlugin.execute(context);
        }
    }
}
