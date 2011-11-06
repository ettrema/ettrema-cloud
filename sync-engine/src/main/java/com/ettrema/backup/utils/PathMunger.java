package com.ettrema.backup.utils;

import com.bradmcevoy.common.Path;
import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Only for URLs
 *
 * @author brad
 */
public class PathMunger {

	private static final Logger log = LoggerFactory.getLogger(PathMunger.class);

	public String findFileFromUrl(List<Root> roots, String url, String seperator) {
		String path = stripHost(url);
		
		for (Root root : roots) {
			if( path.startsWith(root.getRepoName())) {
				path = stripRootPath(path, root.getRepoName());
				path = path.replace("/", seperator);
				path = root.getFullPath() + seperator + path;
				return path;
			}
		}
		log.trace("url does not correspond to any configured root directories. Path: " + path);
		return null;
	}
	
	public Root findRootFromFile(List<Root> roots, File file) {	
		Root bestRoot = null;
		for (Root root : roots) {
			if(file.getAbsolutePath().startsWith(root.getFullPath())) {
				if( bestRoot == null) {
					bestRoot = root;
				} else {
					if( root.getFullPath().length() > bestRoot.getFullPath().length()) {
						bestRoot = root;
					}
				}
			}			
		}
		return bestRoot;
	}	

	public String munge(String filePath, String localRootPath, String repoName) {
		//log.trace( "munge {} {} {}", new String[]{filePath, localRootPath, repoName} );
		if (filePath.startsWith(localRootPath)) {
			String basePath;
			if (filePath.equals(localRootPath)) {
				basePath = localRootPath;
			} else {
				basePath = filePath.substring(localRootPath.length() + 1);
			}
			basePath = basePath.replace("\\", "/");
			if (repoName.length() > 0 && !repoName.endsWith("/")) {
				repoName = repoName + "/";
			}
			return repoName + basePath;
		} else {
			throw new RuntimeException("The file path is not on this root: " + filePath + " - " + localRootPath);
		}

	}

	public String munge(String p, Job job) {
		for (Root r : job.getRoots()) {
			if (p.startsWith(r.getFullPath())) {
				return munge(p, r.getFullPath(), r.getRepoName());
			}
		}
		throw new RuntimeException("File is not within a backed up directory: " + p);
	}

	private String stripHost(String url) {
		URI uri;
		try {
			uri = new URI(url); // Eg http://localhost/users/a1-a1/Documents/dir2/
		} catch (URISyntaxException ex) {
			throw new RuntimeException(url, ex);
		}
		String spath = uri.getPath(); // Eg /users/a1-a1/Documents/dir2/
		Path path = Path.path(spath);
		path = path.getStripFirst().getStripFirst();
		return path.toString();  // Eg /Documents/dir2
		
		// url = http://a.c.c/path
//		int pos = url.indexOf("/", 8);
//		if (pos > 0) {
//			url = url.substring(pos);
//			return url;
//		} else {
//			throw new IllegalArgumentException("Not a valid file path: " + url);
//		}
	}

	private String stripRootPath(String path, String repoName) {
		return path.substring(repoName.length()+1);
	}
}
