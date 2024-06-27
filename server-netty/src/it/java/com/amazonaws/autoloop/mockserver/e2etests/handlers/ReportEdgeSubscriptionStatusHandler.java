package com.amazonaws.autoloop.mockserver.e2etests.handlers;

import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.ReportEdgeSubscriptionStatus;
import com.amazon.hyperloop.streaming.model.ReportedEdgeSubscriptionStatus;
import com.amazonaws.autoloop.mockserver.e2etests.TestUtils;

public class ReportEdgeSubscriptionStatusHandler {

    public CloudEvent handle(
        ReportEdgeSubscriptionStatus reportEdgeSubscriptionStatus,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        switch (reportEdgeSubscriptionStatus.requestId()) {
            case "reportEdgeSubscriptionStatus1": {
                return reportEdgeSubscriptionStatus1(reportEdgeSubscriptionStatus, createAttributeSyncStreamInput);
            }
            case "reportEdgeSubscriptionStatus2": {
                return reportEdgeSubscriptionStatus2(reportEdgeSubscriptionStatus, createAttributeSyncStreamInput);
            }
        }
        throw new UnsupportedOperationException("unhandled testcase");
    }

    private CloudEvent reportEdgeSubscriptionStatus1(
        ReportEdgeSubscriptionStatus reportEdgeSubscriptionStatus,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {

        TestUtils.assertOrThrow(
            "sub1",
            reportEdgeSubscriptionStatus.edgeSubscriptionId(),
            "wrong value for edgeSubscriptionId received"
        );
        TestUtils.assertOrThrow(
            ReportedEdgeSubscriptionStatus.ACCEPTED,
            reportEdgeSubscriptionStatus.status(),
            "wrong status received for reportEdgeSubscriptionStatus"
        );
        TestUtils.assertOrThrow(
            "1",
            reportEdgeSubscriptionStatus.revision(),
            "wrong value received for revision"
        );
        TestUtils.assertOrThrow(
            "Test message",
            reportEdgeSubscriptionStatus.message(),
            "wrong value received for revision"
        );
        TestUtils.validateTimestamp(reportEdgeSubscriptionStatus.timestamp());

        return null;
    }

    private CloudEvent reportEdgeSubscriptionStatus2(
        ReportEdgeSubscriptionStatus reportEdgeSubscriptionStatus,
        CreateAttributeSyncStreamInput createAttributeSyncStreamInput
    ) {
        TestUtils.assertOrThrow(
            "sub1",
            reportEdgeSubscriptionStatus.edgeSubscriptionId(),
            "wrong value for edgeSubscriptionId received"
        );
        TestUtils.assertOrThrow(
            ReportedEdgeSubscriptionStatus.ACCEPTED,
            reportEdgeSubscriptionStatus.status(),
            "wrong status received for reportEdgeSubscriptionStatus"
        );
        TestUtils.assertOrThrow(
            "1",
            reportEdgeSubscriptionStatus.revision(),
            "wrong value received for revision"
        );
        TestUtils.assertNullOrThrow(
            reportEdgeSubscriptionStatus.message(),
            "wrong value received for revision"
        );
        TestUtils.validateTimestamp(reportEdgeSubscriptionStatus.timestamp());

        return null;
    }
}
