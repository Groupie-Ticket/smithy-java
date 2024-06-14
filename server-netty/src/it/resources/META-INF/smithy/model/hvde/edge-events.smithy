$version: "2.0"

namespace com.amazon.hyperloop.streaming
use smithy.protocols#idx

@streaming
@documentation("CloudEvent union defines set of events publish from the client to the cloud")
union EdgeEvent {
    @idx(1)
    AttributeUpdates: AttributeUpdates

    @idx(2)
    PollEdgeSubscriptions: PollEdgeSubscriptions

    @idx(3)
    PollAttributeUpdates: PollAttributeUpdates

    @idx(4)
    ReportEdgeSubscriptionStatus: ReportEdgeSubscriptionStatus

    @idx(5)
    SendAttributeUpdates: SendAttributeUpdates

    @idx(6)
    GetEdgeSubscriptions: GetEdgeSubscriptions

    @idx(7)
    GetAttributeUpdates: GetAttributeUpdates

    @idx(8)
    NotifyEdgeSubscriptionStatus: NotifyEdgeSubscriptionStatus
}

@documentation(
    "This structure defines a batch of attribute updates published for subscribed attribute value changes"
)
structure AttributeUpdates {
    @required
    @idx(1)
    requestId: RequestId

    @required
    @idx(2)
    attributes: AttributeValueUpdates

    @required
    @idx(3)
    timestamp: Timestamp

    @idx(4)
    metadata: AttributeMetadataSet
}

@documentation(
    "This structure defines a batch of attribute updates published for subscribed attribute value changes"
)
structure SendAttributeUpdates {
    @required
    @idx(1)
    requestId: RequestId

    @required
    @idx(2)
    timestamp: Timestamp

    @required
    @idx(3)
    attributes: AttributeValueUpdates

    @idx(4)
    metadata: AttributeMetadataSet
}

structure ReportEdgeSubscriptionStatus {
    @required
    @idx(1)
    requestId: RequestId
    
    @required
    @idx(2)
    edgeSubscriptionId: EdgeSubscriptionId
    
    @required
    @idx(3)
    revision: EdgeSubscriptionRevision
    
    @required
    @idx(4)
    status: ReportedEdgeSubscriptionStatus
    
    @length(min: 0, max: 1000)
    @documentation("Optional reason why EdgeSubscription was rejected")
    @idx(5)
    message: String
    
    @required
    @idx(6)
    timestamp: Timestamp
}

structure PollEdgeSubscriptions {
    @range(min: 1, max: 50)
    @idx(1)
    maxResults: Integer

    @required
    @idx(2)
    requestId: RequestId

    @idx(3)
    edgeSubscriptionsSummary: PollEdgeSubscriptionSummaryList
}

structure GetEdgeSubscriptions {
    @required
    @idx(1)
    requestId: RequestId

    @required
    @idx(2)
    timestamp: Timestamp

    @range(min: 1, max: 50)
    @idx(3)
    maxResults: Integer

    @idx(4)
    edgeSubscriptionsSummary: GetEdgeSubscriptionSummaryList
}

structure PollAttributeUpdates {
    @required
    @documentation("Unique identifier of the request event")
    @idx(1)
    requestId: RequestId

    @documentation("If synchronizationToken is not provided, the latest state of all attributes will be retrieved")
    @idx(2)
    synchronizationToken: SynchronizationToken
}

structure GetAttributeUpdates {
    @required
    @idx(1)
    requestId: RequestId

    @required
    @idx(2)
    timestamp: Timestamp

    @documentation("If synchronizationToken is not provided, the latest state of all attributes will be retrieved")
    @idx(3)
    synchronizationToken: SynchronizationToken
}

structure NotifyEdgeSubscriptionStatus {
    @required
    @idx(1)
    requestId: RequestId

    @required
    @idx(2)
    timestamp: Timestamp

    @required
    @idx(3)
    edgeSubscriptionId: EdgeSubscriptionId

    @required
    @idx(4)
    revision: EdgeSubscriptionRevision

    @required
    @idx(5)
    status: ReportedEdgeSubscriptionStatus

    @length(min: 0, max: 1000)
    @documentation("Optional reason why EdgeSubscription was rejected")
    @idx(6)
    message: String
}