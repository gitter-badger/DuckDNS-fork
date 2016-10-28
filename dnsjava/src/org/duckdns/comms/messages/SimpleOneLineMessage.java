package org.duckdns.comms.messages;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;

import org.duckdns.comms.Message;

public class SimpleOneLineMessage implements Message, Serializable {

	private static final long serialVersionUID = -4815047581037648032L;
	String message = "";
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	final long createdTimeStamp;
	
	public SimpleOneLineMessage(String message) {
		this.message = message;
		this.createdTimeStamp = new Date().getTime();
	}
	
	public void pushMessage(OutputStream out) throws IOException {
		ObjectOutputStream objectOutput = new ObjectOutputStream(out);
        objectOutput.writeObject(this);
        objectOutput.flush();
	}
	
	public long getMessageCreationTimeStamp() {
		return this.createdTimeStamp;
	}

}
