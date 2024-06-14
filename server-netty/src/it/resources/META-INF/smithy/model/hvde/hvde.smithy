$version: "2.0"

namespace com.amazon.hyperloop.streaming
use smithy.protocols#idx

@http(method: "POST", uri: "/create-attribute-sync-stream")
@documentation(
    "Edge Facing API implementing Hyperloop Edge Sync Protocol."
)
@endpoint(hostPrefix: "sync-engine.")
operation CreateAttributeSyncStream {
    input := {
        @httpHeader("x-amzn-attr-stream-data-sync-engine-identifier")
        @required
        @idx(1)
        dataSyncEngineIdentifier: ResourceIdentifierString

        @httpHeader("x-amzn-attr-stream-object-id")
        @required
        @suppress(["AWSReservedWords"])
        @idx(2)
        objectId: ObjectId

        @httpPayload
        @required
        @idx(3)
        event: EdgeEvent
    }
    output := {
        @httpPayload
        @required
        @idx(1)
        event: CloudEvent
    }
    errors: [
        ValidationException
        AccessDeniedException
        ConflictException
        InternalServerException
        ResourceNotFoundException,
        ThrottlingException
    ]

}
