package com.amazonaws.autoloop.mockserver.e2etests;

import com.amazon.hyperloop.streaming.model.AttributeUpdates;
import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.PollAttributeUpdates;
import com.amazon.hyperloop.streaming.model.PollEdgeSubscriptions;
import com.amazon.hyperloop.streaming.model.ReportEdgeSubscriptionStatus;
import com.amazonaws.autoloop.mockserver.e2etests.handlers.AttributeUpdatesHandler;
import com.amazonaws.autoloop.mockserver.e2etests.handlers.PollAttributeUpdatesHandler;
import com.amazonaws.autoloop.mockserver.e2etests.handlers.PollEdgeSubscriptionsHandler;
import com.amazonaws.autoloop.mockserver.e2etests.handlers.ReportEdgeSubscriptionStatusHandler;
import com.amazonaws.autoloop.mockserver.processing.EdgeEventProcessor;


public final class EdgeEventProcessorImpl extends EdgeEventProcessor {

    private final PollAttributeUpdatesHandler pollAttributeUpdatesHandler = new PollAttributeUpdatesHandler();
    private final PollEdgeSubscriptionsHandler pollEdgeSubscriptionsHandler = new PollEdgeSubscriptionsHandler();
    private final AttributeUpdatesHandler attributeUpdatesHandler = new AttributeUpdatesHandler();
    private final ReportEdgeSubscriptionStatusHandler reportEdgeSubscriptionStatusHandler = new ReportEdgeSubscriptionStatusHandler();


    @Override
    protected CloudEvent processAttributeUpdates(
        AttributeUpdates attributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        return attributeUpdatesHandler.handle(attributeUpdates, createAttributeSyncStreamInput);
    }

    @Override
    protected CloudEvent processPollEdgeSubscriptions(
        PollEdgeSubscriptions pollEdgeSubscriptions,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        return pollEdgeSubscriptionsHandler.handle(pollEdgeSubscriptions, createAttributeSyncStreamInput);
    }

    @Override
    protected CloudEvent processPollAttributeUpdates(
        PollAttributeUpdates pollAttributeUpdates,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        return pollAttributeUpdatesHandler.handle(pollAttributeUpdates, createAttributeSyncStreamInput);
    }

    @Override
    protected CloudEvent processReportEdgeSubscriptionStatus(
        ReportEdgeSubscriptionStatus reportEdgeSubscriptionStatus,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        return reportEdgeSubscriptionStatusHandler.handle(reportEdgeSubscriptionStatus, createAttributeSyncStreamInput);
    }
}
