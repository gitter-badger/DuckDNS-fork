package org.duckdns.comms.clients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.duckdns.comms.Client;
import org.duckdns.comms.Message;

public class BasicSender implements Client {

	@Override
	public String send(InputStream in, OutputStream out, Message message) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		message.pushMessage(out);
		String line = br.readLine();
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		while (line != null) {
			sb.append(line);
			if (!first) {
				sb.append("\n");
			} else {
				first = false;
			}
			line = br.readLine();
		}
		//System.out.println("GOT RESPONSE " + sb.toString());
		in.close();
		out.close();
		return sb.toString();
	}

}
