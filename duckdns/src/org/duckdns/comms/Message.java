package org.duckdns.comms;

import java.io.IOException;
import java.io.OutputStream;

public interface Message {
	public void pushMessage(OutputStream o) throws IOException;
	public long getMessageCreationTimeStamp();
}
