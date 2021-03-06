package com.ettrema.cloudsync.view;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.ettrema.backup.config.Config;
import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Repo;
import com.ettrema.backup.engine.TransferAuthorisationService;
import com.ettrema.backup.queue.QueueInserter;
import com.ettrema.backup.utils.PathMunger;
import com.ettrema.httpclient.Folder;
import com.ettrema.httpclient.GenericHttpException;
import com.ettrema.httpclient.HttpException;
import com.ettrema.httpclient.Resource;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author brad
 */
public class GuiTransferAuthorisationService extends javax.swing.JDialog implements TransferAuthorisationService {

    //private final List<TransferAuthorisation> queue = new CopyOnWriteArrayList<TransferAuthorisation>();
    private final QueueInserter queueInserter;
    private final PathMunger pathMunger;
    private final Config config;

    public GuiTransferAuthorisationService(QueueInserter queueInserter, PathMunger pathMunger, Config config) {
        initComponents();
        this.queueInserter = queueInserter;
        this.pathMunger = pathMunger;
        this.config = config;
    }

    @Override
    public void requestDownload(DavRepo repo, Resource remote) throws IOException, HttpException {
        Long bytesToDownload;
        try {
            bytesToDownload = calcBytesToDownload(remote);
        } catch (NotAuthorizedException ex) {
            throw new GenericHttpException(remote.encodedUrl(), ex);
        } catch (BadRequestException ex) {
            throw new GenericHttpException(remote.encodedUrl(), ex);
        }
        String sPath = pathMunger.findFileFromUrl(config.getAllRoots(), remote.encodedUrl(), File.separator);
        File local = new File(sPath);
        queueInserter.enqueueDownload(repo, local, bytesToDownload);
    }

    @Override
    public void resolveConflict(Resource r, File l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void requestUpload(File l) {
        for (Repo r : config.getAllRepos()) {
            queueInserter.enqueueUpload(r, l);
        }
    }

    @Override
    public void requestRemoteDelete(DavRepo repo, Resource r) {
        String sPath = pathMunger.findFileFromUrl(config.getAllRoots(), r.encodedUrl(), File.separator);
        File local = new File(sPath);
        queueInserter.enqueueRemoteDelete(local, repo);
    }

    @Override
    public void requestDeleteLocal(DavRepo repo,File local) {
        queueInserter.enqueueLocalDelete(local, repo);
    }

    private Long calcBytesToDownload(Resource remote) throws IOException, HttpException, NotAuthorizedException, BadRequestException {
        if (remote instanceof Folder) {
            Folder remoteFolder = (Folder) remote;
            return calcBytesToDownload(remoteFolder);
        } else {
            com.ettrema.httpclient.File remoteFile = (com.ettrema.httpclient.File) remote;
            return calcBytesToDownload(remoteFile);
        }
    }

    private Long calcBytesToDownload(com.ettrema.httpclient.File remoteFile) {
        return remoteFile.contentLength;
    }

    private Long calcBytesToDownload(Folder remoteFolder) throws IOException, HttpException, NotAuthorizedException, BadRequestException {
        long l = 0;
        for (Resource r : remoteFolder.children()) {
            l += calcBytesToDownload(r);
        }
        return l;
    }
//	private interface TransferAuthorisation {
//	}
//
//	private class UploadTransferAuthorisation implements TransferAuthorisation {
//
//		private final File file;
//
//		public UploadTransferAuthorisation(File file) {
//			this.file = file;
//		}
//	}
//
//	private class DownloadFileTransferAuthorisation implements TransferAuthorisation {
//
//		private final com.ettrema.httpclient.File file;
//
//		public DownloadFileTransferAuthorisation(com.ettrema.httpclient.File file) {
//			this.file = file;
//		}
//	}
//
//	private class DownloadFolderTransferAuthorisation implements TransferAuthorisation {
//
//		private final Folder folder;
//
//		public DownloadFolderTransferAuthorisation(Folder folder) {
//			this.folder = folder;
//		}
//	}    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
