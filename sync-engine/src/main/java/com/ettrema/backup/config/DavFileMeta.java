package com.ettrema.backup.config;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.ettrema.httpclient.Folder;
import com.ettrema.httpclient.HttpException;
import com.ettrema.httpclient.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author brad
 */
public class DavFileMeta implements FileMeta {

    private final com.ettrema.httpclient.Resource resource;

    public DavFileMeta(com.ettrema.httpclient.Resource resource) {
        this.resource = resource;
    }

    /**
     * @return the modifiedDate
     */
    @Override
    public Date getModifiedDate() {
        return resource.getModifiedDate();
    }

    /**
     * @return the length
     */
    @Override
    public long getLength() {
        if (resource instanceof com.ettrema.httpclient.File) {
            com.ettrema.httpclient.File f = (com.ettrema.httpclient.File) resource;
            Long ll = f.contentLength;
            if (ll != null) {
                return ll;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean isDirectory() {
        return (resource instanceof com.ettrema.httpclient.Folder);
    }

    @Override
    public Long getCrc() {
        return resource.getCrc();
    }

    /**
     * The name of the repository resource within its folder
     *
     * @return
     */
    @Override
    public String getName() {
        return resource.name;
    }

    @Override
    public List<FileMeta> getChildren() throws RepoNotAvailableException {
        if (resource instanceof Folder) {
            try {
                Folder f = (Folder) resource;
                List<FileMeta> list = new ArrayList<FileMeta>();
                for (Resource r : f.children()) {
                    list.add(new DavFileMeta(r));
                }
                return list;
            } catch (NotAuthorizedException ex) {
                throw new RepoNotAvailableException(ex);
            } catch (BadRequestException ex) {
                throw new RepoNotAvailableException(ex);
            } catch (IOException ex) {
                throw new RepoNotAvailableException(ex);
            } catch (HttpException ex) {
                throw new RepoNotAvailableException(ex);
            }
        } else {
            return null;
        }
    }
}
