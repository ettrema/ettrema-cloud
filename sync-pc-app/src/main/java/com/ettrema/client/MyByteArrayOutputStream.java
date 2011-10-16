package com.ettrema.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author mcevoyb
 */
public class MyByteArrayOutputStream extends ByteArrayOutputStream {
    public MyByteArrayOutputStream() {
    }
    
    public void read(InputStream in) {
        try {
            int cnt;
            byte[] buffer = new byte[1024];
            while ((cnt = in.read(buffer)) > 0) {
                write(buffer,0,cnt);
            }
            close();
            in.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
