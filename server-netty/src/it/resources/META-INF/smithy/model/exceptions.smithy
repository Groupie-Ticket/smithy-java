$version: "2"

namespace com.amazon.hyperloop.streaming
use smithy.protocols#idx

@error("client")
@httpError(400)
structure ValidationException {
    @idx(1)
    message: String,
}

@error("client")
@httpError(403)
structure AccessDeniedException {
    @idx(1)
    message: String,
}

@error("client")
@httpError(404)
structure ResourceNotFoundException {
    @idx(1)
    message: String,
}

@error("client")
@httpError(409)
structure ConflictException {
    @idx(1)
    message: String,
}

@error("client")
@httpError(429)
structure ThrottlingException {
    @idx(1)
    message: String,
}

@error("server")
@httpError(500)
structure InternalServerException {
    @idx(1)
    message: String,
}

