package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.server.Operation;

public record ResolutionResult(Operation<?, ?> operation, ServerProtocol protocol, Context resolutionContext) {}
