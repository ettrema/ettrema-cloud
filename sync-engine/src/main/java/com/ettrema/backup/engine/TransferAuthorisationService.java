package com.ettrema.backup.engine;

import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Repo;
import com.ettrema.httpclient.Resource;
import java.io.File;

/**
 * Provides a means for the user to authorise certain operations which result
 * in data transfer
 *
 * @author brad
 */
public interface TransferAuthorisationService {

	/**
	 * Check if we are authorised to download the given remote resource, which
	 * might be a file or folder
	 * 
	 * @param remote 
	 */
	void requestDownload(DavRepo repo, Resource remote);

	void resolveConflict(Resource r, File l);

	void requestUpload(File l);

    void requestRemoteDelete(DavRepo repo, Resource r);

    void requestDeleteLocal(File local);

	
}
