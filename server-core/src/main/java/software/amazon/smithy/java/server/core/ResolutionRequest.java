/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.net.URI;
import java.net.http.HttpHeaders;
import software.amazon.smithy.java.server.core.http.HttpMethod;

public record ResolutionRequest(HttpMethod method, URI uri, HttpHeaders headers) {}
