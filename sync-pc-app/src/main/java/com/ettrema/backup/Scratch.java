/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ettrema.backup;

import java.io.*;
import java.net.*;
import java.util.*;
import static java.lang.System.out;

/**
 *
 *
 *  http://sourceforge.net/projects/jwlanscan/
 *
 * http://www.placelab.org/toolkit/doc/javadoc/org/placelab/core/WiFiReading.html
 * 
 * @author brad
 */
public class Scratch
{
    public static void main(String args[]) throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        for (NetworkInterface netIf : Collections.list(nets)) {
            out.printf("Display name: %s\n", netIf.getDisplayName());
            out.printf("Name: %s\n", netIf.getName());
            out.printf("hw: %s\n", netIf.getHardwareAddress() );
            displaySubInterfaces(netIf);
            out.printf("\n");
        }
    }

    static void displaySubInterfaces(NetworkInterface netIf) throws SocketException {
        Enumeration<NetworkInterface> subIfs = netIf.getSubInterfaces();

        for (NetworkInterface subIf : Collections.list(subIfs)) {
            out.printf("\tSub Interface Display name: %s\n", subIf.getDisplayName());
            out.printf("\tSub Interface Name: %s\n", subIf.getName());
        }
     }
}
