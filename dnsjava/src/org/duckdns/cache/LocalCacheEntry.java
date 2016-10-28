package org.duckdns.cache;

public class LocalCacheEntry {
	private String ip;
	private String ipv6;
	private long timeCreated;
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getIpV6() {
		return ipv6;
	}
	public void setIpV6(String ipv6) {
		this.ipv6 = ipv6;
	}
	public long getTimeCreated() {
		return timeCreated;
	}
	public void setTimeCreated(long timeCreated) {
		this.timeCreated = timeCreated;
	}
}
