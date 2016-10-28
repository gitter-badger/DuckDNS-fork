package org.duckdns.comms;

import java.util.HashSet;
import java.util.Set;

public class Sender {
	Set<OutgoingConnection> connections;
	int port;
	int maxConnections;
	int maxRetries;
	Client client;
	long sleepTimeIfFull;
	String destination;
	
	public int getPort() {
		return port;
	}

	public String getDestination() {
		return destination;
	}

	public Sender(String destination, int port, int maxRetries, int maxConnections, long sleepTimeIfFull, Client client) {
		this.client = client;
		this.port = port;
		this.maxRetries = maxRetries;
		this.maxConnections = maxConnections;
		this.sleepTimeIfFull = sleepTimeIfFull;
		this.connections = new HashSet<OutgoingConnection>(maxConnections);
		this.destination = destination;
	}
	
	public void send(Message theMessage) {
		if (connections.size() < maxConnections) {
			OutgoingConnection c = new OutgoingConnection(this.destination, this.port, this.client, theMessage, this);
			connections.add(c);
			c.start();
		}
	}
	
	public String sendSynchronus(Message theMessage) {
		int attempt = 1; 
		while (attempt <= maxRetries) {
			if (connections.size() < maxConnections) {
				// System.out.println("attempt : " + attempt);
				try {
					OutgoingConnection c = new OutgoingConnection(this.destination, this.port, this.client, theMessage, this);
					connections.add(c);
					// log("Connected to " + s.getInetAddress().getHostAddress() + ":" +
					// s.getPort() + " on port " + s.getLocalPort() + " for service " +
					// service.getClass().getName());
					return c.doSend();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			attempt ++;
			try {
				Thread.sleep(this.sleepTimeIfFull);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return "ERROR OUT OF RETRIES";
	}
	
	protected synchronized void endConnection(OutgoingConnection c) {
		connections.remove(c);
		// System.out.println("Connection to " + c.host + ":" + c.port + " closed.");
	}
	
	
}
