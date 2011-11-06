package com.ettrema.backup.config;

import com.ettrema.cache.Cache;
import com.ettrema.backup.engine.CrcCalculator;
import com.ettrema.httpclient.HttpException;
import java.util.ArrayList;
import java.util.List;
import com.ettrema.backup.engine.Engine;
import com.bradmcevoy.common.Path;
import com.ettrema.backup.engine.ThrottleFactory;
import com.ettrema.backup.event.RepoChangedEvent;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.backup.utils.PathMunger;
import com.ettrema.cache.MemoryCache;
import com.ettrema.httpclient.Folder;
import com.ettrema.httpclient.Host;
import com.ettrema.httpclient.NotFoundException;
import com.ettrema.httpclient.ProgressListener;
import com.ettrema.httpclient.ProxyDetails;
import com.ettrema.httpclient.Resource;
import com.ettrema.httpclient.Unauthorized;
import com.ettrema.httpclient.Utils.CancelledException;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static com.ettrema.backup.engine.Services._;

/**
 *
 * @author brad
 */
public class DavRepo implements Repo {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DavRepo.class);
	private static final int SO_TIMEOUT = 15 * 60 * 1000; // millis
	private String hostName;
	private String rootPath;
	private String user;
	private String pwd;
	private int port;
	private boolean useSSL;
	private boolean sync;
	private boolean proxyUseSystem;
	private String proxyHost;
	private int proxyPort;
	private String proxyUserName;
	private String proxyPassword;
	// transient fields;
	private transient QueueItem current;
	private transient Host h;
	private transient boolean offline;
	private transient Long backedupBytes;
	private transient Long scanningBackedupBytes;
	private transient long backedupSinceLastScanBytes;
	private transient Long accountUsedBytes;
	private transient Long maxBytes;
	private transient Queue queue;
	private transient Job job;
	private transient MemoryCache<Folder, List<Resource>> cache;

	public DavRepo() {
	}

	public DavRepo(Job j) {
		this.job = j;
	}

	public Cache<Folder, List<Resource>> getCache() {
		if (cache == null) {
			if (job == null) {
				cache = new MemoryCache<Folder, List<Resource>>("resource-cache-default", 500, 50);
			} else {
				cache = new MemoryCache<Folder, List<Resource>>("resource-cache-" + job.getId(), 500, 50);
			}
			cache.start();
		}
		return cache;
	}

	public Host host() throws RepoNotAvailableException {
		return host(false);
	}

	public Host host(boolean create) throws RepoNotAvailableException {
		if (create) {
			return createHost();
		} else {
			if (h == null) {
				h = createHost();
				log.trace("repo changed, connected to host");
				EventUtils.fireQuietly(new RepoChangedEvent(this));
			}
			return h;
		}
	}

	private Host createHost() {
		ProxyDetails pd = new ProxyDetails();
		pd.setUseSystemProxy(isProxyUseSystem());
		pd.setProxyHost(getProxyHost());
		pd.setProxyPort(getProxyPort());
		pd.setUserName(getProxyUserName());
		pd.setPassword(getProxyPassword());

		Host host = new Host(getHostName(), getRootPath(), getPort(), getUser(), getPwd(), pd, getCache());
		host.setTimeout(SO_TIMEOUT);
		return host;
	}

	@Override
	public Queue getQueue() {
		if (queue == null) {
			queue = new Queue();
		}
		return queue;
	}

	@Override
	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	@Override
	public void onScan() {
		log.trace("onScan");
		scanningBackedupBytes = 0l;
		// check the host is accessible. if was offline and now online reset the status

		try {
			Host host = host();
			// test the connection
			host.find("/");

			if (offline) {
				// was offline, set back to online
				setOffline(false);
				log.trace("repo status changed, now online");
				EventUtils.fireQuietly(new RepoChangedEvent(this));
			}
			host.flush();
		} catch (HttpException ex) {
			log.error("exception in scan", ex);
			if (!offline) {
				setOffline(true);
				log.trace("repo status changed, now offline");
				EventUtils.fireQuietly(new RepoChangedEvent(this));
			}
		} catch (RepoNotAvailableException e) {
			log.trace("RepoNotAvailableException");
			if (!offline) {
				setOffline(true);
				log.trace("repo status changed, now offline");
				EventUtils.fireQuietly(new RepoChangedEvent(this));
			}
		} catch (IOException ex) {
			log.trace("IOException");
			if (!offline) {
				setOffline(true);
				log.trace("repo status changed, now offline");
				EventUtils.fireQuietly(new RepoChangedEvent(this));
			}
		}


		// As good a time as any to check the quota
	}

	@Override
	public void onScanComplete() {
		log.info("scan complete: backedupBytes: " + backedupBytes);
		log.info("scan complete: scanningBackedupBytes: " + scanningBackedupBytes);
		log.info("scan complete: backedupSinceLastScanBytes: " + backedupSinceLastScanBytes);
		backedupBytes = scanningBackedupBytes;
		scanningBackedupBytes = null;
		backedupSinceLastScanBytes = 0;
	}

	private void addBackedupBytes(long length) {
		if (scanningBackedupBytes == null) {
			scanningBackedupBytes = 0l;
		}
		scanningBackedupBytes += length;
	}

	@Override
	public FileMeta getFileMeta(String filePath, String localRootPath, String repoName, boolean isScan) throws RepoNotAvailableException {
		String path = _(PathMunger.class).munge(filePath, localRootPath, repoName);

		Path p = Path.path(path);
		if (isCodeBehind(p)) {
			p = codeBehindToPage(p);
			path = p.toString();
		} else if (isSource(p)) {
			p = sourceToPage(p);
			path = p.toString();
		}

		try {
			Resource remote = host().find(path, !isScan); // invalidate the cache if its not during a scan, so we get fresh info
			if (remote == null) {
				log.trace("not found: " + path);
				return null;
			} else {
				updateAccountInfo(remote);
				FileMeta meta = new FileMeta(remote.name);
				meta.setModifiedDate(remote.getModifiedDate());
				if (remote instanceof Folder) {
					meta.setDirectory(true);
				} else {
					com.ettrema.httpclient.File remoteFile = (com.ettrema.httpclient.File) remote;
					meta.setLength(remoteFile.contentLength);
					meta.setCrc(remoteFile.getCrc());
					if (isScan) {
						addBackedupBytes(meta.getLength());
					}
				}
				return meta;
			}
		} catch (com.ettrema.httpclient.Unauthorized e) {
			throw new RepoNotAvailableException(e);
		} catch (HttpException e) {
			throw new RepoNotAvailableException(e);
		} catch (IOException e) {
			throw new RepoNotAvailableException(e);
		}
	}

	@Override
	public List<FileMeta> listFileMeta(String repoPath) throws RepoNotAvailableException {
		//repoPath = repoPath.replace( "/", File.separator );
		log.trace("listFileMeta: " + repoPath);
		try {
			Resource remote = host().find(repoPath);
			if (remote != null) {
				if (remote instanceof Folder) {
					Folder remoteFolder = (Folder) remote;
					if (remoteFolder.hasChildren()) {
						Iterable<? extends Resource> children = remoteFolder.children();
						List<FileMeta> list = new ArrayList<FileMeta>();
						for (Resource fChild : children) {
							FileMeta fmChild = new FileMeta(fChild.name);
							fmChild.setCrc(null); // don't have it
							if (fChild instanceof com.ettrema.httpclient.File) {
								com.ettrema.httpclient.File fChildFile = (com.ettrema.httpclient.File) fChild;
								fmChild.setDirectory(false);
								fmChild.setLength(fChildFile.contentLength);
							} else {
								fmChild.setDirectory(true);
							}
							fmChild.setModifiedDate(fChild.getModifiedDate());
							list.add(fmChild);
						}
						return list;
					} else {
						log.trace("no resources in path");
						return null;
					}
				} else {
					log.trace("path is a file, not a folder");
					return null;
				}
			} else {
				log.trace("path doesnt exist: " + repoPath + " in host: " + hostName);
				return null;
			}

		} catch (HttpException e) {
			throw new RepoNotAvailableException(e);
		} catch (IOException e) {
			throw new RepoNotAvailableException(e);
		}
	}

	@Override
	public boolean delete(File file, Job job) throws DeleteException, RepoNotAvailableException {

		String s = _(PathMunger.class).munge(file.getAbsolutePath(), job);
		Path path = Path.path(s);
		log.trace("delete :" + path);
		Path remoteParentPath = path.getParent();
		Folder remoteFolder = getRemoteFolder(host(), remoteParentPath);
		if (remoteFolder == null) {
			log.debug("remote folder does not exist: " + remoteParentPath);
			return false;
		}
		Resource remote;
		try {
			remote = remoteFolder.child(file.getName());
		} catch (HttpException e) {
			throw new RepoNotAvailableException(e);
		} catch (IOException ex) {
			throw new RepoNotAvailableException(ex);
		}
		if (remote == null) {
			log.debug("remote file does not exist");
			return false;
		} else {
			try {
				log.trace("deleting url: " + remote.href());
				remote.delete();

				return true;
			} catch (Exception e) {
				throw new DeleteException(file, e);
			}

		}
	}

	/**
	 * Retrieve the repository version of the file associated with localTarget, and
	 * download it to the dest file location.
	 * 
	 * The dest file is normally a temporary file, because the download might 
	 * get interrupted so we don't want to be writing directly to the target file.
	 * 
	 * @param dest - the file to create by retrieving the repository file
	 * @param localTarget - the local file which identifies the remote file to download
	 * @param job - the backup job which has initiated the download
	 * @param listener - will receive progress callbacks
	 * @throws UploadException
	 * @throws RepoNotAvailableException
	 * @throws PermanentUploadException
	 */
	@Override
	public void download(File dest, File localTarget, Job job, ProgressListener listener) throws UploadException, RepoNotAvailableException, PermanentUploadException, CancelledException {
		try {
			log.info("download: " + dest.getAbsolutePath());
			String s = _(PathMunger.class).munge(localTarget.getAbsolutePath(), job);
			Resource remote = host().find(s, true); // true means to invalidate the cache. Tbis is because if we're downloading the remote server has changed
			if (remote == null) {
				throw new UploadException(localTarget, new Exception("The repository resource does not exist: " + s));
			}
			if (remote instanceof com.ettrema.httpclient.File) {
				com.ettrema.httpclient.File fRemote = (com.ettrema.httpclient.File) remote;
				fRemote.downloadToFile(dest, listener);
			}
		} catch (CancelledException e) {
			throw e;
		} catch (HttpException e) {
			throw new RepoNotAvailableException(e);
		} catch (IOException ex) {
			throw new RepoNotAvailableException(ex);
		}
	}

	@Override
	public void upload(File file, Job job, QueueItem item) throws UploadException, RepoNotAvailableException, PermanentUploadException {
		String s = _(PathMunger.class).munge(file.getAbsolutePath(), job);
		Path path = Path.path(s);
		log.trace("upload path:" + path);

		Path remoteParentPath = path.getParent();
		Folder remoteFolder = getOrCreate(host(), remoteParentPath);
		if (remoteFolder == null) {
			throw new PermanentUploadException("Couldnt resolve parent folder: " + remoteParentPath);
		}
		try {
			long previousBytes = 0;
			log.trace("look for existing file: folder: " + remoteFolder.href() + "  child:" + file.getName());
			Resource existing = remoteFolder.child(file.getName());
			if (existing instanceof com.ettrema.httpclient.File) {
				com.ettrema.httpclient.File fExisting = (com.ettrema.httpclient.File) existing;

				Long remoteCrc = fExisting.getCrc();
				if (remoteCrc != null) {
					log.trace("got crc: " + remoteCrc);
					long localCrc = _(CrcCalculator.class).getLocalCrc(file);
					if (localCrc == remoteCrc) {
						item.setNotes("Not uploaded, because remote file exists and is identical");
						return;
					} else {
						log.trace("files differ: local: " + localCrc + " != remote: " + remoteCrc);
					}
				}

				if (fExisting.contentLength != null) {
					previousBytes = fExisting.contentLength;
				}
			} else {
				log.trace("no existing file");
			}
			QueueItemProgressListener progressListener = new QueueItemProgressListener(item, _(ProgressListener.class));
			ProgressListener throttle = _(ThrottleFactory.class).createThrottle(file.length(), progressListener);
			item.setNotes("Uploading...");
			log.trace("upload bytes: " + file.length() + " previous bytes: " + previousBytes);			
			remoteFolder.upload(file, throttle);

			long newBytes = file.length() - previousBytes;
			backedupSinceLastScanBytes += newBytes;
			if (backedupSinceLastScanBytes < 0) {  // unlikely situation
				backedupSinceLastScanBytes = file.length();
			}
			log.trace("backedupSinceLastScanBytes: " + backedupSinceLastScanBytes + " newBytes: " + newBytes);
		} catch (HttpException e) {
			log.warn("http exception uploading: " + file.getAbsolutePath(), e);
			throw new RepoNotAvailableException(e);
		} catch (IOException ex) {
			System.out.println("davRepo: ioeex");
			throw new RepoNotAvailableException(ex);
		}
		log.trace("finished upload");
	}

	@Override
	public void move(String fullPathFrom, File dest, Job job, QueueItem item) throws RepoNotAvailableException, PermanentUploadException, UploadException {
		try {
			System.out.println("move: " + fullPathFrom + " -> " + dest.getAbsolutePath());
			log.info("move: " + fullPathFrom + " -> " + dest.getAbsolutePath());
			String sSrcPath = _(PathMunger.class).munge(fullPathFrom, job);
			Path pSrc = Path.path(sSrcPath);
			String sDestPath = _(PathMunger.class).munge(dest.getAbsolutePath(), job);
			Path pDest = Path.path(sDestPath);

			Resource rSrc = host().find(pSrc.toString());
			if (rSrc == null) {
				// doesnt exist so upload it. If directory we won't upload, because that
				// could be a very large task. Instead we'll leave it to the next scan
				if (dest.isDirectory()) {
					item.setNotes("moved remote resource doesnt exist. Local resource is a directory, will upload on next scan");
					throw new PermanentUploadException("Cant rename, source doesnt exist");
				} else {
					item.setNotes("moved remote resource doesnt exist. Local resource is a file so will upload immediately");
					upload(dest, job, item);
				}
			} else {
				if (isSameParent(pSrc, pDest)) {
					item.setNotes("rename from:" + pSrc.getName() + " -> " + pDest.getName());
					rSrc.rename(dest.getName());
					item.setNotes("finished rename: " + rSrc.href());
				} else {
					item.setNotes("move from:" + pSrc + " to " + pDest);
					// Find or create the destination folder
					Path remoteParentPath = pDest.getParent();
					Folder remoteFolder = getOrCreate(host(), remoteParentPath);
					if (remoteFolder == null) {
						throw new PermanentUploadException("Couldnt resolve parent folder: " + remoteParentPath);
					}

					rSrc.moveTo(remoteFolder);
					item.setNotes("finished move: " + rSrc.href());
				}
			}
		} catch (HttpException e) {
			throw new RepoNotAvailableException(e);
		} catch (IOException ex) {
			throw new RepoNotAvailableException(ex);
		}
	}

	private boolean isSource(Path path) {
		if (path == null || path.getName() == null) {
			return false;
		}
		return path.getName().endsWith(".source");
	}

	public Path sourceToPage(Path path) {
		String nm = path.getName().replace(".source", "");
		if (path.getParent() == null) {
			return Path.path(nm);
		} else {
			return path.getParent().child(nm);
		}
	}

	private boolean isCodeBehind(Path path) {
		if (path == null || path.getName() == null) {
			return false;
		}
		return path.getName().endsWith(".code.xml");
	}

	public Path codeBehindToPage(Path path) {
		String nm = path.getName().replace(".code.xml", "");
		if (path.getParent() == null) {
			return Path.path(nm);
		} else {
			return path.getParent().child(nm);
		}
	}

	private Folder getOrCreate(Host host, Path remoteParentPath) throws RepoNotAvailableException {
		return getRemoteFolder(host, remoteParentPath, true);
	}

	private Folder getRemoteFolder(Host host, Path remoteParentPath) throws RepoNotAvailableException {
		return getRemoteFolder(host, remoteParentPath, false);
	}

	private Folder getRemoteFolder(Host host, Path remoteParentPath, boolean create) throws RepoNotAvailableException {
		//log.trace( "getOrCreate: " + remoteParentPath );
		Folder f = host;
		if (remoteParentPath != null) {
			for (String childName : remoteParentPath.getParts()) {
				try {
					if (childName.equals("_code")) {
						f = new Folder(f, childName, getCache());
					} else {
						Resource child = f.child(childName);
						if (child == null) {
							if (create) {
								f = f.createFolder(childName);
							} else {
								return null;
							}
						} else if (child instanceof Folder) {
							f = (Folder) child;
						} else {
							log.warn("Can't upload. A resource exists with the same name as a folder, but is a file: " + remoteParentPath);
							return null;
						}
					}
				} catch (HttpException e) {
					throw new RepoNotAvailableException(e);
				} catch (IOException ex) {
					throw new RepoNotAvailableException(ex);
				}
			}
		}
		return f;
	}

	@Override
	public boolean isOffline() {
		return offline;
	}

	@Override
	public void setOffline(boolean b) {

		boolean changed = (b != this.offline);

		this.offline = b;

		if (changed) {
			EventUtils.fireQuietly(new RepoChangedEvent(this));
		}
	}

	@Override
	public void setCurrent(QueueItem item) {
		this.current = item;
	}

	@Override
	public QueueItem getCurrent() {
		return current;
	}

	@Override
	public void setJob(Job j) {
		this.job = j;
	}

	/**
	 * read the quota fields from the response and update the account info
	 * @param remote 
	 */
	private void updateAccountInfo(Resource remote) {
		Long avail = remote.getQuotaAvailableBytes();
		Long used = remote.getQuotaUsedBytes();
		log.trace("updateAccountInfo: " + avail + " - " + used);
		if (used != null) {
			accountUsedBytes = used;
		}
		if (avail != null && used != null) {
			this.maxBytes = avail + used;
		} else {
			this.maxBytes = null;
		}
	}

	@Override
	public Long getMaxBytes() {
		return maxBytes;
	}

	@Override
	public Long getAccountUsedBytes() {
		return accountUsedBytes;
	}

	/**
	 * @return the host
	 */
	public String getHostName() {
		return hostName;
	}

	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
		job.config.onChildChanged();
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
		job.config.onChildChanged();
	}

	/**
	 * @return the pwd
	 */
	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
		job.config.onChildChanged();
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
		job.config.onChildChanged();
	}

	/**
	 * @return the useSSL
	 */
	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
		job.config.onChildChanged();
	}

	@Override
	public boolean isSync() {
		// disabled until properly tested!
		//return sync;
		return false;
	}

	public void setSync(boolean sync) {
		this.sync = sync;
		job.config.onChildChanged();
	}

	@Override
	public Long getBackedUpBytes() {
		if (backedupBytes != null) {
			return backedupBytes + backedupSinceLastScanBytes;
		} else {
			if (scanningBackedupBytes == null) {
				if (backedupSinceLastScanBytes > 0) {
					return backedupSinceLastScanBytes;
				} else {
					return null;
				}
			} else {
				return scanningBackedupBytes;
			}
		}
	}

	@Override
	public String getDescription() {
		return this.hostName;
	}

	@Override
	public boolean ping() {
		if( this.hostName == null || this.hostName.length() == 0 ) {
			log.trace("Not configured");
			return false;
		}
		// Note that we MUST not call through to the host to do a check if there
		// is a task in progress because the call will block on the Host methods
		// And, if there is a task running we must be online
		if (current != null) {
			return true;
		}
		try {
			try {
				if (this.maxBytes == null) {
					Resource r = this.host(true);
					updateAccountInfo(r);
					return true;
				} else {
					this.host().options("/");
					return true;
				}
			} catch (RepoNotAvailableException ex) {
				log.trace("repo exeption", ex);
				return false;
			}
		} catch (NotFoundException ex) {
			log.trace("not found");
			return false;
		} catch (ConnectException ex) {
			log.trace("couldnt connect to: " + this.hostName + ":" + this.port, ex);
			return false;
		} catch (UnknownHostException ex) {
			log.trace("couldnt connect, unknown host", ex);
			return false;
		} catch (SocketTimeoutException ex) {
			log.trace("couldnt connect, socket timeout", ex);
			return false;
		} catch (Unauthorized ex) {
			log.trace("authorisation failure", ex);
			return false;
		} catch (Throwable e) {
			log.trace("unknown exception", e);
			return false;
		}
	}

	@Override
	public boolean isExcludedFile(File child, Root root) {
		return _(Engine.class).isExcludedFolder(child.getParentFile(), root);
	}

	/**
	 * @return the proxyUseSystem
	 */
	public boolean isProxyUseSystem() {
		return proxyUseSystem;
	}

	/**
	 * @param proxyUseSystem the proxyUseSystem to set
	 */
	public void setProxyUseSystem(boolean proxyUseSystem) {
		this.proxyUseSystem = proxyUseSystem;
	}

	/**
	 * @return the proxyHost
	 */
	public String getProxyHost() {
		return proxyHost;
	}

	/**
	 * @param proxyHost the proxyHost to set
	 */
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	/**
	 * @return the proxyPort
	 */
	public int getProxyPort() {
		return proxyPort;
	}

	/**
	 * @param proxyPort the proxyPort to set
	 */
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * @return the proxyUserName
	 */
	public String getProxyUserName() {
		return proxyUserName;
	}

	/**
	 * @param proxyUserName the proxyUserName to set
	 */
	public void setProxyUserName(String proxyUserName) {
		this.proxyUserName = proxyUserName;
	}

	/**
	 * @return the proxyPassword
	 */
	public String getProxyPassword() {
		return proxyPassword;
	}

	/**
	 * @param proxyPassword the proxyPassword to set
	 */
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	/**
	 * Just flush the DAV folder to avoid too much memory consumption
	 * 
	 * @param filePath
	 * @param localRootPath
	 * @param repoName
	 * @throws RepoNotAvailableException
	 */
	@Override
	public void onScanDirComplete(String filePath, String localRootPath, String repoName) throws RepoNotAvailableException {
		String path = _(PathMunger.class).munge(filePath, localRootPath, repoName);

		Path p = Path.path(path);
		if (isCodeBehind(p)) {
			p = codeBehindToPage(p);
			path = p.toString();
		} else if (isSource(p)) {
			p = sourceToPage(p);
			path = p.toString();
		}

		try {
			Resource remote = host().find(path);
			if (remote instanceof Folder) {
				Folder f = (Folder) remote;
				f.flush();
			}
		} catch (HttpException e) {
			throw new RepoNotAvailableException(e);
		} catch (IOException e) {
			throw new RepoNotAvailableException(e);
		}
	}

	private boolean isSameParent(Path pSrc, Path pDest) {
		if (pDest.getParent() == null) {
			return pSrc.getParent() == null;
		} else {
			return pDest.getParent().equals(pSrc.getParent());
		}
	}

	public void setPwd(char[] password) {
		setPwd(new String(password));
	}

	@Override
	public boolean isConfigured() {
		return (hostName != null && hostName.length() > 0 ) &&
				(user != null && user.length() > 0);
	}
}
