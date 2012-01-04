package com.ettrema.backup.engine;

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
	void requestDownload(Resource remote);

	public void resolveConflict(Resource r, File l);

	public void requestUpload(File l);

    public void requestRemoteDelete(Resource r);

    public void requestDeleteLocal(File local);

	
}
