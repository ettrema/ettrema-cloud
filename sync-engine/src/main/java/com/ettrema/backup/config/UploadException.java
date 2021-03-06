package com.ettrema.backup.config;

import java.io.File;

public class UploadException extends Exception {

    private static final long serialVersionUID = 1L;
    private final File source;

    public UploadException( File source, Exception cause ) {
        super( cause );
        this.source = source;
    }

    public File getSource() {
        return source;
    }
}
