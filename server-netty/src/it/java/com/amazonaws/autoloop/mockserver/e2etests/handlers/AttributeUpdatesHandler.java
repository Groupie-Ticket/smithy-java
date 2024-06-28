package com.amazonaws.autoloop.mockserver.e2etests.handlers;

import com.amazon.hyperloop.streaming.model.AttributeMetadataValue;
import com.amazon.hyperloop.streaming.model.AttributeUpdates;
import com.amazon.hyperloop.streaming.model.AttributeValueUpdate;
import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CloudEvent.EventAckMember;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.EdgeEventAck;
import com.amazonaws.autoloop.mockserver.e2etests.TestUtils;
import java.util.Arrays;
import java.util.Map;

public class AttributeUpdatesHandler {

    public CloudEvent handle(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        // special case is the CreateAttributeSyncStreamTest : don't validate the Timestamp, since it is expected
        // to differ since the test lasts for multiple seconds
        if (attributeUpdates.requestId().contains("Request")) {
            return createAttributeSyncStreamTest(attributeUpdates, createAttributeSyncStreamInput);
        }

        TestUtils.validateTimestamp(attributeUpdates.timestamp());
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


    private CloudEvent createAttributeSyncStreamTest(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        return makeEdgeEventAck(attributeUpdates);
    }


    private CloudEvent attributeUpdate1(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {

        var attributes = attributeUpdates.attributes();
        var metadata = attributeUpdates.metadata();

        validateEdgeSubscriptions(attributeUpdates, "edgesubscription");

        TestUtils.assertOrThrow(0, metadata.size(), "no metadata expected");
        TestUtils.assertOrThrow(8, attributes.size(), "wrong amount of attributes received");

        TestUtils.assertOrThrow(
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.BOOLEAN_VALUE,
            attributes.get("boolean").value().type(),
            "Boolean type not received"
        );
        TestUtils.assertTrueOrThrow(attributes.get("boolean").value().booleanValue(), "Boolean value wrong");

        TestUtils.assertOrThrow(
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.LONG_VALUE,
            attributes.get("long").value().type(),
            "Long type not received"
        );
        TestUtils.assertOrThrow(1L, attributes.get("long").value().longValue(), "Long value wrong");

        TestUtils.assertOrThrow(
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.STRING_VALUE,
            attributes.get("string").value().type(),
            "String type not received"
        );
        TestUtils.assertOrThrow("StringThere", attributes.get("string").value().stringValue(), "String value wrong");

        TestUtils.assertOrThrow(
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.DOUBLE_VALUE,
            attributes.get("double").value().type(),
            "Double type not received"
        );
        TestUtils.assertOrThrow(42.0, attributes.get("double").value().doubleValue(), "Double value wrong");

        TestUtils.assertOrThrow(
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.BOOLEAN_LIST,
            attributes.get("booleanList").value().type(),
            "Boolean List type not received"
        );
        TestUtils.assertOrThrow(
            Arrays.asList(new Boolean[]{Boolean.TRUE, Boolean.FALSE}),
            attributes.get("booleanList").value().booleanList(),
            "Boolean List value wrong"
        );

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeValue.Type.LONG_LIST,
            attributes.get("longList").value().type(),
            "Long List type not received"
        );
        TestUtils.assertOrThrow(

            Arrays.asList(new Long[]{1L, 2L, 3L, 4L}),
            attributes.get("longList").value().longList(),
            "Long List value wrong"
        );

        TestUtils.assertOrThrow(
            attributes.get("stringList").value().type(),
            com.amazon.hyperloop.streaming.model.AttributeValue.Type.STRING_LIST,
            "String List type not received"
        );
        TestUtils.assertOrThrow(
            attributes.get("stringList").value().stringList(),
            Arrays.asList(new String[]{"String", "Second"}),
            "String List value wrong"
        );

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeValue.Type.DOUBLE_LIST,
            attributes.get("doubleList").value().type(),
            "Double List type not received"
        );
        TestUtils.assertOrThrow(

            Arrays.asList(new Double[]{42.0, 43.0}),
            attributes.get("doubleList").value().doubleList(),
            "Double List value wrong"
        );
        return makeEdgeEventAck(attributeUpdates);
    }

    private CloudEvent attributeUpdate2(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        var attributes = attributeUpdates.attributes();
        validateEdgeSubscriptions(attributeUpdates, "edgesubscription");

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeValue.Type.BOOLEAN_VALUE,
            attributes.get("metadata").value().type(),
            "Boolean type not received"
        );
        TestUtils.assertTrueOrThrow(attributes.get("metadata").value().booleanValue(), "Boolean value wrong");
        validateMetaData(attributes.get("metadata").metadata());
        return makeEdgeEventAck(attributeUpdates);
    }

    private CloudEvent attributeUpdate3(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        var attributes = attributeUpdates.attributes();
        var metadata = attributeUpdates.metadata();
        validateMetaData(metadata);

        validateEdgeSubscriptions(attributeUpdates, "edgesubscription");

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeValue.Type.BOOLEAN_VALUE,
            attributes.get("boolean").value().type(),
            "Boolean type not received"
        );
        TestUtils.assertTrueOrThrow(attributes.get("boolean").value().booleanValue(), "Boolean value wrong");
        return makeEdgeEventAck(attributeUpdates);
    }


    private void validateMetaData(Map<String, AttributeMetadataValue> metadata) {
        TestUtils.assertOrThrow(metadata.size(), 6, "wrong amount of metadata received");

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_VALUE,
            metadata.get("stringKey1").type(),
            "String type not received"
        );
        TestUtils.assertOrThrow("", metadata.get("stringKey1").strValue(), "String value wrong");

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_VALUE,
            metadata.get("stringKey2").type(),
            "String type not received"
        );
        TestUtils.assertOrThrow(metadata.get("stringKey2").strValue(), "any string value", "String value wrong");

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_ARRAY_VALUE,
            metadata.get("arrayKey1").type(),
            "String Array type not received"
        );
        TestUtils.assertOrThrow(metadata.get("arrayKey1").strArrayValue(), Arrays.asList(), "String Array value wrong");

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_ARRAY_VALUE,
            metadata.get("arrayKey2").type(),
            "String Array type not received"
        );
        TestUtils.assertOrThrow(
            Arrays.asList("a", "b"),
            metadata.get("arrayKey2").strArrayValue(),
            "String Array value wrong"
        );

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_ARRAY_VALUE,
            metadata.get("arrayKey3").type(),
            "String Array type not received"
        );
        TestUtils.assertOrThrow(

            Arrays.asList("a", null, "c"),
            metadata.get("arrayKey3").strArrayValue(),
            "String Array value wrong"
        );

        TestUtils.assertOrThrow(

            com.amazon.hyperloop.streaming.model.AttributeMetadataValue.Type.STR_ARRAY_VALUE,
            metadata.get("arrayKey4").type(),
            "String Array type not received"
        );
        TestUtils.assertOrThrow(
            Arrays.asList(null, null),
            metadata.get("arrayKey4").strArrayValue(),
            "String Array value wrong"
        );
    }

    private void validateEdgeSubscriptions(AttributeUpdates attributeUpdates, String exepectedEdgeSubscription) {
        for (AttributeValueUpdate attributeValueUpdate : attributeUpdates.attributes().values()) {
            TestUtils.assertOrThrow(
                exepectedEdgeSubscription,
                attributeValueUpdate.edgeSubscriptionId(),
                "wrong EdgeSubscription"
            );
        }
    }

    private EventAckMember makeEdgeEventAck(AttributeUpdates attributeUpdates) {
        EdgeEventAck edgeEventAck = EdgeEventAck.builder().requestId(attributeUpdates.requestId()).build();
        return new EventAckMember(edgeEventAck);
    }
}
