package org.duckdns.comms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Note that implementations of this interface must have a no-argument
 * constructor if they are to be dynamically instantiated by the main()
 * method of the Server class.
*/
public interface Service {
	public void serve(InputStream in, OutputStream out) throws IOException;
	public String getServiceName();
	public String getServiceStatus();
}