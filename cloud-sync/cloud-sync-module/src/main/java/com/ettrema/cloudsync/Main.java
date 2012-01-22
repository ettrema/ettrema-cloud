package com.ettrema.cloudsync;

/**
 * For running cloud-sync directly, not inside a GUI
 *
 * @author brad
 */
public class Main {

    public static void main(String[] args) throws Exception {
        try {
            ModuleFactory factory = ModuleFactory.get();
            factory.startAll();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(9);
        }
    }
}
