package com.ettrema.backup.config;

public class RepoNotAvailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public RepoNotAvailableException(Throwable cause) {
        super(cause);
    }

    public RepoNotAvailableException(String msg) {
        super(msg);
    }
    
    public RepoNotAvailableException(String msg, Throwable cause) {
        super(msg, cause);
    }    
}
