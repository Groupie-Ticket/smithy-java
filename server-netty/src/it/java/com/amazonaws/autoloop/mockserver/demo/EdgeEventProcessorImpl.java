package com.amazonaws.autoloop.mockserver.demo;

import com.amazon.hyperloop.streaming.model.AttributeUpdates;
import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CloudEvent.EventAckMember;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.EdgeEventAck;
import com.amazonaws.autoloop.mockserver.processing.EdgeEventProcessor;

public final class EdgeEventProcessorImpl extends EdgeEventProcessor {

    @Override
    protected CloudEvent processAttributeUpdates(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        EdgeEventAck edgeEventAck = EdgeEventAck.builder().requestId(attributeUpdates.requestId()).build();
        return new EventAckMember(edgeEventAck);
    }
}
