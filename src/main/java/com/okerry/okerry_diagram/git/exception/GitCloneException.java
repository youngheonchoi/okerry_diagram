package com.okerry.okerry_diagram.git.exception;

public class GitCloneException extends RuntimeException {

    public GitCloneException(String message) {
        super(message);
    }

    public GitCloneException(String message, Throwable cause) {
        super(message, cause);
    }

}
