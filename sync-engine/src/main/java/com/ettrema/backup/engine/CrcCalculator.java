package com.ettrema.backup.engine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;


/**
 *
 * @author brad
 */
public class CrcCalculator {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( CrcCalculator.class );

    public long getLocalCrc(File file) {
        if( file.isDirectory() ) {
            return getLocalCrcDirectory(file);
        } else {
            return getLocalCrcFile(file);
        }
    }
    
    public long getLocalCrcDirectory(File file) {
        throw new RuntimeException("todo");
    }
    
    public long getLocalCrcFile(File file) {
        long tm = System.currentTimeMillis();
        FileInputStream fin = null;
        BufferedInputStream bufin = null;
        try {
            fin = new FileInputStream( file );
            bufin = new BufferedInputStream( fin );
            CheckedInputStream cin = new CheckedInputStream( bufin, new CRC32() );
            NullOutputStream out = new NullOutputStream();
            IOUtils.copy( cin, out );
            Checksum chk = cin.getChecksum();
            tm = System.currentTimeMillis() - tm;
            log.trace( "calculated checksum in: " + tm + "ms");
            return chk.getValue();

        } catch(FileNotFoundException ex) {
            throw new RuntimeException( ex );
        } catch(IOException ex) {
            throw new RuntimeException( ex );
        } finally{
            IOUtils.closeQuietly( bufin);
            IOUtils.closeQuietly( fin);
        }
        //CheckedInputStream cin = new CheckedInputStream( in, new CRC32() );
    }

}
