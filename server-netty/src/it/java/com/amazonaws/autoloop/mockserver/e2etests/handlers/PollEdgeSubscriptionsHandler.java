package com.amazonaws.autoloop.mockserver.e2etests.handlers;

import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CloudEvent.EdgeSubscriptionChangesMember;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.EdgeSubscriptionAttribute;
import com.amazon.hyperloop.streaming.model.EdgeSubscriptionChange;
import com.amazon.hyperloop.streaming.model.EdgeSubscriptionChangeType;
import com.amazon.hyperloop.streaming.model.EdgeSubscriptionChanges;
import com.amazon.hyperloop.streaming.model.PollEdgeSubscriptions;
import com.amazonaws.autoloop.mockserver.e2etests.TestUtils;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PollEdgeSubscriptionsHandler {

    public CloudEvent handle(
        PollEdgeSubscriptions pollEdgeSubscriptions,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        switch (pollEdgeSubscriptions.requestId()) {
            case "pollEdgesubscription1": {
                return pollEdgesubscription1(pollEdgeSubscriptions, createAttributeSyncStreamInput);
            }
            case "pollEdgesubscription2": {
                return pollEdgesubscription2(pollEdgeSubscriptions, createAttributeSyncStreamInput);
            }
            case "pollEdgesubscription3": {
                return pollEdgesubscription3(pollEdgeSubscriptions, createAttributeSyncStreamInput);
            }
        }
        throw new UnsupportedOperationException("unhandled testcase");
    }

    private CloudEvent pollEdgesubscription1(
        PollEdgeSubscriptions pollEdgeSubscriptions,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        TestUtils.assertOrThrow(

            Arrays.asList(),
            pollEdgeSubscriptions.edgeSubscriptionsSummary(),
            "edgeSubscriptionsSummary should be empty"
        );
        TestUtils.assertOrThrow(
            Boolean.FALSE,
            pollEdgeSubscriptions.hasEdgeSubscriptionsSummary(),
            "wrong value for hasEdgeSubscriptionsSummary received"
        );
        EdgeSubscriptionChanges edgeSubscriptionChanges = EdgeSubscriptionChanges.builder()
            .requestId(pollEdgeSubscriptions.requestId())
            .changes(Arrays.asList())
            .pollEnd(true)
            .build();
        return new EdgeSubscriptionChangesMember(edgeSubscriptionChanges);
    }


    private CloudEvent pollEdgesubscription2(
        PollEdgeSubscriptions pollEdgeSubscriptions,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        TestUtils.assertTrueOrThrow(
            pollEdgeSubscriptions.hasEdgeSubscriptionsSummary(),
            "wrong value for hasEdgeSubscriptionsSummary received"
        );
        TestUtils.assertOrThrow(

            101,
            pollEdgeSubscriptions.edgeSubscriptionsSummary().size(),
            "wrong amount of entries received for edgeSubscriptionsSummary"
        );
        TestUtils.assertOrThrow(

            "EdgeSubscription0",
            pollEdgeSubscriptions.edgeSubscriptionsSummary().get(0).edgeSubscriptionId(),
            "wrong value for edgeSubscriptionId"
        );
        TestUtils.assertOrThrow(

            "EdgeSubscription100",
            pollEdgeSubscriptions.edgeSubscriptionsSummary().get(100).edgeSubscriptionId(),
            "wrong value for edgeSubscriptionId"
        );
        TestUtils.assertOrThrow(

            "10",
            pollEdgeSubscriptions.edgeSubscriptionsSummary().get(0).revision(),
            "wrong value for revision"
        );
        TestUtils.assertOrThrow(

            "10",
            pollEdgeSubscriptions.edgeSubscriptionsSummary().get(100).revision(),
            "wrong value for revision"
        );

        List<EdgeSubscriptionChange> changes = IntStream.range(0, 100)
            .mapToObj(
                i -> EdgeSubscriptionChange.builder()
                    .change(EdgeSubscriptionChangeType.CREATE)
                    .attributes(Arrays.asList())
                    .revision("123")
                    .edgeSubscriptionId("esarn")
                    .build()
            )
            .collect(Collectors.toList());
        EdgeSubscriptionChanges edgeSubscriptionChanges = EdgeSubscriptionChanges.builder()
            .requestId(pollEdgeSubscriptions.requestId())
            .changes(changes)
            .pollEnd(false)
            .build();
        return new EdgeSubscriptionChangesMember(edgeSubscriptionChanges);
    }

    private CloudEvent pollEdgesubscription3(
        PollEdgeSubscriptions pollEdgeSubscriptions,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        TestUtils.assertFalseOrThrow(
            pollEdgeSubscriptions.hasEdgeSubscriptionsSummary(),
            "wrong value for hasEdgeSubscriptionsSummary received"
        );
        TestUtils.assertOrThrow(

            0,
            pollEdgeSubscriptions.edgeSubscriptionsSummary().size(),
            "wrong amount of entries received for edgeSubscriptionsSummary"
        );


        List<EdgeSubscriptionChange> changes = Arrays.asList(
            EdgeSubscriptionChange.builder()
                .change(EdgeSubscriptionChangeType.CREATE)
                .attributes(
                    Arrays.asList(
                        EdgeSubscriptionAttribute.builder()
                            .attributeName("attribute")
                            .maxRateInMs(1000)
                            .consistencyGroup("testGroup")
                            .build()
                    )
                )
                .revision("123")
                .edgeSubscriptionId("edgesubscription")
                .build()
        );
        EdgeSubscriptionChanges edgeSubscriptionChanges = EdgeSubscriptionChanges.builder()
            .requestId(pollEdgeSubscriptions.requestId())
            .changes(changes)
            .pollEnd(true)
            .build();
        return new EdgeSubscriptionChangesMember(edgeSubscriptionChanges);
    }
}
