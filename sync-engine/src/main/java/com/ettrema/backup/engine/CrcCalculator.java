package com.ettrema.backup.engine;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

/**
 *
 * @author brad
 */
public class CrcCalculator {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CrcCalculator.class);

	public long getLocalCrcForDirectory(List<StateToken> tokens) {
		NullOutputStream nulOut = new NullOutputStream();
		return generateLocalCrcForDirectory(tokens, nulOut);
	}

	public String getLocalCrcForDirectoryData(List<StateToken> tokens) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		long crc = generateLocalCrcForDirectory(tokens, bout);
		return bout.toString() + "\n ==> " + crc;
	}
	
	public long generateLocalCrcForDirectory(List<StateToken> tokens, OutputStream out) {
		CheckedOutputStream cout = new CheckedOutputStream(out, new Adler32());
		try {
			for (StateToken t : tokens) {
				String name = getName(t);
				String line = name + ":" + t.currentCrc + '\n';
				cout.write(line.getBytes());
			}
			return cout.getChecksum().getValue();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	public long getLocalCrc(File file) {
		long tm = System.currentTimeMillis();
		FileInputStream fin = null;
		BufferedInputStream bufin = null;
		try {
			fin = new FileInputStream(file);
			bufin = new BufferedInputStream(fin);
			CheckedInputStream cin = new CheckedInputStream(bufin, new CRC32());
			NullOutputStream out = new NullOutputStream();
			IOUtils.copy(cin, out);
			Checksum chk = cin.getChecksum();
			tm = System.currentTimeMillis() - tm;
			log.trace("calculated checksum in: " + tm + "ms");
			return chk.getValue();

		} catch (FileNotFoundException ex) {
			throw new RuntimeException(ex);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			IOUtils.closeQuietly(bufin);
			IOUtils.closeQuietly(fin);
		}
		//CheckedInputStream cin = new CheckedInputStream( in, new CRC32() );
	}

	private String getName(StateToken t) {
		File f = new File(t.filePath);
		return f.getName();
	}
}
