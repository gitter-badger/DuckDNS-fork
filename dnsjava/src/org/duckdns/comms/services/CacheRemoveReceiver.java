package org.duckdns.comms.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.UUID;

import org.duckdns.cache.LocalMemoryCache;
import org.duckdns.comms.Service;
import org.duckdns.comms.messages.SimpleOneLineMessage;
import org.duckdns.utils.GoogleAnalyticsHelper;

public class CacheRemoveReceiver implements Service {
	
	private String name = "";
	private LocalMemoryCache localCache;
	
	public CacheRemoveReceiver(String name, LocalMemoryCache theCache) {
		this.name = name;
		this.localCache = theCache;
	}
	
	public void serve(InputStream i, OutputStream o) throws IOException {
		PrintWriter out = new PrintWriter(o);
		StringBuffer sb = new StringBuffer(); 
	    try {
			ObjectInputStream s = new ObjectInputStream(i);
			SimpleOneLineMessage message = (SimpleOneLineMessage) s.readObject();
			// Bosh an Item!
			//System.out.println("Removing:"+message.getMessage()+":");
			GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","clearcache",message.getMessage(),"1");
			localCache.removeForDomain(message.getMessage());
			out.print("success\n");
	        out.flush();
	        //System.out.println("WORKED");
		} catch (IOException e) {
			out.print("fail\n");
	        out.flush();
	        GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","clearcachefail1","unknown","1");
	        //System.out.println("ERR1" + e);
		} catch (ClassNotFoundException e) {
			out.print("fail\n");
	        out.flush();
	        GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","clearcachefail2","unknown","1");
	        //System.out.println("ERR2" + e);
		} finally {
			out.close();
			i.close();
		}
	}

	public String getServiceName() {
		return name;
	}

	public String getServiceStatus() {
		return "Alive!";
	}


}
