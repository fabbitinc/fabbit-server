package com.fabbitinc.server.domain.common.exception;

import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {

    private final String domainCode;

    public DomainException(String domainCode, String message) {
        super(message);
        this.domainCode = domainCode;
    }
}
