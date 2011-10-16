package com.ettrema.backup.engine;

import com.ettrema.backup.config.Config;
import com.ettrema.httpclient.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ettrema.backup.engine.Services._;

/**
 *
 * 100% throttle mean no slow down
 *
 * 0% means dwell for 10 times the time spent uploading
 *
 * @author brad
 */
public class ThrottleFactory {

    private static final Logger log = LoggerFactory.getLogger( ThrottleFactory.class );
    private final BandwidthService bandwidthService;

    public ThrottleFactory( BandwidthService bandwidthService ) {
        this.bandwidthService = bandwidthService;
    }

    public BandwidthService getBandwidthService() {
        return bandwidthService;
    }

    

    public ProgressListener createThrottle(long totalBytes, ProgressListener wrapped) {
        return new UiThrottle(totalBytes, wrapped);
    }

    public boolean isPaused() {
        return _(Config.class).isPaused();
    }

    public int throttleVal() {
        return _(Config.class).getThrottlePerc();
    }

    public class UiThrottle implements ProgressListener {
		private final ProgressListener wrapped;
        private final long totalBytes;
        private long lastSampleTime;
        private long lastSleepTime;
        private long bytesSinceLastCheck;
        private long bytesSinceLastSample;

        public UiThrottle(long totalBytes, ProgressListener wrapped) {
			this.wrapped = wrapped;
            this.totalBytes = totalBytes;
            lastSampleTime = System.currentTimeMillis();
            lastSleepTime = System.currentTimeMillis();
        }

        private void checkThrottle(int len) {
//            log.trace("onRead: " + len + " - " + bytesSinceLastCheck);
            bytesSinceLastCheck += len;
            if( bytesSinceLastCheck < 4000 ) { // do at least 4k chunks
//                log.trace( "onRead: not enough data: " + bytesSinceLastCheck);
                return;
            }
            bytesSinceLastCheck = 0;

            int v = throttleVal();
            if( v < 95 ) { // only sleep if not close to 100
                int dwellFactor = ( 100 - v ) * 10;
//                log.trace( "dwellFactor: " + dwellFactor);

                if( dwellFactor > 0 ) {
                    long timeSinceLastSleep = System.currentTimeMillis() - lastSleepTime;
                    if( timeSinceLastSleep < 10 ) {
                        return ;
                    }
                    long dwellTime = dwellFactor * timeSinceLastSleep / 100;
                    lastSampleTime = System.currentTimeMillis();

                    // don't dwell longer then 1sec
                    if( dwellTime > 1000 ) {
                        dwellTime = 1000;
                    }
                    try {
                        Thread.sleep( dwellTime );
                    } catch( InterruptedException ex ) {
                    }
                } else {
                }
            }
            lastSleepTime = System.currentTimeMillis();

        }

        private void checkBandwidth(int len) {
            bytesSinceLastSample += len;
            long timeSinceLastSample = System.currentTimeMillis() - lastSampleTime;
            if( bytesSinceLastSample < totalBytes/2) { // if transfer rates are high we might miss it
                if( timeSinceLastSample < 1000 ) {
                    return ;
                }
            }

            // send the sample
            bandwidthService.sample( bytesSinceLastSample, timeSinceLastSample );

            // reset sampling counters
            lastSampleTime = System.currentTimeMillis();
            bytesSinceLastSample = 0;
        }

		@Override
		public void onRead(int bytes) {
            checkThrottle(bytes);
            checkBandwidth(bytes);
			wrapped.onRead(bytes);
		}

		@Override
		public void onProgress(long bytesRead, Long totalBytes, String fileName) {
			wrapped.onProgress(bytesRead, totalBytes, fileName);
		}

		@Override
		public void onComplete(String fileName) {
			wrapped.onProgress(totalBytes, totalBytes, fileName);
		}

		@Override
		public boolean isCancelled() {
			return wrapped.isCancelled();
		}
    }
}
