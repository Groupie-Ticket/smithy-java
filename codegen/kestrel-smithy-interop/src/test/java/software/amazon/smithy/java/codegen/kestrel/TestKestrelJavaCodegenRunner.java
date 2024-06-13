/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import java.nio.file.Paths;
import java.util.Optional;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.java.codegen.server.JavaServerCodegenPlugin;
import software.amazon.smithy.kestrel.codegen.KestrelCodegenPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;

public class TestKestrelJavaCodegenRunner {


    public static void main(String[] args) {
        KestrelCodegenPlugin plugin = new KestrelCodegenPlugin();
        JavaServerCodegenPlugin serverPlugin = new JavaServerCodegenPlugin();
        Model model = Model.assembler(TestKestrelJavaCodegenRunner.class.getClassLoader())
            .discoverModels(TestKestrelJavaCodegenRunner.class.getClassLoader())
            .assemble()
            .unwrap();
        String output = Optional.ofNullable(System.getenv("output"))
            .orElse("/Volumes/workplace/smithy-java/codegen/kestrel-smithy-interop/build/generated-src");

        System.out.println("WRITING TO : " + output);
        String service = Optional.ofNullable(System.getenv("service"))
            .orElse("smithy.java.codegen.kestrel.test#TestService");
        String namespace = Optional.ofNullable(System.getenv("namespace")).orElse("smithy.java.codegen.kestrel.test");
        PluginContext context = PluginContext.builder()
            .fileManifest(FileManifest.create(Paths.get(output)))
            .settings(
                ObjectNode.builder()
                    .withMember("service", service)
                    .withMember("namespace", namespace)
                    .build()
            )
            .model(model)
            .build();
        plugin.execute(context);
        serverPlugin.execute(context);
    }
}
