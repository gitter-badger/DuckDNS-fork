// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

import java.io.*;
import java.net.*;
import java.util.*;

import org.duckdns.cache.LocalCacheEntry;
import org.duckdns.cache.LocalMemoryCache;
import org.duckdns.comms.MultiThreadedServer;
import org.duckdns.comms.services.CacheRemoveReceiver;
import org.duckdns.dao.AmazonDynamoDBDAO;
import org.duckdns.dao.model.Domain;
import org.duckdns.utils.GoogleAnalyticsHelper;
import org.xbill.DNS.*;

/**
 * Based on JNamed by Brian Wellington
 * 
 * @author Brian Wellington &lt;bwelling@xbill.org&gt;
 * @author Steven Harper &lt;steven@duckdns.org&gt;
 * @author Richard Harper &lt;richard@duckdns.org&gt; 
 */

public class DuckDnsServer {
	
	static boolean debug = false;
	
	//SINK HOLE IP
	static final String sinkHole = "192.169.69.25"; 
	
	static final int FLAG_DNSSECOK = 1;
	static final int FLAG_SIGONLY = 2;
	
	Map<Integer,Cache> caches;
	Map<Name,Zone> znames;
	Map<Name,TSIG> TSIGs;
	
	private LocalMemoryCache localMemoryCache = null;
	
	private static String addrport(InetAddress addr, int port) {
		return addr.getHostAddress() + "#" + port;
	}
	
	public static boolean isIPv6Address(String input) {
    	if (input != null && input.contains(":")) {
    		return true;
    	}
    	return false;
    }
	
	public DuckDnsServer(String conffile) throws IOException, ZoneTransferException {
		FileInputStream fs;
		InputStreamReader isr;
		BufferedReader br;
		List<Integer> ports = new ArrayList<Integer>();
		List<InetAddress> addresses = new ArrayList<InetAddress>();
		try {
			fs = new FileInputStream(conffile);
			isr = new InputStreamReader(fs);
			br = new BufferedReader(isr);
		} catch (Exception e) {
			System.out.println("Cannot open " + conffile);
			return;
		}
	
		try {
			caches = new HashMap<Integer,Cache>();
			znames = new HashMap<Name,Zone>();
			TSIGs = new HashMap<Name,TSIG>();
	
			String line = null;
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				if (!st.hasMoreTokens()) {
					continue;
				}
				String keyword = st.nextToken();
				if (!st.hasMoreTokens()) {
					System.out.println("Invalid line: " + line);
					continue;
				}
				if (keyword.charAt(0) == '#') {
					continue;
				}
				if (keyword.equals("primary")) {
					addPrimaryZone(st.nextToken(), st.nextToken());
				} else if (keyword.equals("secondary")) {
					addSecondaryZone(st.nextToken(),st.nextToken());
				} else if (keyword.equals("cache")) {
					Cache cache = new Cache(st.nextToken());
					caches.put(new Integer(DClass.IN), cache);
				} else if (keyword.equals("key")) {
					String s1 = st.nextToken();
					String s2 = st.nextToken();
					if (st.hasMoreTokens()) {
						addTSIG(s1, s2, st.nextToken());
					} else {
						addTSIG("hmac-md5", s1, s2);
					}
				} else if (keyword.equals("port")) {
					ports.add(Integer.valueOf(st.nextToken()));
				} else if (keyword.equals("address")) {
					String addr = st.nextToken();
					addresses.add(Address.getByAddress(addr));
				} else {
					System.out.println("unknown keyword: " + keyword);
				}
			}
			
			// SETUP The Cache
			localMemoryCache = new LocalMemoryCache();
			// WARM THE DB
			String theFoundIp = "";
			Domain dbRecord = AmazonDynamoDBDAO.getInstance().domainGetDomain("www");
			if (dbRecord != null && dbRecord.getCurrentIp() != null && dbRecord.getCurrentIp().length() != 0) {
				theFoundIp = dbRecord.getCurrentIp();
				// Add to the MEM cache
				localMemoryCache.addToCache("www", theFoundIp, null);
			} else {
				if (debug) {
					theFoundIp = "1.1.1.1";
					localMemoryCache.addToCache("www", theFoundIp, null);
				}
			}
			System.out.println("DNS Server: Dynamo DB warmed - cached www as " + theFoundIp);
			// START
			MultiThreadedServer s = MultiThreadedServer.getInstance();
			s.addService(new CacheRemoveReceiver("Local Memory Reciever", localMemoryCache), 10025, 50);
			System.out.println("DNS Server: Cache Remover Started");
			
			if (ports.size() == 0) {
				ports.add(new Integer(53));
				System.out.println("Adding default port of 53");
			}
	
			if (addresses.size() == 0) {
				addresses.add(Address.getByAddress("0.0.0.0"));
			}
	
			Iterator<InetAddress> iaddr = addresses.iterator();
			while (iaddr.hasNext()) {
				InetAddress addr = (InetAddress) iaddr.next();
				Iterator<Integer> iport = ports.iterator();
				while (iport.hasNext()) {
					int port = ((Integer)iport.next()).intValue();
					addUDP(addr, port);
					//addTCP(addr, port);
					System.out.println("DNS Server: listening on " + addrport(addr, port));
				}
			}
			
			System.out.println("DNS Server: running");
		} finally {
			fs.close();
		}
	}
	
	public void addPrimaryZone(String zname, String zonefile) throws IOException {
		Name origin = null;
		if (zname != null) {
			origin = Name.fromString(zname, Name.root);
		}
		Zone newzone = new Zone(origin, zonefile);
		znames.put(newzone.getOrigin(), newzone);
	}
	
	public void addSecondaryZone(String zone, String remote) throws IOException, ZoneTransferException {
		Name zname = Name.fromString(zone, Name.root);
		Zone newzone = new Zone(zname, DClass.IN, remote);
		znames.put(zname, newzone);
	}
	
	public void addTSIG(String algstr, String namestr, String key) throws IOException {
		Name name = Name.fromString(namestr, Name.root);
		TSIGs.put(name, new TSIG(algstr, namestr, key));
	}
	
	public Cache getCache(int dclass) {
		Cache c = (Cache) caches.get(new Integer(dclass));
		if (c == null) {
			c = new Cache(dclass);
			caches.put(new Integer(dclass), c);
		}
		return c;
	}
	
	public Zone findBestZone(Name name) {
		Zone foundzone = null;
		foundzone = (Zone) znames.get(name);
		if (foundzone != null) {
			return foundzone;
		}
		int labels = name.labels();
		for (int i = 1; i < labels; i++) {
			Name tname = new Name(name, i);
			foundzone = (Zone) znames.get(tname);
			if (foundzone != null) {
				return foundzone;
			}
		}
		return null;
	}
	
	public RRset findExactMatch(Name name, int type, int dclass, boolean glue) {
		Zone zone = findBestZone(name);
		if (zone != null) {
			return zone.findExactMatch(name, type);
		} else {
			RRset [] rrsets;
			Cache cache = getCache(dclass);
			if (glue) {
				rrsets = cache.findAnyRecords(name, type);
			} else {
				rrsets = cache.findRecords(name, type);
			}
			if (rrsets == null) {
				return null;
			} else {
				return rrsets[0]; /* not quite right */
			}
		}
	}
	
	void addRRset(Name name, Message response, RRset rrset, int section, int flags) {
		for (int s = 1; s <= section; s++) {
			if (response.findRRset(name, rrset.getType(), s)) {
				return;
			}
		}
		if ((flags & FLAG_SIGONLY) == 0) {
			Iterator it = rrset.rrs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild()) {
					r = r.withName(name);
				}
				response.addRecord(r, section);
			}
		}
		if ((flags & (FLAG_SIGONLY | FLAG_DNSSECOK)) != 0) {
			Iterator it = rrset.sigs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild()) {
					r = r.withName(name);
				}
				response.addRecord(r, section);
			}
		}
	}
	
	private final void addSOA(Message response, Zone zone) {
		response.addRecord(zone.getSOA(), Section.AUTHORITY);
	}
	
	private final void addNS(Message response, Zone zone, int flags) {
		RRset nsRecords = zone.getNS();
		addRRset(nsRecords.getName(), response, nsRecords, Section.AUTHORITY, flags);
	}
	
	private final void addCacheNS(Message response, Cache cache, Name name) {
		SetResponse sr = cache.lookupRecords(name, Type.NS, Credibility.HINT);
		if (!sr.isDelegation()) {
			return;
		}
		RRset nsRecords = sr.getNS();
		Iterator it = nsRecords.rrs();
		while (it.hasNext()) {
			Record r = (Record) it.next();
			response.addRecord(r, Section.AUTHORITY);
		}
	}
	
	private void addGlue(Message response, Name name, int flags) {
		RRset a = findExactMatch(name, Type.A, DClass.IN, true);
		if (a == null) {
			return;
		}
		addRRset(name, response, a, Section.ADDITIONAL, flags);
	}
	
	private void addAdditional2(Message response, int section, int flags) {
		Record [] records = response.getSectionArray(section);
		for (int i = 0; i < records.length; i++) {
			Record r = records[i];
			Name glueName = r.getAdditionalName();
			if (glueName != null) {
				addGlue(response, glueName, flags);
			}
		}
	}
	
	private final void addAdditional(Message response, int flags) {
		addAdditional2(response, Section.ANSWER, flags);
		addAdditional2(response, Section.AUTHORITY, flags);
	}
	
	byte addAnswer(Message response, Name name, int type, int dclass, int iterations, int flags) {
		SetResponse sr;
		byte rcode = Rcode.NOERROR;
	
		if (iterations > 6) {
			return Rcode.NOERROR;
		}
		if (debug) { System.out.println("typebefore:"+type); };
		if (type == Type.SIG || type == Type.RRSIG) {
			type = Type.ANY;
			flags |= FLAG_SIGONLY;
		}
	
		Zone zone = findBestZone(name);
		if (zone != null) {
			sr = zone.findRecords(name, type);
		} else {
			Cache cache = getCache(dclass);
			sr = cache.lookupRecords(name, type, Credibility.NORMAL);
		}
	
		if (sr.isUnknown()) {
			if (debug) { System.out.println("UNKNOWN"); };
			addCacheNS(response, getCache(dclass), name);
		}
		if (sr.isNXDOMAIN()) {
			if (debug) { System.out.println("NXDOMAIN"); };
			boolean didFind = false;
			boolean worthTryingDB = true;
	
			LocalCacheEntry theCacheItem = null;
			String theFoundIpV4 = null;
			String theFoundIpV6 = null;
			
			
			String subdomain = AmazonDynamoDBDAO.getSubdomain(name.toString().toLowerCase());
			// CHECK THE CACHE FIRST
			boolean didHitMemCache = false;
			theCacheItem = localMemoryCache.getFromCache(subdomain);
			if (theCacheItem == null) {
				//System.out.println("**** MISS CACHE " + subdomain);
			} else {
				didHitMemCache = true;
				//System.out.println("**** HIT CACHE " + subdomain);
			}
			if (theCacheItem == null) {
				
				// OK so nothings been found
				if (subdomain == null || subdomain.length() == 0) {
					// If there was no dot in the domain fall quick
					worthTryingDB = false;
				} else {
					// CHECK the pattern
					if (!subdomain.matches("^[a-zA-Z0-9\\-]*$")) {
						worthTryingDB = false;
					}
				}
				
				if (worthTryingDB) {
					// LOOKUP THE RECORD - just use subdomain as it's already cleaned
					Domain dbRecord = AmazonDynamoDBDAO.getInstance().domainGetDomain(subdomain);
					if (dbRecord != null) {
						// NAUGHTY DUCK
						if (dbRecord.getLocked() != null && dbRecord.getLocked().equals("true")) {
							theFoundIpV4 = sinkHole;
							theFoundIpV6 = null;
						} else {
							theFoundIpV4 = dbRecord.getCurrentIp();
							theFoundIpV6 = dbRecord.getCurrentIpV6();
						}
						// Add to the MEM cache
						localMemoryCache.addToCache(subdomain, theFoundIpV4, theFoundIpV6);
					} else {
						// SET AS NOT FOUND
						theFoundIpV4 = null;
						theFoundIpV6 = null;
						// Cache the Negative
						// Add to the MEM cache
						localMemoryCache.addToCache(subdomain, null, null);
					}
				}
			} else {
				// GOT AN ITEM GREAT!
				theFoundIpV4 = theCacheItem.getIp();
				theFoundIpV6 = theCacheItem.getIpV6();
			}
			
			if (debug) { 
				System.out.println("before ip v6 in v4 space hack");
				System.out.println("theFoundIpV4: " + theFoundIpV4);
				System.out.println("theFoundIpV6: " + theFoundIpV6);
			};
			
			boolean needsNoErrorEmptyResponse = false;
			
			if ((theFoundIpV4 != null && theFoundIpV4.length() > 0) || (theFoundIpV6 != null && theFoundIpV6.length() > 0)) {
				didFind = true;
				// IF WE ACCIDENTLY STORED A V4 in a V6 space, then if the V6 is empty replace it, also set v4 empty
				if (isIPv6Address(theFoundIpV4)) {
					if (theFoundIpV6 == null || theFoundIpV6.length() == 0) {
						theFoundIpV6 = theFoundIpV4;
					}
					theFoundIpV4 = null;
				}
			}
			
			if (debug) { 
				System.out.println("after hack");
				System.out.println("theFoundIpV4: " + theFoundIpV4);
				System.out.println("theFoundIpV6: " + theFoundIpV6);
			};
			
			if (didFind) {
				// HACK A ZONE 
				try {
					zone.findRecords(new Name("www.duckdns.org"), Type.SIG);
				} catch (TextParseException e) {
					e.printStackTrace();
				}
				sr = zone.findRecords(name, type);
				
				// MX that Hit
				if (type == Type.MX) {
					
					// ONES WE CAN RESPOND TO - WITH IPv4 Addresses
					if (theFoundIpV4 != null && theFoundIpV4.length() > 0) {
						if (didHitMemCache) {
							if (!subdomain.equals("www")) {
								// Only Record non-www - too many threads
								GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","lookupmxcached",subdomain,"1");
							}
						} else {
							GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","lookupmx",subdomain,"1");
						}
						
						response.getHeader().setRcode(Rcode.NOERROR);
						Record aRec = null;
						//Name n = new Name(s);
						aRec = new MXRecord(name, Type.A, 600L, 50, name);
						RRset rrset = new RRset(aRec);
						addRRset(name, response, rrset,Section.ANSWER, Flags.QR);
						
						try {
							aRec = new ARecord(name, Type.A, 600L, Address.getByAddress(theFoundIpV4, Address.IPv4));
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
						rrset = new RRset(aRec);
						addRRset(name, response, rrset,Section.ADDITIONAL, Flags.QR);
						needsNoErrorEmptyResponse = false;
					} else {
						// NO IPv4 - no error - no response
						response.getHeader().setRcode(Rcode.NOERROR);
						addSOA(response, zone);
						if (iterations == 0) {
							response.getHeader().setFlag(Flags.AA);
						}
						needsNoErrorEmptyResponse = true;
					}
					
				} else if (type == Type.AAAA) {
					
					// DO WE HAVE AN IPv6 STORED!
					if (theFoundIpV6 != null && theFoundIpV6.length() > 0) {
						if (debug) { System.out.println("AAAA has ipv6 Record"); };
						
						if (didHitMemCache) {
							GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","lookupcached",subdomain,"1");
						} else {
							GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","lookup",subdomain,"1");
						}
						
						// MANUALLY MAKE A RESPONSE
						response.getHeader().setRcode(Rcode.NOERROR);
						Record aRec = null;
						try {
							aRec = new AAAARecord(name, Type.A, 60L,Address.getByAddress(theFoundIpV6, Address.IPv6));
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
						RRset rrset = new RRset(aRec);
						addRRset(name, response, rrset,Section.ANSWER, Flags.QR);
						needsNoErrorEmptyResponse = false;
						
					} else {
						if (debug) { System.out.println("AAAA no ipv6 Record"); };
						// NO IPv6 - no error - no response
						response.getHeader().setRcode(Rcode.NOERROR);
						addSOA(response, zone);
						if (iterations == 0) {
							response.getHeader().setFlag(Flags.AA);
						}
						needsNoErrorEmptyResponse = true;
					}
				} else {
					// THIS IS FOR A RESPONSES
					// DO WE HAVE AN IPv4 STORED!
					if (theFoundIpV4 != null && theFoundIpV4.length() > 0) {
						if (debug) { System.out.println("A is IPv4 Record : " + theFoundIpV4); };
						if (didHitMemCache) {
							GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","lookupcached",subdomain,"1");
						} else {
							GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","lookup",subdomain,"1");
						}
						
						// WORK OUT SPECIAL TTL
						long TTL = 60L;
						if (name.toString().toLowerCase().equals("www.duckdns.org.")) {
							TTL = 60L;
						}
						
						// MANUALLY MAKE A RESPONSE
						response.getHeader().setRcode(Rcode.NOERROR);
						Record aRec = null;
						try {
							aRec = new ARecord(name, Type.A, TTL, Address.getByAddress(theFoundIpV4, Address.IPv4));
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
						RRset rrset = new RRset(aRec);
						addRRset(name, response, rrset,Section.ANSWER, Flags.QR);
						needsNoErrorEmptyResponse = false;
					} else {
						if (debug) { System.out.println("A no ipv4 Record"); };
						// NO IPv4 - no error - no response
						response.getHeader().setRcode(Rcode.NOERROR);
						addSOA(response, zone);
						if (iterations == 0) {
							response.getHeader().setFlag(Flags.AA);
						}
						needsNoErrorEmptyResponse = true;
					}
				}
				//System.out.println("ANSWER "+0 + " ," + name+ " ," + rrset + " ," + Flags.QR);
				
				// DONT ADD ZONE FOR AAAA that did find
				if (!needsNoErrorEmptyResponse) {
					if (zone != null) {
						addNS(response, zone, flags);
						if (iterations == 0) {
							response.getHeader().setFlag(Flags.AA);
						}
					}
				}
			} else { 
				// NOTHING FOUND AT ALL - OR NEGATIVE CACHE
				if (didHitMemCache) {
					// Negative Cache Hit!
					GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","lookupcached",subdomain,"1");
				} else if (worthTryingDB) {
					GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","lookupfail",subdomain,"1");
				} else {
					GoogleAnalyticsHelper.RecordAsyncEvent("9999","dns","lookupignored",subdomain,"1");
				}
				
				response.getHeader().setRcode(Rcode.NXDOMAIN);
				if (zone != null) {
					if (debug) { System.out.println("ADDING SOA"); };
					addSOA(response, zone);
					if (iterations == 0) {
						response.getHeader().setFlag(Flags.AA);
					}
				}
				//rcode = Rcode.NXDOMAIN;
			}
		} else if (sr.isNXRRSET()) {
			if (debug) { System.out.println("NXRRSET"); };
			if (zone != null) {
				addSOA(response, zone);
				if (iterations == 0) {
					response.getHeader().setFlag(Flags.AA);
				}
			}
		} else if (sr.isDelegation()) {
			if (debug) { System.out.println("DELEGATION"); };
			RRset nsRecords = sr.getNS();
			addRRset(nsRecords.getName(), response, nsRecords, Section.AUTHORITY, flags);
		} else if (sr.isCNAME()) {
			if (debug) { System.out.println("CNAME"); };
			CNAMERecord cname = sr.getCNAME();
			RRset rrset = new RRset(cname);
			addRRset(name, response, rrset, Section.ANSWER, flags);
			if (zone != null && iterations == 0) {
				response.getHeader().setFlag(Flags.AA);
			}
			rcode = addAnswer(response, cname.getTarget(), type, dclass, iterations + 1, flags);
		} else if (sr.isDNAME()) {
			if (debug) { System.out.println("DNAME"); };
			DNAMERecord dname = sr.getDNAME();
			RRset rrset = new RRset(dname);
			addRRset(name, response, rrset, Section.ANSWER, flags);
			Name newname;
			try {
				newname = name.fromDNAME(dname);
			} catch (NameTooLongException e) {
				return Rcode.YXDOMAIN;
			}
			rrset = new RRset(new CNAMERecord(name, dclass, 0, newname));
			addRRset(name, response, rrset, Section.ANSWER, flags);
			if (zone != null && iterations == 0) {
				response.getHeader().setFlag(Flags.AA);
			}
			rcode = addAnswer(response, newname, type, dclass, iterations + 1, flags);
			
		} else if (sr.isSuccessful()) {
			if (debug) { 
				System.out.println("Type:" + type);
				System.out.println("dclass:" + dclass);
				System.out.println("name:" + name);
				System.out.println("flags:" + flags);
			}
			
			//System.out.println("SUCCESSFULL");
			RRset [] rrsets = sr.answers();
			
			if (debug) { System.out.println("rrsets.length:"+rrsets.length); };
			
			for (int i = 0; i < rrsets.length; i++) {
				
				boolean didFindOverload = false;
				if (name.toString().equals("duckdns.org.")) {
					// HACKERY
					LocalCacheEntry theCacheItem = localMemoryCache.getFromCache("www");
					String theFoundIpV4 = null;
					if (theCacheItem == null) {
						// for www.duckdns.org.
						Domain dbRecord = AmazonDynamoDBDAO.getInstance().domainGetDomain("www");
						theFoundIpV4 = dbRecord.getCurrentIp();
						if (theFoundIpV4 == null || theFoundIpV4.length() < 1) {
							System.out.println("NO www : ARRRRROOOOOOOGA!");
						}
						localMemoryCache.addToCache("www", theFoundIpV4, null);
					} else {
						theFoundIpV4 = theCacheItem.getIp();
						if (theFoundIpV4 == null || theFoundIpV4.length() == 0) {
							System.out.println("NASTY HACK!");
							theFoundIpV4 = "54.187.92.222";
						}
					}
					try {
						int typeToUse = Type.A;
						Record aRec = null;
						if (type == Type.MX || type == Type.SOA || type == Type.NS) {
							// typeToUse = Type.MX;
							//aRec = new MXRecord(name, typeToUse, 60L, 20, new Name("mx.duckdns.org."));
							didFindOverload = false;
						} else {
							aRec = new ARecord(name, typeToUse, 60L, Address.getByAddress(theFoundIpV4, Address.IPv4));
							RRset rrset = new RRset(aRec);
							addRRset(name, response, rrset,Section.ANSWER, flags);
							didFindOverload = true;
						}
	
						System.out.println("Hack duckdns.org. response : for type " + type);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
				} 
				if (!didFindOverload) {
					addRRset(name, response, rrsets[i],Section.ANSWER, flags);
				}			
				//System.out.println("ANSWER "+i + " ," + name+ " ," + rrsets[i] + " ," + flags + " zone " + zone);
				
				//Record r = new Re
			}
			if (zone != null) {
				if (debug) { System.out.println("adding NS in zone block iterations:"+iterations); };
				addNS(response, zone, flags);
				if (iterations == 0) {
					response.getHeader().setFlag(Flags.AA);
				}
			} else {
				if (debug) { System.out.println("adding cached"); };
				addCacheNS(response, getCache(dclass), name);
			}
		}
		return rcode;
	}
	
	byte [] doAXFR(Name name, Message query, TSIG tsig, TSIGRecord qtsig, Socket s) {
		Zone zone = (Zone) znames.get(name);
		boolean first = true;
		if (zone == null) {
			return errorMessage(query, Rcode.REFUSED);
		}
		Iterator it = zone.AXFR();
		try {
			DataOutputStream dataOut;
			dataOut = new DataOutputStream(s.getOutputStream());
			int id = query.getHeader().getID();
			while (it.hasNext()) {
				RRset rrset = (RRset) it.next();
				Message response = new Message(id);
				Header header = response.getHeader();
				header.setFlag(Flags.QR);
				header.setFlag(Flags.AA);
				addRRset(rrset.getName(), response, rrset, Section.ANSWER, FLAG_DNSSECOK);
				if (tsig != null) {
					tsig.applyStream(response, qtsig, first);
					qtsig = response.getTSIG();
				}
				first = false;
				byte [] out = response.toWire();
				dataOut.writeShort(out.length);
				dataOut.write(out);
			}
		} catch (IOException ex) {
			System.out.println("AXFR failed");
		} try {
			s.close();
		} catch (IOException ex) {
		}
		return null;
	}
	
	/*
	 * Note: a null return value means that the caller doesn't need to do
	 * anything.  Currently this only happens if this is an AXFR request over
	 * TCP.
	 */
	byte [] generateReply(Message query, byte [] in, int length, Socket s) throws IOException {
		Header header;
		boolean badversion;
		int maxLength;
		int flags = 0;
	
		header = query.getHeader();
		if (header.getFlag(Flags.QR)) {
			return null;
		}
		if (header.getRcode() != Rcode.NOERROR) {
			return errorMessage(query, Rcode.FORMERR);
		}
		if (header.getOpcode() != Opcode.QUERY) {
			return errorMessage(query, Rcode.NOTIMP);
		}
	
		Record queryRecord = query.getQuestion();
	
		TSIGRecord queryTSIG = query.getTSIG();
		TSIG tsig = null;
		if (queryTSIG != null) {
			tsig = (TSIG) TSIGs.get(queryTSIG.getName());
			if (tsig == null || tsig.verify(query, in, length, null) != Rcode.NOERROR) {
				return formerrMessage(in);
			}
		}
	
		OPTRecord queryOPT = query.getOPT();
		if (queryOPT != null && queryOPT.getVersion() > 0) {
			badversion = true;
		}
	
		if (s != null) {
			maxLength = 65535;
		} else if (queryOPT != null) {
			maxLength = Math.max(queryOPT.getPayloadSize(), 512);
		} else {
			maxLength = 512;
		}
		
		if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0) {
			flags = FLAG_DNSSECOK;
		}
	
		Message response = new Message(query.getHeader().getID());
		response.getHeader().setFlag(Flags.QR);
		if (query.getHeader().getFlag(Flags.RD)) {
			response.getHeader().setFlag(Flags.RD);
		}
		response.addRecord(queryRecord, Section.QUESTION);
	
		Name name = queryRecord.getName();
		int type = queryRecord.getType();
		int dclass = queryRecord.getDClass();
		if (type == Type.AXFR && s != null) {
			return doAXFR(name, query, tsig, queryTSIG, s);
		}
		if (!Type.isRR(type) && type != Type.ANY) {
			return errorMessage(query, Rcode.NOTIMP);
		}
	
		byte rcode = addAnswer(response, name, type, dclass, 0, flags);
		if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) {
			return errorMessage(query, rcode);
		}
		
		addAdditional(response, flags);
	
		if (queryOPT != null) {
			int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
			OPTRecord opt = new OPTRecord((short)4096, rcode, (byte)0, optflags);
			response.addRecord(opt, Section.ADDITIONAL);
		}
	
		response.setTSIG(tsig, Rcode.NOERROR, queryTSIG);
		return response.toWire(maxLength);
	}
	
	byte [] buildErrorMessage(Header header, int rcode, Record question) {
		Message response = new Message();
		response.setHeader(header);
		for (int i = 0; i < 4; i++) {
			response.removeAllRecords(i);
		}
		if (rcode == Rcode.SERVFAIL) {
			response.addRecord(question, Section.QUESTION);
		}
		header.setRcode(rcode);
		return response.toWire();
	}
	
	public byte[] formerrMessage(byte [] in) {
		Header header;
		try {
			header = new Header(in);
		} catch (IOException e) {
			return null;
		}
		return buildErrorMessage(header, Rcode.FORMERR, null);
	}
	
	public byte[] errorMessage(Message query, int rcode) {
		return buildErrorMessage(query.getHeader(), rcode, query.getQuestion());
	}
	
	public void TCPclient(Socket s) {
		try {
			int inLength;
			DataInputStream dataIn;
			DataOutputStream dataOut;
			byte [] in;
	
			InputStream is = s.getInputStream();
			dataIn = new DataInputStream(is);
			inLength = dataIn.readUnsignedShort();
			in = new byte[inLength];
			dataIn.readFully(in);
	
			Message query;
			byte [] response = null;
			try {
				query = new Message(in);
				response = generateReply(query, in, in.length, s);
				if (response == null) {
					return;
				}
			} catch (IOException e) {
				response = formerrMessage(in);
			}
			dataOut = new DataOutputStream(s.getOutputStream());
			dataOut.writeShort(response.length);
			dataOut.write(response);
		} catch (IOException e) {
			System.out.println("TCPclient(" + addrport(s.getLocalAddress(), s.getLocalPort()) + "): " + e);
		} finally {
			try {
				s.close();
			} catch (IOException e) {}
		}
	}
	
	public void serveTCP(InetAddress addr, int port) {
		try {
			ServerSocket sock = new ServerSocket(port, 128, addr);
			while (true) {
				final Socket s = sock.accept();
				Thread t;
				t = new Thread(new Runnable() {
					public void run() {
						TCPclient(s);
					}
				});
				t.start();
			}
		} catch (IOException e) {
			System.out.println("serveTCP(" + addrport(addr, port) + "): " + e);
		}
	}
	
	class MultiServer {
	
	    private DatagramSocket serversocket;
	
	    public MultiServer(int port, InetAddress addr) {
	        try {
	            this.serversocket = new DatagramSocket(port, addr);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	
	    public void start() throws IOException {
	        while(true) {
	        	try {
		        	final short udpLength = 512;
		    		byte [] in = new byte[udpLength];
		    		DatagramPacket indp = new DatagramPacket(in, in.length);
		    		indp.setLength(in.length);
		            serversocket.receive(indp);
		            new Thread(new ClientHandler(indp, in, serversocket)).start();
	        	} catch (Throwable tw) {
	        		System.out.println("Throwable in UDP thread : " + tw.toString());
	        	}
	        }
	    }
	}
	
	class ClientHandler implements Runnable {
	
	    private final DatagramPacket inputPacket;
	    private byte [] in;
	    DatagramSocket serversocket;
	
	    ClientHandler(DatagramPacket indp, byte [] in, DatagramSocket serversocket) {
	        this.inputPacket = indp;
	        this.in = in;
	        this.serversocket = serversocket;
	    }
	
	    public void run() {
	        //receive packet, send msg, get ip, get portnumber ?
	    	DatagramPacket outdp = null;
	    	Message query;
			byte [] response = null;
			try {
				query = new Message(in);
				response = generateReply(query, in, inputPacket.getLength(), null);
				if (response == null) {
					return;
				}
			} catch (IOException e) {
				response = formerrMessage(in);
			}
			outdp = new DatagramPacket(response, response.length, inputPacket.getAddress(), inputPacket.getPort());
			try {
				serversocket.send(outdp);
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	
	}
	
	public void serveUDP(InetAddress addr, int port) {
		try {
			MultiServer server = new MultiServer(port, addr);
			server.start();
		} catch (IOException e) {
			System.out.println("serveUDP(" + addrport(addr, port) + "): " + e);
		}
	}
	
	public void addTCP(final InetAddress addr, final int port) {
		Thread t;
		t = new Thread(new Runnable() {
			public void run() {
				serveTCP(addr, port);
			}
		});
		t.start();
	}
	
	public void addUDP(final InetAddress addr, final int port) {
		Thread t;
		t = new Thread(new Runnable() {
			public void run() {
				serveUDP(addr, port);
			}
		});
		t.start();
	}
	
	public static void main(String [] args) {
		if (args.length > 1) {
			System.out.println("usage: jnamed [conf]");
			System.exit(0);
		}
		DuckDnsServer s;
		try {
			String conf;
			if (args.length == 1) {
				conf = args[0];
			} else {
				conf = "jnamed.conf";
			}
			s = new DuckDnsServer(conf);
		} catch (IOException e) {
			System.out.println(e);
		} catch (ZoneTransferException e) {
			System.out.println(e);
		}
	}

}