package org.duckdns.comms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Client {
	public String send(InputStream in, OutputStream out, Message message) throws IOException;		
}
