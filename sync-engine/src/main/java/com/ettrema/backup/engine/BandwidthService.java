package com.ettrema.backup.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates an averaged bandwidth based on the given samples
 *
 * @author brad
 */
public class BandwidthService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( BandwidthService.class );

    private static int MAX_SIZE = 100;
    private List<Sample> samples = new ArrayList<Sample>();
    private long totalBytes;
    private long totalTimeMs;

    public BandwidthService() {
    }



    /**
     * Update the running average bandwidth
     *
     * @param n
     * @return
     */
    public synchronized int getBytesPerSec() {
        if( totalTimeMs == 0 ) {
            return 0;
        } else {
            int av = (int)(totalBytes / totalTimeMs);
            //log.trace("runningtotal: " + totalBytes + " / samples:" + totalTimeMs + " = " + av);
            return av * 1000; // totalTimeMs is millis, so x 1000 to make it seconds
        }
    }

    public void sample(long bytes, long timeMs) {
        //log.trace( "sample: " + bytes + " - " + timeMs + "ms");
        samples.add( new Sample( bytes, timeMs));
        totalBytes += bytes;
        totalTimeMs += timeMs;
        if( samples.size() > MAX_SIZE) {
            Sample s = samples.remove( 0 );
            totalBytes -= s.bytes;
            totalTimeMs -= s.timeMs;
        }
    }

    /**
     * A sample is a number of bytes uploaded in a period of time
     */
    private class Sample {
        final long bytes;
        final long timeMs;

        public Sample( long bytes, long timeMs ) {
            this.bytes = bytes;
            this.timeMs = timeMs;
        }
    }

}
