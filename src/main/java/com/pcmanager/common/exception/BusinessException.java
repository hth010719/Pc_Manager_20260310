package com.pcmanager.common.exception;

/**
 * 사용자에게 그대로 안내할 수 있는 업무 오류를 나타내는 런타임 예외다.
 *
 * 네트워크 계층, 서비스 계층, UI 계층 모두 이 예외를 공통으로 사용해
 * "왜 실패했는지"를 메시지 중심으로 전달한다.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
