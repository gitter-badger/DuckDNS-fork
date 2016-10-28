package org.duckdns.cache;

import java.util.HashMap;
import java.util.Map;

public class LocalMemoryCache {
	
	Map<String, LocalCacheEntry> theCache = new HashMap<String, LocalCacheEntry>();
	
	public LocalCacheEntry getFromCache(String subdomain) {
		if (theCache.containsKey(subdomain)) {
			LocalCacheEntry theCacheEntry = theCache.get(subdomain);
			// TODO Maybe test age of cache
			if (theCacheEntry != null) {
				return theCacheEntry;
			}
		}
		return null;
	}
	
	public void removeForDomain(String subdomain) {
		if (theCache.containsKey(subdomain)) {
			theCache.remove(subdomain);
		}
	}
	
	public void addToCache(String subdomain, String theFoundIp, String theIPv6) {
		LocalCacheEntry newEntry = new LocalCacheEntry();
		newEntry.setIp(theFoundIp);
		newEntry.setIpV6(theIPv6);
		newEntry.setTimeCreated(System.currentTimeMillis());
		theCache.put(subdomain, newEntry);
	}

}
