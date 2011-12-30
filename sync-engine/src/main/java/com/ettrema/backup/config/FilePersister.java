package com.ettrema.backup.config;

import com.ettrema.backup.config.DavRepo.DavRepoState;
import com.ettrema.backup.queue.DeletedFileQueueItem;
import com.ettrema.backup.queue.MovedQueueItem;
import com.ettrema.backup.queue.NewFileQueueItem;
import com.ettrema.backup.queue.RemotelyDeletedQueueItem;
import com.ettrema.backup.queue.RemotelyModifiedQueueItem;
import com.ettrema.backup.queue.RemotelyMovedQueueItem;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class FilePersister {

	private static final Logger log = LoggerFactory.getLogger(FilePersister.class);
	
	public Properties loadPropertiesFromFile(File fData) throws RuntimeException {
		Properties props = new Properties();
		if (fData.exists()) {
			FileInputStream fin = null;
			try {
				try {
					fin = new FileInputStream(fData);
					props.load(fin);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			} finally {
				IOUtils.closeQuietly(fin);
			}
		}
		return props;
	}
	
	public void savePropertiesToFile(Properties props, File fData) {
		FileOutputStream fout = null;
		try {
			try {
				fout = new FileOutputStream(fData);
				props.store(fout, null);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}			
		} finally {
			IOUtils.closeQuietly(fout);
		}
	}	
	
	public void saveToXml(Object o, File f) {
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(f);
		} catch (FileNotFoundException ex) {
			throw new RuntimeException(f.getAbsolutePath(), ex);
		}
		try {
			XStream xstream = initXstream();
			xstream.toXML(o, fout);
		} finally {
			IOUtils.closeQuietly(fout);
		}		
	}
	
	public Object loadFromXml(File f) {
		FileInputStream fin;
		try {
			fin = new FileInputStream(f);
			log.info("opened config file: " + f.getAbsolutePath());
		} catch (FileNotFoundException ex) {
			log.info("not found: " + f.getAbsolutePath());
			f.getParentFile().mkdirs();
			return null;
		}		
		try {
			XStream xstream = initXstream();
			return xstream.fromXML(fin);
		} catch(Exception e) {
			log.error("Failed to open file: " + f.getAbsolutePath(), e);
			f.delete();
			return null;
		} finally {
			IOUtils.closeQuietly(fin);
		}
	}
	
	private XStream initXstream() {
		XStream xstream = new XStream();
		initAliases(xstream);
		return xstream;
	}

	private void initAliases(XStream x) {
		x.alias("config", Config.class);
		x.alias("job", Job.class);
		x.alias("root", Root.class);
		x.alias("dir", Dir.class);
		x.alias("local", LocalRepo.class);
		x.alias("dav", DavRepo.class);
		x.alias("queue", Queue.class);
		x.alias("conList", CopyOnWriteArrayList.class);
		x.alias("davRepoState", DavRepoState.class);
		x.alias("remotelyModified", RemotelyModifiedQueueItem.class);
		x.alias("deleted", DeletedFileQueueItem.class);
		x.alias("moved", MovedQueueItem.class);
		x.alias("newOrUpdated", NewFileQueueItem.class);
		x.alias("remotelyDeleted", RemotelyDeletedQueueItem.class);
		x.alias("remotelyMoved", RemotelyMovedQueueItem.class);
	}
	
}
