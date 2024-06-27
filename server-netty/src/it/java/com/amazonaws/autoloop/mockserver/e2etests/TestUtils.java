package com.amazonaws.autoloop.mockserver.e2etests;

import com.amazon.hyperloop.streaming.model.ValidationException;
import java.time.Instant;

public class TestUtils {
    public static void validateTimestamp(Instant timestamp) {
        assertTrueOrThrow(timestamp != null, "timestamp is null");
        assertTrueOrThrow(
            timestamp.toEpochMilli() < 1000 + System.currentTimeMillis() && timestamp.toEpochMilli() > -1000 + System
                .currentTimeMillis(),
            "timestamp implausible"
        );
    }

    public static <T> void assertTrueOrThrow(boolean expression, String message) {
        if (!expression) {
            throw ValidationException.builder().message(message).build();
        }
    }

    public static <T> void assertFalseOrThrow(boolean expression, String message) {
        assertTrueOrThrow(!expression, message);
    }

    public static <T> void assertNullOrThrow(T actual, String message) {
        assertTrueOrThrow(actual == null, message);
    }


    public static <T> void assertOrThrow(T expected, T actual, String message) {
        assert (expected != null);
        if (!actual.equals(expected)) {
            throw ValidationException.builder().message(message).build();
        }
    }
}
