/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.sparrowhawk;

import java.nio.file.Paths;
import java.util.Optional;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.java.codegen.server.JavaServerCodegenPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.sparrowhawk.codegen.SparrowhawkCodegenPlugin;

public class TestSparrowhawkJavaCodegenRunner {


    public static void main(String[] args) {
        SparrowhawkCodegenPlugin plugin = new SparrowhawkCodegenPlugin();
        JavaServerCodegenPlugin serverPlugin = new JavaServerCodegenPlugin();
        Model model = Model.assembler(TestSparrowhawkJavaCodegenRunner.class.getClassLoader())
            .discoverModels(TestSparrowhawkJavaCodegenRunner.class.getClassLoader())
            .assemble()
            .unwrap();
        String output = Optional.ofNullable(System.getenv("output"))
            .orElse("/Volumes/workplace/smithy-java/codegen/sparrowhawk-smithy-interop/build/generated-src");

        System.out.println("WRITING TO : " + output);
        String service = Optional.ofNullable(System.getenv("service"))
            .orElse("smithy.java.codegen.sparrowhawk.test#TestService");
        String namespace = Optional.ofNullable(System.getenv("namespace"))
            .orElse("smithy.java.codegen.sparrowhawk.test");
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
