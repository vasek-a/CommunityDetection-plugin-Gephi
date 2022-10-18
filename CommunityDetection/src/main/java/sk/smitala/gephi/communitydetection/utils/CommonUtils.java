/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.utils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 *
 * @author smitalm
 */
public class CommonUtils {

    public static void open(URI uri) {
	if (Desktop.isDesktopSupported()) {
	    try {
		Desktop.getDesktop().browse(uri);
	    } catch (IOException e) {
		System.err.println("Exception raised while opening URL: " + e.getMessage());
//		e.printStackTrace();
	    }
	} else {
	    System.out.println("Desktop is not supported, thus cannot open URL.");
	}
    }
}
