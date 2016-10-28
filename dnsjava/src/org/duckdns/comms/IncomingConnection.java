package org.duckdns.comms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class IncomingConnection extends Thread {
	Socket client; // The socket to talk to the client through
	Service service; // The service being provided to that client
	Listener listener;

	public IncomingConnection(Socket client, Listener listener) {
		super("Server.Connection:" + client.getInetAddress().getHostAddress() + ":" + client.getPort());
		this.client = client;
		this.listener = listener;
		this.service = listener.service;
	}

	public void run() {
		try {
			InputStream in = client.getInputStream();
			OutputStream out = client.getOutputStream();
			service.serve(in, out);
		} catch (IOException e) {
			// log(e);
		} finally {
			listener.endConnection(this);
		}
	}

}