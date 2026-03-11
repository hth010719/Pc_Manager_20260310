package com.pcmanager.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 소켓 프로토콜에서 문자열 안전성을 보장하기 위한 Base64 인코더/디코더다.
 *
 * 현재 프로토콜은 `|`, `,`, `;` 같은 구분자를 직접 쓰므로
 * 사용자 입력 텍스트는 송수신 전에 인코딩해 충돌을 막는다.
 */
public final class ProtocolCodec {
    private ProtocolCodec() {
    }

    /**
     * UTF-8 문자열을 Base64 문자열로 변환한다.
     */
    public static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 문자열을 원래 UTF-8 문자열로 복원한다.
     */
    public static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
