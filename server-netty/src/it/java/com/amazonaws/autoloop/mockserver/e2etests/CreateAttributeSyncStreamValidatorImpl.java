
package com.amazonaws.autoloop.mockserver.e2etests;

import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.ValidationException;
import com.amazonaws.autoloop.mockserver.processing.CreateAttributeSyncStreamValidator;

public final class CreateAttributeSyncStreamValidatorImpl extends CreateAttributeSyncStreamValidator {

    @Override
    public void validate(CreateAttributeSyncStreamInput input) {
        if (input.objectId().length() < 5) {
            throw ValidationException.builder().message("Object ID too short: " + input.objectId()).build();
        }
    }
}
