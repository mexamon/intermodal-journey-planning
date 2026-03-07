package com.thy.cloud.base.core.exception;

public class AccessDeniedException extends RuntimeException{


    private static final long serialVersionUID = -1594183961301143030L;

    public AccessDeniedException() {
        super();
    }

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}