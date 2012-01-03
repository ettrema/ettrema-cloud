package com.ettrema.backup.engine;

import com.ettrema.backup.config.Dir;
import com.ettrema.backup.config.Root;
import java.io.File;
import java.util.List;

/**
 *
 * @author brad
 */
public class SyncExclusionsService {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( SyncExclusionsService.class );
	
    public boolean isBackupable( File child, Root root ) {
        File f = child;
        if( f.isFile() && f.length() == 0 ) {
            log.trace( "not uploading empty file: " + f.getAbsolutePath() );
            return false;
        }
        if( f.isFile() && f.getName().contains( ".conflicted." ) ) {
            log.trace( "not uploading conflict file: " + f.getAbsolutePath() );
            return false;
        }
        while( f.getAbsolutePath().startsWith( root.getFullPath() ) ) {
            if( f.getName().equals( "Thumb.db" ) ) {
                log.trace( "is thumbs.db" );
                return false;
            }
            if( f.isHidden() ) {
                //log.trace( "ishidden" );
                return false;
            }
            if( f.getName().startsWith( "." ) ) {
                log.trace( "starts with ." );
                return false;
            }
            if( f.getName().endsWith( ".tmp" ) ) {
                log.trace( "ends with .tmp" );
                return false;
            }
            if( f.getName().startsWith( "~" ) ) {
                log.trace( "starts with tilda" );
                return false;
            }
            f = f.getParentFile();
        }

        return true;
    }	
	
    public boolean isExcludedFolder( File dir, Root root ) {
        if( dir.getAbsolutePath().equals( root.getFullPath() ) ) {
            // can't exclude a root
            return false;
        }
        List<Dir> exclusions = root.getExclusions();
        String s = dir.getAbsolutePath();
        for( Dir d : exclusions ) {
            if( s.startsWith( d.getFullPath() ) ) {
                return true;
            }
        }
        return false;
    }	
}
