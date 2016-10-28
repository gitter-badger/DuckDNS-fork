package org.duckdns.util;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
@SuppressWarnings("deprecation")
public class GoogleAnalyticsHelper {

	private static final long SLEEP_TIME = 60000;
	
	public static final String CATEGORY_ACCOUNT = "account";
	public static final String CATEGORY_DOMAIN = "domain";
	public static final String CATEGORY_DNS = "dns";
	public static final String CATEGORY_DNS_GNUDIP = "dns_gnudip"; 
	public static final String CATEGORY_DNS_DYN = "dns_dyndns";
	public static final String CATEGORY_USER = "user";
	public static final String CATEGORY_TOTAL = "total";
	
	public static final String ACTION_CREATE_ACCOUNT = "create_account";
	public static final String ACTION_CREATE_DOMAIN = "create_domain";
	public static final String ACTION_UPDATE_FAILED = "update_failed";
	public static final String ACTION_UPDATE = "update";
	public static final String ACTION_UPDATE_SAME = "update_same";
	public static final String ACTION_DELETE_ACCOUNT = "delete_account"; 
	public static final String ACTION_DELETE_DOMAIN = "delete_domain";
	public static final String ACTION_LOGIN_GOOGLE = "login_google";
	public static final String ACTION_LOGIN_REDDIT = "login_reddit";
	public static final String ACTION_LOGIN_FACEBOOK = "login_facebook";
	public static final String ACTION_LOGIN_TWITTER = "login_twitter";
	public static final String ACTION_LOGIN_PERSONA = "login_persona";
	public static final String ACTION_LOGOUT = "logout"; 
	public static final String ACTION_RECREATE_TOKEN = "recreate_token"; 
	public static final String ACTION_READ_ONLY = "account_read_only";
	public static final String ACTION_LOOKUP_CACHED = "lookupcached";
	public static final String ACTION_LOOKUP_IGNORED = "lookupignored";
	public static final String ACTION_LOOKUP_MXCACHED = "lookupmxcached";
	
	public static final String TMP_ID = "9999";

	private static GoogleAnalyticsHelper instance = new GoogleAnalyticsHelper();
	
	protected static int numUpdateSame;
	protected static Object lockUpdateSame;
	protected static int numUpdateSameGnuDip;
	protected static Object lockUpdateSameGnuDip;
	protected static int numUpdateFailedGnuDip;
	protected static Object lockUpdateFailedGnuDip;
	protected static int numUpdateSameDyn;
	protected static Object lockUpdateSameDyn;
	
	public GoogleAnalyticsHelper() {
		
		numUpdateSame = 0;
		numUpdateSameGnuDip = 0;
		numUpdateFailedGnuDip = 0;
		numUpdateSameDyn = 0;
		lockUpdateSame = new Object();
		lockUpdateSameGnuDip = new Object();
		lockUpdateFailedGnuDip = new Object();
		lockUpdateSameDyn = new Object();
		
		( new Thread("Google Analytics Cached Counter") {
			public void run() {
				
				int numNotSent = 0;
				int numToSend = 0;
				
				while (true) {
					
					synchronized (lockUpdateSame) {
						numToSend  = numUpdateSame;
						numUpdateSame = 0;
					}
					numNotSent = sendTotaled(numToSend,CATEGORY_DNS+"_"+ACTION_UPDATE_SAME,"dns update same");
					if (numNotSent > 0) {
						synchronized (lockUpdateSame) {
							numUpdateSame = numUpdateSame + numNotSent;
						}
					}
					
					synchronized (lockUpdateSameGnuDip) {
						numToSend  = numUpdateSameGnuDip;
						numUpdateSameGnuDip = 0;
					}
					numNotSent = sendTotaled(numToSend,CATEGORY_DNS_GNUDIP+"_"+ACTION_UPDATE_SAME,"dnsgnudip update same");
					if (numNotSent > 0) {
						synchronized (lockUpdateSameGnuDip) {
							numUpdateSameGnuDip = numUpdateSameGnuDip + numNotSent;
						}
					}
					
					synchronized (lockUpdateFailedGnuDip) {
						numToSend  = numUpdateFailedGnuDip;
						numUpdateFailedGnuDip = 0;
					}
					numNotSent = sendTotaled(numToSend,CATEGORY_DNS_GNUDIP+"_"+ACTION_UPDATE_FAILED,"dnsgnudip update failed");
					if (numNotSent > 0) {
						synchronized (lockUpdateFailedGnuDip) {
							numUpdateFailedGnuDip = numUpdateFailedGnuDip + numNotSent;
						}
					}
					
					synchronized (lockUpdateSameDyn) {
						numToSend  = numUpdateSameDyn;
						numUpdateSameDyn = 0;
					}
					numNotSent = sendTotaled(numToSend,CATEGORY_DNS_DYN+"_"+ACTION_UPDATE_SAME,"dnsdyn update same");
					if (numNotSent > 0) {
						synchronized (lockUpdateSameDyn) {
							numUpdateSameDyn = numUpdateSameDyn + numNotSent;
						}
					}
					
					try {
						// SLEEP FOR some seconds
						Thread.sleep(SLEEP_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			}
		} ).start();
	}
	
	public int sendTotaled(Integer theTotal, String action, String logName) {
		if (theTotal > 0) {
			// Now Record it
			try {
				System.out.println("Sending "+logName+" for : " + theTotal); 
				RecordSyncEvent(TMP_ID,CATEGORY_TOTAL,action,""+theTotal,theTotal+"");
			} catch (Exception e) {
				return theTotal;
			}
		}
		return 0;
	}
	
	public static int RecordSyncEvent(String clientId, String category, String action, String label, String value) throws ClientProtocolException, IOException {
		
		final HttpParams httpParams = new BasicHttpParams();
		// TEN SECOND TIMEOUT
	    HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
	    DefaultHttpClient client = new DefaultHttpClient(httpParams);
		
		StringBuffer sb = new StringBuffer();
		sb.append("http://www.google-analytics.com/collect?");
		sb.append("v=1"); 			// Version.
		sb.append("&tid=");			// Tracking ID / Web property / Property ID.
		sb.append(EnvironmentUtils.getInstance().getGOOGLE_TRACKING_ID());
		sb.append("&cid=");			// Anonymous Client ID.
		sb.append(URLEncoder.encode(clientId,"UTF-8"));
		sb.append("&t=event");		// Event hit type
		sb.append("&ec="); 			// Event Category. Required.
		sb.append(URLEncoder.encode(category,"UTF-8"));
		sb.append("&ea="); 			// Event Action. Required.
		sb.append(URLEncoder.encode(action,"UTF-8"));
		if (label != null && label.length() != 0) {
			sb.append("&el=");		// Event label.
			sb.append(URLEncoder.encode(label,"UTF-8"));
		}
		if (value != null && value.length() != 0) {
			sb.append("&ev=");		// Event value.
			sb.append(URLEncoder.encode(value,"UTF-8"));
		}
		HttpPost request = new HttpPost(sb.toString());
		HttpResponse httpResp = client.execute(request);
		int code = httpResp.getStatusLine().getStatusCode();
		client.close();
		return code;
	}
	
	public static void RecordAsyncEvent(final String clientId, final String category, final String action, final String label, final String value) {
		if (category.equals(CATEGORY_DNS) && action.equals(ACTION_UPDATE_SAME)) {
			synchronized (lockUpdateSame) {
				numUpdateSame++;
			}
		} else if (category.equals(CATEGORY_DNS_GNUDIP) && action.equals(ACTION_UPDATE_SAME)) {
			synchronized (lockUpdateSameGnuDip) {
				numUpdateSameGnuDip++;
			}
		} else if (category.equals(CATEGORY_DNS_GNUDIP) && action.equals(ACTION_UPDATE_FAILED)) {
			synchronized (lockUpdateFailedGnuDip) {
				numUpdateFailedGnuDip++;
			}
		} else if (category.equals(CATEGORY_DNS_DYN) && action.equals(ACTION_UPDATE_SAME)) {
			synchronized (lockUpdateSameDyn) {
				numUpdateSameDyn++;
			}
		} else {
			( new Thread("Google Async Event") {
				public void run() {
					try {
						RecordSyncEvent(clientId,category,action,label,value);
					} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} ).start();
		}
	}
	
	public static void main(String[] args) {
		try {
			System.out.println("Response was " + RecordSyncEvent("999999","dns","lookup","OK",""));
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		RecordAsyncEvent("999999",CATEGORY_DNS,ACTION_UPDATE_SAME,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS,ACTION_UPDATE_SAME,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS_GNUDIP,ACTION_UPDATE_FAILED,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS_GNUDIP,ACTION_UPDATE_FAILED,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS_GNUDIP,ACTION_UPDATE_SAME,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS_GNUDIP,ACTION_UPDATE_SAME,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS_GNUDIP,ACTION_UPDATE_SAME,"www","1");
		try {
			Thread.sleep(SLEEP_TIME+1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		RecordAsyncEvent("999999",CATEGORY_DNS,ACTION_UPDATE_SAME,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS,ACTION_UPDATE_SAME,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS_GNUDIP,ACTION_UPDATE_FAILED,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS_GNUDIP,ACTION_UPDATE_SAME,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS_GNUDIP,ACTION_UPDATE_SAME,"www","1");
		RecordAsyncEvent("999999",CATEGORY_DNS,ACTION_UPDATE_SAME,"www","1");
	}

	public static GoogleAnalyticsHelper getInstance() {
		return instance;
	}
	
}