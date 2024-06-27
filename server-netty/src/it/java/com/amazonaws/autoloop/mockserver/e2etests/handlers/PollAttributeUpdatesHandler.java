package com.amazonaws.autoloop.mockserver.e2etests.handlers;

import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CloudEvent.PolledAttributeUpdatesMember;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.PollAttributeUpdates;
import com.amazon.hyperloop.streaming.model.PolledAttributeUpdates;
import com.amazonaws.autoloop.mockserver.e2etests.TestUtils;
import java.util.Arrays;

public class PollAttributeUpdatesHandler {

    public CloudEvent handle(
        PollAttributeUpdates pollAttributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        switch (pollAttributeUpdates.requestId()) {
            case "pollAttributeUpdates1": {
                return pollAttributeUpdates1(pollAttributeUpdates, createAttributeSyncStreamInput);
            }
        }
        throw new UnsupportedOperationException("unhandled testcase");
    }


    private CloudEvent pollAttributeUpdates1(
        PollAttributeUpdates pollAttributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        TestUtils.assertNullOrThrow(pollAttributeUpdates.synchronizationToken(), "synchronizationToken should be null");
        PolledAttributeUpdates polledAttributeUpdates = PolledAttributeUpdates.builder()
            .attributes(Arrays.asList())
            .requestId(pollAttributeUpdates.requestId())
            .pollEnd(true)
            .build();
        return new PolledAttributeUpdatesMember(polledAttributeUpdates);
    }
}
