package com.ettrema.backup.engine;

import com.ettrema.backup.config.Job;
import com.ettrema.backup.config.Root;
import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.contentobjects.jnotify.JNotifyListener;

/**
 * Listens for filesystem events. Looks for sequences of events which represent a single
 * action
 *
 * On Windows 7:
 *
 * Move
 * File/Folder X moved from folder A to folder B:
 *  - delete A/X
 *  - create B/X
 *  - modified B (ignore)
 *  - modified A (ignore)
 *
 * Renamed
 * File/Folder x is renamed to x1
 *  - renamed old name - > new name
 *  - modified (parent dir) (ignore)
 *
 * Modified
 * If a file X is modified
 * - modified X
 * - modified X
 *
 * Deleted
 *  - deleted A/X
 *  - modified A (ignore)
 *
 * Created
 *  - created A/X
 *  - modified A (ignore)
 *
 * Rules
 * -----
 *
 * 1. Always ignore modified on directories
 * 2. A delete becomes a move if there is a corresponding create. Match on folder name (not path)
 * 3. Merge file modified events
 * 4. A create becomes a move if there is a corresponding delete. Match on folder name (not path)
 *
 * Notes
 * ------
 * The "name" of a file or folder is NOT given in the JNotify arguments. The name
 * is just the path following the root path
 *
 *
 * @author brad
 */
public class WatchJob implements JNotifyListener {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( WatchJob.class );
    private static final long TIME_TO_LIVE = 500;
    private final Job job;
    private final Root root;
    private final java.util.Queue<WatchEvent> watchEvents = new ConcurrentLinkedQueue<WatchEvent>();
    private boolean disabled;

    public WatchJob( Job job, Root root ) {
        this.job = job;
        this.root = root;
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay( new Runnable() {

			@Override
            public void run() {
                filterEvents();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS );
    }

	@Override
    public void fileCreated( int wd, String rootPath, String name ) {
        if( disabled ) {
            return;
        }
        log.debug( "file created: " + rootPath + " : " + name );
        watchEvents.add( new CreatedWatchEvent( rootPath, name ) );
    }

	@Override
    public void fileDeleted( int wd, String rootPath, String name ) {
        if( disabled ) {
            return;
        }

        log.debug( "deleted " + rootPath + " : " + name );
        // when deleted,discard any previous modified events
        DeletedWatchEvent de = new DeletedWatchEvent( rootPath, name );
        de.removeModifiedEvents( de.getFullPath() );
        watchEvents.add( de );

    }

	@Override
    public void fileModified( int wd, String rootPath, String name ) {
        if( disabled ) {
            return;
        }

        log.debug( "modified " + rootPath + " : " + name );
        try {
            String fullPath = rootPath + File.separator + name;
            File f = new File( fullPath );
            if( f.isDirectory() ) {
                log.debug( "modified directory, ignore" );
                return;
            }
            watchEvents.add( new ModifiedWatchEvent( rootPath, name ) );
        } catch( Throwable e ) {
            log.error( rootPath, e );
        }
    }

	@Override
    public void fileRenamed( int wd, String rootPath, String name, String newName ) {
        if( disabled ) {
            return;
        }
        log.debug( "renamed " + rootPath + " : " + name + "  newName:" + newName );
        watchEvents.add( new RenamedWatchEvent( rootPath, name, newName ) );
    }

    private void filterEvents() {
        long expiredTime = System.currentTimeMillis() - TIME_TO_LIVE;
        WatchEvent e = watchEvents.peek();
        while( e != null && e.timeCreated < expiredTime ) {
            try {
                e = watchEvents.poll(); // will remove the head
                if( e != null ) {
                    log.trace( "processing event: " + e.getClass().getCanonicalName() + " - tm: " + e.timeCreated );
                    e.visit();
                } else {
                    log.debug( "poll removed null entry" );
                }

                // check for next
                e = watchEvents.peek();
            } catch( Throwable ex ) {
                log.error( "Exception processing: " + e, ex );
            }
        }
    }

    private void onNewFile( File newFile ) {
        root.onFileModified( newFile );
    }

    private void onMoved( String fullPathFrom, File dest ) {
        log.trace( "onMoved: " + fullPathFrom + " -> " + dest.getAbsolutePath() );
		if( fullPathFrom.equals(dest.getAbsolutePath())) {
			log.trace("Didnt actually move, same location");
			return ;
		}
        root.onMoved( fullPathFrom, dest );
    }

    private void onDeleted( File file ) {
        log.info( "onDeleted: " + file.getAbsolutePath() );
        root.onFileDeleted( file );
    }

    public void setDisabled( boolean disabled ) {
        this.disabled = disabled;
    }

    public boolean isDisabled() {
        return disabled;
    }

    private abstract class WatchEvent {

        final String rootPath;
        final String name;
        final long timeCreated;

        abstract void visit();

        public WatchEvent( String rootPath, String name ) {
            this.rootPath = rootPath;
            this.name = name;
            timeCreated = System.currentTimeMillis();
        }

        File getFile() {
            File f = new File( getFullPath() );
            return f;
        }

        String getFullPath() {
            return rootPath + File.separator + name;
        }

        void removeModifiedEvents( String path ) {
            // Remove any modified events for the same file
            Iterator<WatchEvent> it = watchEvents.iterator();
            while( it.hasNext() ) {
                WatchEvent e = it.next();
                if( e instanceof ModifiedWatchEvent ) {
                    ModifiedWatchEvent mwe = (ModifiedWatchEvent) e;
                    if( mwe.getFullPath().equals( path ) ) {
                        log.trace( "consumer redundant event" );
                        it.remove();
                    }
                }
            }
        }
    }

    private class CreatedWatchEvent extends WatchEvent {

        public CreatedWatchEvent( String rootPath, String name ) {
            super( rootPath, name );
        }

        @Override
        void visit() {
            log.trace( "visit created" );
            // scan queue looking for delete events on the same name. If one
            // is found remove it and generate a move event. If not found generate
            // a new file event
            String resourceName = getFile().getName();
            DeletedWatchEvent found = null;
            for( WatchEvent e : watchEvents ) {
                if( e instanceof DeletedWatchEvent ) {
                    // Check to see if the deleted name exactly matches the created name
                    DeletedWatchEvent dwe = (DeletedWatchEvent) e;
                    String deletedName = dwe.getFile().getName();
                    if( resourceName.equals( deletedName ) ) {
                        // Got a match
                        found = dwe;
                        break;
                    }
                }
            }
            removeModifiedEvents( getFullPath() );
            if( found == null ) {
                onNewFile( getFile() );
            } else {
                watchEvents.remove( found );
                onMoved( found.getFullPath(), getFile() );
            }
        }
    }

    private class DeletedWatchEvent extends WatchEvent {

        public DeletedWatchEvent( String rootPath, String name ) {
            super( rootPath, name );
        }

        @Override
        void visit() {
            log.trace( "visit deleted" );
            // scan queue looking for create events. If found generate a
            // move event, otherwise a delete event
            String resourceName = getFile().getName();
            CreatedWatchEvent found = null;
            for( WatchEvent e : watchEvents ) {
                if( e instanceof CreatedWatchEvent ) {
                    // Check to see if the deleted name exactly matches the created name
                    CreatedWatchEvent cwe = (CreatedWatchEvent) e;
                    String deletedName = cwe.getFile().getName();
                    if( resourceName.equals( deletedName ) ) {
                        // Got a match
                        found = cwe;
                        break;
                    }
                }
            }
            removeModifiedEvents( getFullPath() );
            if( found == null ) {
                onDeleted( getFile() );
            } else {
                watchEvents.remove( found );
                onMoved( getFullPath(), found.getFile() );
            }

        }
    }

    private class ModifiedWatchEvent extends WatchEvent {

        public ModifiedWatchEvent( String rootPath, String name ) {
            super( rootPath, name );
        }

        @Override
        void visit() {
            log.trace( "visit modified" );
            removeModifiedEvents( getFullPath() );
            root.onFileModified( getFile() );
        }
    }

    /**
     *
     *  Eg
     * rootPath - C:\Users\brad\Pictures
     * name - zoo\temp\New Text Document.txt
     * newName - zoo\temp\aaNew Text Document.txt
     *
     */
    private class RenamedWatchEvent extends WatchEvent {

        final String newName;

        public RenamedWatchEvent( String rootPath, String name, String newName ) {
            super( rootPath, name );
            this.newName = newName;
        }

        String getDestPath() {
            return rootPath + File.separator + newName;
        }

        @Override
        void visit() {
            log.trace( "visit renamed" );

            // Consume any modified events for the dest file
            Iterator<WatchEvent> it = watchEvents.iterator();
            removeModifiedEvents( getFullPath() );
            removeModifiedEvents( getDestPath() );

            File dest = new File( getDestPath() );
            onMoved( getFullPath(), dest );
        }
    }
}

