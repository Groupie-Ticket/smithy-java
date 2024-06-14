$version: "2"

namespace com.amazon.hyperloop.streaming

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.protocols#idx
use smithy.protocols#indexed


@title("Autoloop Streaming")
// Optional, custom SDK service ID trait.
@service(
    sdkId: "Autoloop Streaming",
    arnNamespace: "autoloop",
    cloudTrailEventSource: "autoloopstreaming.amazonaws.com"
)


@restJson1(http: ["h2"])
@sigv4(name: "autoloop")
@indexed
service Autoloop {
    version: "2023-05-03",

    operations: [
        CreateAttributeSyncStream,
    ]
}

@trait(
    breakingChanges: [
        {
            change: "remove"
            severity: "WARNING"
            message: "Removing the @sdkInternal trait makes a shape visible to public SDKs."
        }
    ]
)
structure sdkInternal {}
