
package com.amazonaws.autoloop.mockserver.e2etests;

import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.ValidationException;
import com.amazonaws.autoloop.mockserver.processing.CreateAttributeSyncStreamValidator;

public final class CreateAttributeSyncStreamValidatorImpl extends CreateAttributeSyncStreamValidator {

    @Override
    public void validate(CreateAttributeSyncStreamInput input) {
        if (input.dataSyncEngineIdentifier().equals("invalid")) {
            throw ValidationException.builder().message("Invalid datasync engine identifier received").build();
        }
    }
}
