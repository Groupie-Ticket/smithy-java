$version: "2.0"

namespace com.amazon.hyperloop.streaming
use smithy.protocols#idx

@streaming
union CloudEvent {
    @idx(1)
    EventAck: EdgeEventAck

    @idx(2)
    EventNAck: EdgeEventNAck

    @idx(3)
    EdgeSubscriptionChanges: EdgeSubscriptionChanges

    @idx(4)
    InternalServerException: InternalServerException

    @idx(5)
    AccessDeniedException: AccessDeniedException

    @idx(6)
    ValidationException: ValidationException

    @idx(7)
    ResourceNotFoundException: ResourceNotFoundException

    @idx(8)
    PolledAttributeUpdates: PolledAttributeUpdates

    @idx(9)
    ThrottlingException: ThrottlingException

    @idx(10)
    SendAttributeUpdatesResponse: SendAttributeUpdatesResponse

    @idx(11)
    GetEdgeSubscriptionsResponse: GetEdgeSubscriptionsResponse

    @idx(12)
    GetAttributeUpdatesResponse: GetAttributeUpdatesResponse

    @idx(13)
    NotifyEdgeSubscriptionStatusResponse: NotifyEdgeSubscriptionStatusResponse
}

@documentation("Acknowledgement of successful processing of edge events from Cloud")
structure EdgeEventAck {
    @required
    @idx(1)
    requestId: RequestId
}

@documentation("Negative Acknowledgement of failed processing of edge events from Cloud")
structure EdgeEventNAck {
    @required
    @idx(1)
    requestId: RequestId

    @required
    @idx(2)
    reason: NAckReason
}

structure SendAttributeUpdatesResponse {
    @required
    @idx(1)
    requestId: RequestId

    @required
    @idx(2)
    status: SendAttributeUpdatesResponseStatus
}

intEnum SendAttributeUpdatesResponseStatus {
    SUCCESS = 0
    MESSAGE_TOO_BIG = 1
    UNKNOWN = 2
}

list EdgeSubscriptionList {
    member: EdgeSubscription
}

structure EdgeSubscription {
    @required
    @idx(1)
    attributeId: AttributeId

    @required
    @idx(2)
    edgeSubscriptionId: EdgeSubscriptionId

    @required
    @idx(3)
    edgeSubscriptionStatus: EdgeSubscriptionStatus

    @required
    @idx(4)
    debounceIntervalMs: DebounceIntervalMs

    @range(min: 1, max: 10)
    @documentation("This is only applicable to numeric values")
    @idx(5)
    precision: Integer
}

structure EdgeSubscriptionChanges {
    @required
    @idx(1)
    changes: EdgeSubscriptionChangesList

    @required
    @idx(2)
    pollEnd: Boolean

    @required
    @idx(3)
    requestId: String
}

structure ListEdgeSubscriptionsResponse {
    @required
    @idx(1)
    requestId: RequestId

    @idx(2)
    @required
    edgeSubscriptions: EdgeSubscriptionList

    @idx(3)
    nextToken: String
}

structure GetEdgeSubscriptionsResponse {
    @required
    @idx(1)
    requestId: RequestId

    @required
    @idx(2)
    changes: EdgeSubscriptionChangesList

    @required
    @idx(3)
    pollEnd: Boolean
}

structure PolledAttributeUpdates {
    @required
    @documentation("Unique identifier of the request event")
    @idx(1)
    requestId: RequestId

    @required
    @documentation("Attribute update values")
    @idx(2)
    attributes: PolledAttributeSet

    @required
    @documentation("Denotes whether polling for attribute updates has ended or there are additional updates to poll")
    @idx(3)
    pollEnd: Boolean

    @documentation("Synchronization token which was used to retrieve this result")
    @idx(4)
    synchronizationToken: SynchronizationToken
}

structure GetAttributeUpdatesResponse {
    @required
    @documentation("Unique identifier of the request event")
    @idx(1)
    requestId: RequestId

    @required
    @documentation("Attribute update values")
    @idx(2)
    attributes: PolledAttributeSet

    @required
    @documentation("Denotes whether polling for attribute updates has ended or there are additional updates to poll")
    @idx(3)
    pollEnd: Boolean

    @documentation("Synchronization token which was used to retrieve this result")
    @idx(4)
    synchronizationToken: SynchronizationToken
}

structure NotifyEdgeSubscriptionStatusResponse {
    @required
    @documentation("Unique identifier of the request event")
    @idx(1)
    requestId: RequestId
}
