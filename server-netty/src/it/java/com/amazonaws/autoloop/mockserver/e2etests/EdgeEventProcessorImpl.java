package com.amazonaws.autoloop.mockserver.e2etests;

import com.amazon.hyperloop.streaming.model.AttributeMetadataValue;
import com.amazon.hyperloop.streaming.model.AttributeUpdates;
import com.amazon.hyperloop.streaming.model.AttributeValueUpdate;
import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CloudEvent.EdgeSubscriptionChangesMember;
import com.amazon.hyperloop.streaming.model.CloudEvent.EventAckMember;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.EdgeEventAck;
import com.amazon.hyperloop.streaming.model.EdgeSubscriptionAttribute;
import com.amazon.hyperloop.streaming.model.EdgeSubscriptionChange;
import com.amazon.hyperloop.streaming.model.EdgeSubscriptionChangeType;
import com.amazon.hyperloop.streaming.model.EdgeSubscriptionChanges;
import com.amazon.hyperloop.streaming.model.PollEdgeSubscriptions;
import com.amazonaws.autoloop.mockserver.processing.EdgeEventProcessor;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.*;

public final class EdgeEventProcessorImpl extends EdgeEventProcessor {

    @Override
    protected CloudEvent processAttributeUpdates(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        validateTimestamp(attributeUpdates.timestamp());
        switch (attributeUpdates.requestId()) {
            case "attributeUpdate1": {
                return attributeUpdate1(attributeUpdates, createAttributeSyncStreamInput);
            }
            case "attributeUpdate2": {
                return attributeUpdate2(attributeUpdates, createAttributeSyncStreamInput);
            }
            case "attributeUpdate3": {
                return attributeUpdate3(attributeUpdates, createAttributeSyncStreamInput);
            }
        }
        throw new UnsupportedOperationException("unhandled testcase");
    }

    protected CloudEvent processPollEdgeSubscriptions(
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
        assertOrThrow(
            pollEdgeSubscriptions.edgeSubscriptionsSummary(),
            Arrays.asList(),
            "edgeSubscriptionsSummary should be empty"
        );
        assertOrThrow(
            pollEdgeSubscriptions.hasEdgeSubscriptionsSummary(),
            Boolean.FALSE,
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
        assertOrThrow(
            pollEdgeSubscriptions.hasEdgeSubscriptionsSummary(),
            Boolean.TRUE,
            "wrong value for hasEdgeSubscriptionsSummary received"
        );
        assertOrThrow(
            pollEdgeSubscriptions.edgeSubscriptionsSummary().size(),
            101,
            "wrong amount of entries received for edgeSubscriptionsSummary"
        );
        assertOrThrow(
            pollEdgeSubscriptions.edgeSubscriptionsSummary().get(0).edgeSubscriptionId(),
            "EdgeSubscription0",
            "wrong value for edgeSubscriptionId"
        );
        assertOrThrow(
            pollEdgeSubscriptions.edgeSubscriptionsSummary().get(100).edgeSubscriptionId(),
            "EdgeSubscription100",
            "wrong value for edgeSubscriptionId"
        );
        assertOrThrow(
            pollEdgeSubscriptions.edgeSubscriptionsSummary().get(0).revision(),
            "10",
            "wrong value for revision"
        );
        assertOrThrow(
            pollEdgeSubscriptions.edgeSubscriptionsSummary().get(100).revision(),
            "10",
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
        assertOrThrow(
            pollEdgeSubscriptions.hasEdgeSubscriptionsSummary(),
            Boolean.FALSE,
            "wrong value for hasEdgeSubscriptionsSummary received"
        );
        assertOrThrow(
            pollEdgeSubscriptions.edgeSubscriptionsSummary().size(),
            0,
            "wrong amount of entries received for edgeSubscriptionsSummary"
        );


        List<EdgeSubscriptionChange> changes = Arrays.asList( EdgeSubscriptionChange.builder()
                .change(EdgeSubscriptionChangeType.CREATE)
                .attributes(Arrays.asList(
                    EdgeSubscriptionAttribute.builder().attributeName("attribute").maxRateInMs(1000).consistencyGroup("testGroup").build()
                ))
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


    private CloudEvent attributeUpdate1(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {

        var attributes = attributeUpdates.attributes();
        var metadata = attributeUpdates.metadata();

        validateEdgeSubscriptions(attributeUpdates, "edgesubscription");

        assertOrThrow(metadata.size(), 0, "no metadata expected");
        assertOrThrow(attributes.size(), 8, "wrong amount of attributes received");

        assertOrThrow(
            attributes.get("boolean").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.BOOLEAN_VALUE,
            "Boolean type not received"
        );
        assertOrThrow(attributes.get("boolean").value().booleanValue(), true, "Boolean value wrong");

        assertOrThrow(
            attributes.get("long").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.LONG_VALUE,
            "Long type not received"
        );
        assertOrThrow(attributes.get("long").value().longValue(), 1L, "Long value wrong");

        assertOrThrow(
            attributes.get("string").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.STRING_VALUE,
            "String type not received"
        );
        assertOrThrow(attributes.get("string").value().stringValue(), "StringThere", "String value wrong");

        assertOrThrow(
            attributes.get("double").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.DOUBLE_VALUE,
            "Double type not received"
        );
        assertOrThrow(attributes.get("double").value().doubleValue(), 42.0, "Double value wrong");

        assertOrThrow(
            attributes.get("booleanList").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.BOOLEAN_LIST,
            "Boolean List type not received"
        );
        assertOrThrow(
            attributes.get("booleanList").value().booleanList(),
            Arrays.asList(new Boolean[]{Boolean.TRUE, Boolean.FALSE}),
            "Boolean List value wrong"
        );

        assertOrThrow(
            attributes.get("longList").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.LONG_LIST,
            "Long List type not received"
        );
        assertOrThrow(
            attributes.get("longList").value().longList(),
            Arrays.asList(new Long[]{1L, 2L, 3L, 4L}),
            "Long List value wrong"
        );

        assertOrThrow(
            attributes.get("stringList").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.STRING_LIST,
            "String List type not received"
        );
        assertOrThrow(
            attributes.get("stringList").value().stringList(),
            Arrays.asList(new String[]{"String", "Second"}),
            "String List value wrong"
        );

        assertOrThrow(
            attributes.get("doubleList").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.DOUBLE_LIST,
            "Double List type not received"
        );
        assertOrThrow(
            attributes.get("doubleList").value().doubleList(),
            Arrays.asList(new Double[]{42.0, 43.0}),
            "Double List value wrong"
        );
        return getEdgeEventAck(attributeUpdates);
    }

    private CloudEvent attributeUpdate2(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        var attributes = attributeUpdates.attributes();
        validateEdgeSubscriptions(attributeUpdates, "edgesubscription");

        assertOrThrow(
            attributes.get("metadata").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.BOOLEAN_VALUE,
            "Boolean type not received"
        );
        assertOrThrow(attributes.get("metadata").value().booleanValue(), true, "Boolean value wrong");
        validateMetaData(attributes.get("metadata").metadata());
        return getEdgeEventAck(attributeUpdates);
    }

    private CloudEvent attributeUpdate3(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        var attributes = attributeUpdates.attributes();
        var metadata = attributeUpdates.metadata();
        validateMetaData(metadata);

        validateEdgeSubscriptions(attributeUpdates, "edgesubscription");

        assertOrThrow(
            attributes.get("boolean").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.BOOLEAN_VALUE,
            "Boolean type not received"
        );
        assertOrThrow(attributes.get("boolean").value().booleanValue(), true, "Boolean value wrong");
        return getEdgeEventAck(attributeUpdates);
    }


    private void validateMetaData(Map<String, AttributeMetadataValue> metadata) {
        assertOrThrow(metadata.size(), 6, "wrong amount of metadata received");

        assertOrThrow(
            metadata.get("stringKey1").type(),
            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_VALUE,
            "String type not received"
        );
        assertOrThrow(metadata.get("stringKey1").strValue(), "", "String value wrong");

        assertOrThrow(
            metadata.get("stringKey2").type(),
            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_VALUE,
            "String type not received"
        );
        assertOrThrow(metadata.get("stringKey2").strValue(), "any string value", "String value wrong");

        assertOrThrow(
            metadata.get("arrayKey1").type(),
            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_ARRAY_VALUE,
            "String Array type not received"
        );
        assertOrThrow(metadata.get("arrayKey1").strArrayValue(), Arrays.asList(), "String Array value wrong");

        assertOrThrow(
            metadata.get("arrayKey2").type(),
            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_ARRAY_VALUE,
            "String Array type not received"
        );
        assertOrThrow(metadata.get("arrayKey2").strArrayValue(), Arrays.asList("a", "b"), "String Array value wrong");

        assertOrThrow(
            metadata.get("arrayKey3").type(),
            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_ARRAY_VALUE,
            "String Array type not received"
        );
        assertOrThrow(
            metadata.get("arrayKey3").strArrayValue(),
            Arrays.asList("a", null, "c"),
            "String Array value wrong"
        );

        assertOrThrow(
            metadata.get("arrayKey4").type(),
            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_ARRAY_VALUE,
            "String Array type not received"
        );
        assertOrThrow(metadata.get("arrayKey4").strArrayValue(), Arrays.asList(null, null), "String Array value wrong");
    }

    private void validateEdgeSubscriptions(AttributeUpdates attributeUpdates, String exepectedEdgeSubscription) {
        for (AttributeValueUpdate attributeValueUpdate : attributeUpdates.attributes().values()) {
            assertOrThrow(
                attributeValueUpdate.edgeSubscriptionId(),
                exepectedEdgeSubscription,
                "wrong EdgeSubscription"
            );
        }
    }

    private EventAckMember getEdgeEventAck(AttributeUpdates attributeUpdates) {
        EdgeEventAck edgeEventAck = EdgeEventAck.builder().requestId(attributeUpdates.requestId()).build();
        return new EventAckMember(edgeEventAck);
    }

    void validateTimestamp(Instant timestamp) {
        assertTrueOrThrow(
            timestamp.toEpochMilli() < 1000 + System.currentTimeMillis() && timestamp.toEpochMilli() > -1000 + System
                .currentTimeMillis(),
            "timestamp implausible"
        );
    }

    <T> void assertTrueOrThrow(boolean expression, String message) {
        if (!expression) {
            throw new WrongResultException(message);
        }
    }

    <T> void assertOrThrow(T expected, T actual, String message) {
        if (!actual.equals(expected)) {
            throw new WrongResultException(message);
        }
    }
}
