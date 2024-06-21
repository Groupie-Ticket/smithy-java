package software.amazon.smithy.java.server.protocoltests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.transform.ModelTransformer;

final class ProtocolTestDiscovery {
    private static final ProtocolTestDiscovery INSTANCE = new ProtocolTestDiscovery();

    private final Model baseModel;
    private final ModelTransformer transformer = ModelTransformer.create();

    private ProtocolTestDiscovery() {
        baseModel = Model.assembler(EndToEndProtocolTests.class.getClassLoader())
            .discoverModels(EndToEndProtocolTests.class.getClassLoader())
            .assemble()
            .unwrap();
    }

    static ProtocolTestDiscovery get() {
        return INSTANCE;
    }

    Map<ProtocolTestService, List<OperationShape>> discoverTests(Predicate<Shape> removalPredicate) {
        var result = new HashMap<ProtocolTestService, List<OperationShape>>();

        Model filtered = transformer.removeUnreferencedShapes(transformer.removeShapesIf(baseModel, removalPredicate));
        for (ServiceShape service : filtered.getServiceShapes()) {
            Model serviceModel = applyTransformations(service, filtered);

            var operations = service.getAllOperations()
                .stream()
                .map(serviceModel::getShape)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(s -> (OperationShape) s)
                .toList();

            if (!operations.isEmpty()) {
                result.put(new ProtocolTestService(service, serviceModel), operations);
            }
        }

        return result;
    }

    private Model applyTransformations(ServiceShape service, Model filtered) {
        Model serviceModel = transformer.copyServiceErrorsToOperations(filtered, service);
        serviceModel = transformer.flattenAndRemoveMixins(serviceModel);
        serviceModel = transformer.createDedicatedInputAndOutput(serviceModel, "Input", "Output");
        return serviceModel;
    }


    record ProtocolTestService(ServiceShape service, Model serviceModel) {}


}
