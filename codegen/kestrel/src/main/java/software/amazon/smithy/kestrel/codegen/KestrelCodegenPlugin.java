/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.kestrel.codegen;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.codegen.core.directed.CodegenDirector;

public class KestrelCodegenPlugin implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "kestrel-codegen";
    }

    @Override
    public void execute(PluginContext context) {
        var director = new CodegenDirector<JavaWriter, KestrelIntegration, GenerationContext, KestrelSettings>();

        var settings = KestrelSettings.from(context.getSettings());
        director.settings(settings);
        director.directedCodegen(new DirectedKestrelCodegen());
        director.fileManifest(context.getFileManifest());
        director.service(settings.getService());
        director.model(context.getModel());
        director.integrationClass(KestrelIntegration.class);
        director.performDefaultCodegenTransforms();
        director.createDedicatedInputsAndOutputs();
        director.run();

    }
}
