package org.duckdns.comms;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Listener extends Thread {
	ServerSocket listen_socket;
	int port;
	Service service;
	Set<IncomingConnection> connections;
	int maxConnections;
	ThreadGroup threadGroup;
	volatile boolean stop = false;

	public Listener(ThreadGroup group, int port, Service service,int maxConnections) throws IOException {
		super(group, "uk.co.ee.maps.comms.Listener : " + port + " - " + service.getServiceName());
		listen_socket = new ServerSocket(port);
		// give it a non-zero timeout so accept() can be interrupted
		listen_socket.setSoTimeout(600000);
		this.port = port;
		this.service = service;
		this.maxConnections = maxConnections;
		this.connections = new HashSet<IncomingConnection>(maxConnections);
		this.threadGroup = group;
	}

	public void pleaseStop() {
		this.stop = true; // Set the stop flag
		this.interrupt(); // Stop blocking in accept()
		try {
			listen_socket.close();
		} // Stop listening.
		catch (IOException e) {
		}
	}

	public void run() {
		while (!stop) { // loop until we're asked to stop.
			try {
				Socket client = listen_socket.accept();
				this.addConnection(client);
			} catch (InterruptedIOException e) {
			} catch (IOException e) {
				// log(e);
			}
		}
	}

	protected synchronized void addConnection(Socket s) {
		// If the connection limit has been reached
		if (connections.size() >= maxConnections) {
			try {
				// Then tell the client it is being rejected.
				PrintWriter out = new PrintWriter(s.getOutputStream());
				out.print("Connection refused; the server is busy; please try again later.\n");
				out.flush();
				s.close();
				// log("Connection refused to " +
				// s.getInetAddress().getHostAddress() + ":" + s.getPort() +
				// ": max connections reached.");
			} catch (IOException e) {
				// log(e);
			}
		} else {
			IncomingConnection c = new IncomingConnection(s, this);
			connections.add(c);
			// log("Connected to " + s.getInetAddress().getHostAddress() + ":" +
			// s.getPort() + " on port " + s.getLocalPort() + " for service " +
			// service.getClass().getName());
			c.start();
		}
	}

	protected synchronized void endConnection(IncomingConnection c) {
		connections.remove(c);
		// log("Connection to " + c.client.getInetAddress().getHostAddress() +
		// ":" + c.client.getPort() + " closed.");
	}

	public synchronized void setMaxConnections(int max) {
		maxConnections = max;
	}

}
