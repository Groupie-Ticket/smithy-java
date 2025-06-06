/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.waiters.models;

import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

public final class WaiterApiService implements ApiService {
    @Override
    public Schema schema() {
        return Schema.createService(ShapeId.from("smithy.example#Waiter"));
    }
}
