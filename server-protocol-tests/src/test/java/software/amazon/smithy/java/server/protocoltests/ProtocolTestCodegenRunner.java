/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocoltests;

import java.nio.file.Paths;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.java.codegen.server.JavaServerCodegenPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

import java.util.Collections;

public final class ProtocolTestCodegenRunner {

    public static void main(String[] args) {
        JavaServerCodegenPlugin plugin = new JavaServerCodegenPlugin();
        Model model = Model.assembler(ProtocolTestCodegenRunner.class.getClassLoader())
            .discoverModels(ProtocolTestCodegenRunner.class.getClassLoader())
            .assemble()
            .unwrap();

        ModelTransformer transformer = ModelTransformer.create();

        Model filtered = transformer.removeUnreferencedShapes(
            transformer.removeShapes(
                model,
                Collections.singleton(model.expectShape(ShapeId.from("aws.protocoltests.restjson#RecursiveShapes")))
            )
        );
        System.out.println("WRITING TO : " + System.getenv("output"));
        filtered.getServiceShapes()
            .stream()
            .filter(s -> s.getId().getNamespace().equals("aws.protocoltests.restjson"))
            .forEach(serviceShape -> {
                PluginContext context = PluginContext.builder()
                    .fileManifest(FileManifest.create(Paths.get(System.getenv("output"))))
                    .settings(
                        ObjectNode.builder()
                            .withMember("service", serviceShape.getId().toString())
                            .withMember("namespace", serviceShape.getId().getNamespace())
                            .build()
                    )
                    .model(filtered)
                    .build();
                plugin.execute(context);
            });

    }

    private ProtocolTestCodegenRunner() {}
}
