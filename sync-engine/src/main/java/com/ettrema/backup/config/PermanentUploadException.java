package com.ettrema.backup.config;

public class PermanentUploadException extends Exception {

    private static final long serialVersionUID = 1L;

    public PermanentUploadException( Throwable cause ) {
        super( cause );
    }

    public PermanentUploadException( String message ) {
        super( message );
    }
}
