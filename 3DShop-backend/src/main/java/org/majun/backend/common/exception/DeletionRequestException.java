package org.majun.backend.common.exception;

import org.majun.backend.common.ResultCode;

public class DeletionRequestException extends BusinessException {
    public DeletionRequestException(ResultCode resultCode) {
        super(resultCode);
    }
}