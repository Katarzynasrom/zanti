package com.zimperium.zanti.cracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Intent;

public class XmlParser {
	XmlPullParserFactory factory;
	String filename;
	public boolean finished = false;
	
	public XmlParser(String filename) throws XmlPullParserException {
		super();
		this.filename = filename;
		factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
	}
	public ArrayList<Intent> ParseUserPass() throws  IOException 
	{
		XmlPullParser xpp;
		try
		{
			xpp = factory.newPullParser();
		} catch (XmlPullParserException e)
		{
			e.printStackTrace();
			return null;
		}
		File f = new File(filename);
		if (!f.exists())
			return null;
		FileInputStream fis = new FileInputStream(f);
		try
		{
			xpp.setInput(new InputStreamReader(fis));
		} catch (XmlPullParserException e)
		{
			e.printStackTrace();
			return null;
		}

		String host = "";
		String user = "";
		String pass = "";
		String targeturl = "";
		
		ArrayList<Intent> hosts;
		hosts = new ArrayList<Intent>();
		
		int eventType =	0;
		try
		{
			eventType = xpp.getEventType();
		} catch (XmlPullParserException e)
		{
			e.printStackTrace();
			return null;
		}
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_DOCUMENT) {
			} else if (eventType == XmlPullParser.END_DOCUMENT) {
			} else if (eventType == XmlPullParser.START_TAG) {
				String nodeName = xpp.getName();
				//Log.d("", "<" + nodeName + ">");
				String param = "";
				if (nodeName.matches("hostname")) {
					try
					{
						targeturl = xpp.nextText();
					} catch (XmlPullParserException e)
					{
						targeturl = "";
					}
					//Log.d("", ">>>>>>>>>>>>>> targeturl = " + targeturl );
				}
				else if (nodeName.matches("user")) {
					try
					{
						user = xpp.nextText();
					} catch (XmlPullParserException e)
					{
					}
				}
				else if (nodeName.matches("pass")) {
					try
					{
						pass = xpp.nextText();
					} catch (XmlPullParserException e)
					{
					}
				}
				else if (nodeName.matches("info")) {
					try
					{
						targeturl = xpp.nextText();
					} catch (XmlPullParserException e)
					{
					}
				}
				else if (nodeName.matches("client")) {
					try
					{
						host = xpp.nextText();
					} catch (XmlPullParserException e)
					{
					}
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				if (xpp.getName().contentEquals("host")) {
					if (targeturl != "") {
						Intent n = new Intent();
						n.putExtra("title", targeturl);
						if (user != "") {
							n.putExtra("line1", "u/p: " + user + "/" + pass);
							n.putExtra("line2", "(from: " + host + ")");
						} else {
							SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss a");
							String now = formatter.format(new Date());
							n.putExtra("line1", "Time: " + now);
						}
						//Log.d("", ">>>>>>>>>>>>>> targeturl = " + targeturl );
						hosts.add(n);
					}
					host = user = pass = targeturl = "";
				}
			} else if (eventType == XmlPullParser.TEXT) {
			}
			
			try
			{
				eventType = xpp.next();
			} catch (XmlPullParserException e)
			{
				e.printStackTrace();
				//Log.d("", "" + eventType);
			}
		}
		return hosts;
	}
	
	public ArrayList<HashMap<String, Object>> Parse() throws IOException {

		XmlPullParser xpp;
		try {
                        xpp = factory.newPullParser();
                } catch (XmlPullParserException e) {
                        e.printStackTrace();
                        return null;
                }
		FileInputStream fis = new FileInputStream(new File(filename));
		
		try {
			fis.getFD().sync();
			Thread.sleep(5);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		try {
                        xpp.setInput(new InputStreamReader(fis));
                } catch (XmlPullParserException e) {
                        e.printStackTrace();
                        return null;
                }
		//ArrayList<String> HostsAndPorts = new ArrayList<String>();

		String protocol = "";
		Port port = null;
		String host = "";
		//boolean state_up = false;
		boolean in_port = false;
		boolean add_port = false;
                boolean found_vul_MS08_067 = false;
                boolean found_vul_MS12_020 = false;
		boolean found_vul_iphone_ssh = false;
		boolean found_vul_java = false;
		boolean found_plesk = false;
		String hostname = "";
		String mac_vendor = "";
		String mac_addr = "";
		String os_name = "";
		String os_vendor = "";
		String os_family = "";
		String os_info = "";
		int win_ports = 0;
		int os_icon = R.drawable.windowsxp;
		String localip = "";

		ArrayList<HashMap<String, Object>> hosts;
		hosts = new ArrayList<HashMap<String, Object>>();
		List<Port> portlist = new ArrayList<Port>();
		
		int eventType = 0;
                try {
                        eventType = xpp.getEventType();
                } catch (XmlPullParserException e) {
                        e.printStackTrace();
                }
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_DOCUMENT) {
			} else if (eventType == XmlPullParser.END_DOCUMENT) {
			} else if (eventType == XmlPullParser.START_TAG) {
				String nodeName = xpp.getName();
				//Log.d("-", nodeName);
                                if (nodeName.contentEquals("hostname")) {
                                        hostname = xpp.getAttributeValue(null, "name");
                                }
                                else if (nodeName.contentEquals("runstats")) {
					finished = true;
				}
				else if (nodeName.contentEquals("status")) {
					//String state = xpp.getAttributeValue(null, "state");
					//state_up = "up".matches(state);
				}
				else if (nodeName.contentEquals("address") /*&& state_up*/ && xpp.getAttributeValue(null, "addrtype").matches("ipv4")) {
					host = xpp.getAttributeValue(null, "addr");
				}
				else if (nodeName.contentEquals("address") /*&& state_up*/ && xpp.getAttributeValue(null, "addrtype").matches("mac")) {
					mac_vendor = xpp.getAttributeValue(null, "vendor");
					if (mac_vendor == null) mac_vendor="";
					mac_addr = xpp.getAttributeValue(null, "addr");
					if (mac_addr == null) mac_addr="";
				}
				else if (nodeName.contentEquals("port")) {
					in_port = true;
					port = new Port();
					protocol = xpp.getAttributeValue(null, "protocol");
					port.portnumber = Integer.parseInt(xpp.getAttributeValue(null, "portid"));
					add_port = false;
				}
				else if (nodeName.contentEquals("state") && in_port) {
					String state = xpp.getAttributeValue(null, "state");
					if (state.matches("open") && protocol.matches("tcp")) {
						add_port = true;
					}
				}
				//found_vul_MS08_067 = true;
				else if (nodeName.contentEquals("script")) {
					String output = xpp.getAttributeValue(null, "output");
					if (output.contains("MS08-067: VULNERABLE") || output.contains("MS08-067: LIKELY VULNERABLE")) {
						found_vul_MS08_067 = true;
					}
					if (output.contains("MS12-020") && output.contains("State: VULNERABLE")) {
                                                found_vul_MS12_020 = true;
					}
					if (output.contains("PLESK infected")) {
						found_plesk = true;
					}
				}
				else if (nodeName.contentEquals("found_vul_iphone_ssh")) {
                                        found_vul_iphone_ssh = true;
                                }
				else if (nodeName.contentEquals("found_vul_java")) {
                                        found_vul_java = true;
                                }
				else if (nodeName.contentEquals("service") && in_port) {
					port.portdescription = xpp.getAttributeValue(null, "name");
				}
				else if (nodeName.contentEquals("osclass")) {
					os_vendor = xpp.getAttributeValue(null, "vendor");
					if (os_vendor != null) os_info += os_vendor;
					os_family = xpp.getAttributeValue(null, "osfamily");
					if (os_family != null) os_info += os_family;
				}
				else if (nodeName.contentEquals("osmatch")) {
					os_name = xpp.getAttributeValue(null, "name");
					if (os_name != null) os_info += os_name;
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				if (xpp.getName().contentEquals("port")) {
					in_port = false;
					if (add_port) {
						portlist.add(port);
						if (port.portnumber == 135) win_ports+=port.portnumber;
						if (port.portnumber == 139) win_ports+=port.portnumber;
						if (port.portnumber == 445) win_ports+=port.portnumber;
					}
				}
				else if (xpp.getName().contentEquals("host")) {
					os_icon = R.drawable.globes;
					
					HashMap<String, Object> item = new HashMap<String, Object>();
					item.put("ip", host);
					item.put("hostname", hostname);
					item.put("ports", portlist);
					item.put("icon", os_icon);
					item.put("mac", mac_addr);
					if (found_vul_MS08_067)
						item.put("found_vul_MS08_067", 1);
					else
						item.put("found_vul_MS08_067", 0);
                                        if (found_vul_MS12_020)
                                                item.put("found_vul_MS12_020", 1);
                                        else
                                                item.put("found_vul_MS12_020", 0);
                                        if (found_vul_iphone_ssh)
                                                item.put("found_vul_iphone_ssh", 1);
                                        else
                                                item.put("found_vul_iphone_ssh", 0);
                                        if (found_vul_java)
                                                item.put("found_vul_java", 1);
                                        else
                                                item.put("found_vul_java", 0);
                                        if (found_plesk) {
                                        	item.put("found_plesk", 1);
                                        } else {
                                        	item.put("found_plesk", 0);
                                        }
					
					hosts.add(item);
                                        found_vul_MS08_067 = false;
                                        found_vul_MS12_020 = false;
					found_vul_iphone_ssh = false;
					found_vul_java = false;
					host = "";
					hostname = "";
					mac_vendor = "";
					mac_addr = "";
					os_name = "";
					os_vendor = "";
					os_family = "";
					os_info = "";
					os_icon = R.drawable.globes;
					win_ports = 0;
					portlist = new ArrayList<Port>();
				}
			} else if (eventType == XmlPullParser.TEXT) {
			}
			try {
                                eventType = xpp.next();
                        } catch (ArrayIndexOutOfBoundsException e) {
                                e.printStackTrace();
                        } catch (XmlPullParserException e) {
                                e.printStackTrace();
                        }
		}
		return hosts;
	}
	
	public static class Port {
		public int portnumber;
		public String portdescription;

		@Override
		public boolean equals(Object o) {
			if (o instanceof Port) {
				return ((Port) o).portnumber == portnumber;
			}
			return super.equals(o);
		}
	}

}
