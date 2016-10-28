package org.duckdns.util;

import java.util.HashSet;
import java.util.Set;

public class DomainNameFilterer {
	private static DomainNameFilterer instance = null;
	
	private Set<String> bannedList;
	
	protected DomainNameFilterer() {
		bannedList = new HashSet<String>();
		bannedList.add("www");
		bannedList.add("dns");
		bannedList.add("ns1");
		bannedList.add("ns2");
		bannedList.add("ns3");
		bannedList.add("ns4");
		bannedList.add("ns5");
		bannedList.add("www1");
		bannedList.add("www2");
		bannedList.add("admin");
		bannedList.add("support");
		bannedList.add("help");
		bannedList.add("login");
		bannedList.add("email");
		bannedList.add("mail");
		bannedList.add("webmail");
		bannedList.add("update");
		bannedList.add("secure");
		// STUFF WE WANT IN HERE FOR NOW
		bannedList.add("richard");
		bannedList.add("tracey");
		bannedList.add("luke");
		bannedList.add("cyberconiii");
		bannedList.add("steven");
		bannedList.add("rachel");
		bannedList.add("jenny");
		bannedList.add("ben");
		bannedList.add("neve");
	}
	
	public static DomainNameFilterer getInstance() {
		if(instance == null) {
			instance = new DomainNameFilterer();
		}
		return instance;
	}
	
	public boolean isAllowed(String testString) {
		if (bannedList.contains(testString)) {
			return false;
		}
		return true;
	}
}
