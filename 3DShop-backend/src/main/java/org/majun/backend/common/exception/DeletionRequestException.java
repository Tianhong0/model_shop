package org.majun.backend.common.exception;

import org.majun.backend.common.ResultCode;

/**
 * 账户删除请求异常
 * 用于处理用户账户删除相关的业务异常
 */
public class DeletionRequestException extends BusinessException {
    public DeletionRequestException(ResultCode resultCode) {
        super(resultCode);
    }
}