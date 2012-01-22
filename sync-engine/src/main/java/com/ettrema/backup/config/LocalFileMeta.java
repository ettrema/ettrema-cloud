package com.ettrema.backup.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author brad
 */
public class LocalFileMeta implements FileMeta {

    private final File file;

    public LocalFileMeta(File file) {
        this.file = file;
    }

    @Override
    public Date getModifiedDate() {
        return new Date(file.lastModified());
    }

    @Override
    public long getLength() {
        return file.length();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public Long getCrc() {
        return null;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public List<FileMeta> getChildren() throws RepoNotAvailableException {
        if (file.isDirectory()) {
            List<FileMeta> list = new ArrayList<FileMeta>();
            for (File child : file.listFiles()) {
                list.add(new LocalFileMeta(child));
            }
            return list;
        } else {
            return null;
        }
    }
}
