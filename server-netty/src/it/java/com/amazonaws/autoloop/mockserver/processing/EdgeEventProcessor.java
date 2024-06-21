package com.amazonaws.autoloop.mockserver.processing;

import com.amazon.hyperloop.streaming.model.AttributeUpdates;
import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.EdgeEvent;
import com.amazon.hyperloop.streaming.model.GetAttributeUpdates;
import com.amazon.hyperloop.streaming.model.GetEdgeSubscriptions;
import com.amazon.hyperloop.streaming.model.NotifyEdgeSubscriptionStatus;
import com.amazon.hyperloop.streaming.model.PollAttributeUpdates;
import com.amazon.hyperloop.streaming.model.PollEdgeSubscriptions;
import com.amazon.hyperloop.streaming.model.ReportEdgeSubscriptionStatus;
import com.amazon.hyperloop.streaming.model.SendAttributeUpdates;

public class EdgeEventProcessor {

    public CloudEvent process(EdgeEvent edgeEvent, CreateAttributeSyncStreamInput createAttributeSyncStreamInput) {
        switch (edgeEvent.type()) {
            case ATTRIBUTE_UPDATES: {
                return processAttributeUpdates(edgeEvent.AttributeUpdates(), createAttributeSyncStreamInput);
            }
            case POLL_EDGE_SUBSCRIPTIONS: {
                return processPollEdgeSubscriptions(edgeEvent.PollEdgeSubscriptions(), createAttributeSyncStreamInput);
            }
            case POLL_ATTRIBUTE_UPDATES: {
                return processPollAttributeUpdates(edgeEvent.PollAttributeUpdates(), createAttributeSyncStreamInput);
            }
            case REPORT_EDGE_SUBSCRIPTION_STATUS: {
                return processReportEdgeSubscriptionStatus(
                    edgeEvent.ReportEdgeSubscriptionStatus(),
                    createAttributeSyncStreamInput
                );
            }
            case SEND_ATTRIBUTE_UPDATES: {
                return processSendAttributeUpdates(edgeEvent.SendAttributeUpdates(), createAttributeSyncStreamInput);
            }
            case GET_EDGE_SUBSCRIPTIONS: {
                return processGetEdgeSubscriptions(edgeEvent.GetEdgeSubscriptions(), createAttributeSyncStreamInput);
            }
            case GET_ATTRIBUTE_UPDATES: {
                return processGetAttributeUpdates(edgeEvent.GetAttributeUpdates(), createAttributeSyncStreamInput);
            }
            case NOTIFY_EDGE_SUBSCRIPTION_STATUS: {
                return processNotifyEdgeSubscriptionStatus(
                    edgeEvent.NotifyEdgeSubscriptionStatus(),
                    createAttributeSyncStreamInput
                );
            }
            case $UNKNOWN: {
                break;
            }

        }
        throw new IllegalStateException(
            edgeEvent.type() + ":unknown state, please update this switch-case with new state(s)!"
        );
    }

    protected CloudEvent processAttributeUpdates(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        throw new UnsupportedOperationException("Unimplemented method 'handlePollEdgeSubscriptions'");
    }

    protected CloudEvent processPollEdgeSubscriptions(
        PollEdgeSubscriptions pollEdgeSubscriptions,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        throw new UnsupportedOperationException("Unimplemented method 'handlePollEdgeSubscriptions'");
    }

    protected CloudEvent processPollAttributeUpdates(
        PollAttributeUpdates pollAttributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        throw new UnsupportedOperationException("Unimplemented method 'processPollAttributeUpdates'");
    }

    protected CloudEvent processReportEdgeSubscriptionStatus(
        ReportEdgeSubscriptionStatus reportEdgeSubscriptionStatus,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        throw new UnsupportedOperationException("Unimplemented method 'processReportEdgeSubscriptionStatus'");
    }

    protected CloudEvent processSendAttributeUpdates(
        SendAttributeUpdates sendAttributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        throw new UnsupportedOperationException("Unimplemented method 'processSendAttributeUpdates'");
    }

    protected CloudEvent processGetEdgeSubscriptions(
        GetEdgeSubscriptions getEdgeSubscriptions,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        throw new UnsupportedOperationException("Unimplemented method 'processGetEdgeSubscriptions'");
    }

    protected CloudEvent processGetAttributeUpdates(
        GetAttributeUpdates getAttributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        throw new UnsupportedOperationException("Unimplemented method 'processGetAttributeUpdates'");
    }

    protected CloudEvent processNotifyEdgeSubscriptionStatus(
        NotifyEdgeSubscriptionStatus notifyEdgeSubscriptionStatus,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        throw new UnsupportedOperationException("Unimplemented method 'processNotifyEdgeSubscriptionStatus'");
    }

}
