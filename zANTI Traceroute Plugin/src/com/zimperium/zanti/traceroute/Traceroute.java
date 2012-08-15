package com.zimperium.zanti.traceroute;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.PathDashPathEffect;
import android.graphics.PathDashPathEffect.Style;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.zimperium.zanti.plugins.AntiPlugin;
import com.zimperium.zanti.traceroute.ShellPool.ShellResult;
import com.zimperium.zanti.traceroute.ShellPool.ShellTask;

public class Traceroute extends MapActivity {

	List<RouteNode> routelist = new LinkedList<Traceroute.RouteNode>();
	List<RouteNode> runninglist = new LinkedList<Traceroute.RouteNode>();
	Date displayFloatingDate = new Date();

	RelativeLayout floating;
	ListView lv, lvfloating;
	String target;
	boolean RunningTrace = false;
	boolean TraceComplete = false;
	MapView mapView;
	NodeOverlay nodeOverlay;
	ImageButton traceroutebutton;
	LinearLayout mapnavigator;
	String CurrentScanTarget = "";

	RelativeLayout walkthroughtooltip;
	ImageButton walkthrough_next, walkthrough_prev;
	TextView walkthrough_loc, walkthrough_host;

	RouteListAdapter adapter = new RouteListAdapter(routelist);
	RouteListAdapter adapterfloating = new RouteListAdapter(routelist);
	Object Lock = new Object();

	boolean WalkSuccess = false;

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (TraceComplete) {
			return routelist;
		}
		return null;
	}

	@Override
	protected void onPause() {
		super.onPause();
		for (RouteNode rn : runninglist) {
			rn.Cancel();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.traceroute_list);

		target = getIntent().getStringExtra(AntiPlugin.EXTRA_TARGET);

		walkthroughtooltip = (RelativeLayout) findViewById(R.id.walkthroughtooltip);
		walkthrough_next = (ImageButton) findViewById(R.id.nextbutton);
		walkthrough_prev = (ImageButton) findViewById(R.id.prevbutton);
		walkthrough_loc = (TextView) findViewById(R.id.walkthroughcity);
		walkthrough_host = (TextView) findViewById(R.id.walkthroughhost);

		lv = (ListView) findViewById(R.id.listview);
		lvfloating = (ListView) findViewById(R.id.listviewfloating);
		floating = (RelativeLayout) findViewById(R.id.floating);
		traceroutebutton = (ImageButton) findViewById(R.id.traceroutebutton);
		mapnavigator = (LinearLayout) findViewById(R.id.mapnavigator);
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.getController().setZoom(2);

		mapView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() != MotionEvent.ACTION_DOWN)
					return false;
				Date now = new Date();
				if (now.getTime() - displayFloatingDate.getTime() > 1000 && floating.getVisibility() == View.VISIBLE) {
					floating.setVisibility(View.GONE);
					FlyOut(floating);
				}

				if (TraceComplete && mapnavigator.getVisibility() == View.GONE) {
					mapnavigator.setVisibility(View.VISIBLE);
					FadeIn(mapnavigator);
				}

				if (walkthroughtooltip.getVisibility() == View.VISIBLE) {
					walkthroughtooltip.setVisibility(View.GONE);
					FlyOut(walkthroughtooltip);
				}
				return false;
			}
		});

		lv.setAdapter(adapter);
		lvfloating.setAdapter(adapterfloating);

		traceroutebutton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				maptraceprogress = -1;

				Runnable runnable = new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (WalkSuccess) {
							WalkSuccess = AnimateToNext(1, this);
						}
					}
				};

				WalkSuccess = AnimateToNext(1, runnable);
				// mapnavigator.setVisibility(View.GONE);
				// FadeOut(mapnavigator);
				walkthroughtooltip.setVisibility(View.VISIBLE);
				FlyIn(walkthroughtooltip);
			}
		});

		walkthrough_next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AnimateToNext(1, null);
				if (walkthroughtooltip.getVisibility() != View.VISIBLE) {
					walkthroughtooltip.setVisibility(View.VISIBLE);
					FlyIn(walkthroughtooltip);
				}
			}
		});

		walkthrough_prev.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AnimateToNext(-1, null);
				if (walkthroughtooltip.getVisibility() != View.VISIBLE) {
					walkthroughtooltip.setVisibility(View.VISIBLE);
					FlyIn(walkthroughtooltip);
				}
			}
		});

		RelativeLayout scaninprogress = (RelativeLayout) findViewById(R.id.scaninprogress);
		RelativeLayout noscaninprogress = (RelativeLayout) findViewById(R.id.noscaninprogress);
		RelativeLayout scancomplete = (RelativeLayout) findViewById(R.id.scancomplete);

		CurrentScanTarget = target;

		Object o = getLastNonConfigurationInstance();
		if (o != null && o instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<RouteNode> lastroutelist = (List<RouteNode>) (o);
			routelist.addAll(lastroutelist);
			TraceCompleted();
		} else {
			startTraceRoute(target);

			noscaninprogress.setVisibility(View.GONE);
			scaninprogress.setVisibility(View.VISIBLE);
			scancomplete.setVisibility(View.GONE);
			mapnavigator.setVisibility(View.GONE);

			TextView loadingtext = (TextView) findViewById(R.id.loadingtext);

			loadingtext.setText("Tracing " + target);
			UpdateScanText();
		}
	}

	void UpdateScanText() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				TextView loadingtextsub = (TextView) findViewById(R.id.loadingtextsub);
				loadingtextsub.setText("" + routelist.size() + "/" + (routelist.size() + runninglist.size()) + " nodes traced successfully.");
			}
		});
	}

	protected void startTraceRoute(final String text) {
		synchronized (Lock) {
			RunningTrace = true;
			TraceComplete = false;
			for (RouteNode rn : runninglist) {
				rn.Cancel();
			}
			routelist.clear();
			Refresh();
			runninglist.clear();
			List<Overlay> overlays = mapView.getOverlays();
			overlays.clear();
			nodeOverlay = null;

			for (int i = 1; i < 21; i++) {
				// -- it's a good balance to make sure we don't try scan forever
				runninglist.add(new RouteNode(text, i));
			}
		}
	}

	private void Refresh() {
		adapter.Refresh();
		adapterfloating.Refresh();
		DisplayMapPoints();
	}

	public synchronized void RouteNodeDone(final RouteNode rn) {
		if (rn.Cancelled)
			return;
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				synchronized (Lock) {
					runninglist.remove(rn);
					if (rn.IsTarget) {
						for (int i = 0; i < runninglist.size(); i++) {
							RouteNode rn2 = runninglist.get(i);
							if (rn2.hops > rn.hops) {
								runninglist.remove(i);
								i--;
								rn2.Cancel();
							}
						}
					}
					boolean inserted = false;
					for (int i = 0; i < routelist.size(); i++) {
						if (routelist.get(i).hops > rn.hops) {
							inserted = true;
							routelist.add(i, rn);
							break;
						}
					}
					if (!inserted)
						routelist.add(rn);
					if (runninglist.isEmpty()) {
						TraceCompleted();
					}
					RemoveDuplicateTargets();
					Refresh();
					UpdateScanText();
				}
			}
		});
	}

	public void RemoveDuplicateTargets() {
		boolean FoundTarget = false;
		for (int i = 0; i < routelist.size(); i++) {
			if (FoundTarget) {
				routelist.remove(i);
				i--;
			}
			if (routelist.get(i).IsTarget) {
				FoundTarget = true;
			}
		}
	}

	int maptraceprogress;
	public synchronized void TraceCompleted() {
		RunningTrace = false;
		TraceComplete = true;
		Refresh();

		RelativeLayout scaninprogress = (RelativeLayout) findViewById(R.id.scaninprogress);
		RelativeLayout noscaninprogress = (RelativeLayout) findViewById(R.id.noscaninprogress);
		RelativeLayout scancomplete = (RelativeLayout) findViewById(R.id.scancomplete);

		noscaninprogress.setVisibility(View.GONE);
		scaninprogress.setVisibility(View.GONE);
		scancomplete.setVisibility(View.VISIBLE);
		mapnavigator.setVisibility(View.VISIBLE);
		FadeIn(mapnavigator);

		TextView scancompletetext = (TextView) findViewById(R.id.scancompletetext);
		scancompletetext.setText("Displaying trace results for \n" + CurrentScanTarget);
	}

	public boolean AnimateToNext(int change, Runnable runnable) {
		try {
			Log.i("AnimateToNext", "Animating from " + maptraceprogress + " by " + change);
			maptraceprogress += change;
			RouteNode nr = routelist.get(maptraceprogress);
			while (change != 0 && !nr.HasMapPoint) {
				maptraceprogress += change;
				if (maptraceprogress < 0) {
					Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
					walkthroughtooltip.startAnimation(shake);
					maptraceprogress = 0;
					return false;
				}
				if (maptraceprogress >= routelist.size()) {
					Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
					walkthroughtooltip.startAnimation(shake);
					maptraceprogress = routelist.size() - 1;
					return false;
				}
				nr = routelist.get(maptraceprogress);
			}
			Log.i("AnimateToNext", "Animating at " + maptraceprogress);
			MapController mc = mapView.getController();
			walkthrough_loc.setText(nr.AreaTitle);
			walkthrough_host.setText(nr.host);

			Projection proj = mapView.getProjection();
			GeoPoint p = new GeoPoint((int) (nr.Latitude * 1E6), (int) (nr.Longitude * 1E6));
			Point point = proj.toPixels(p, null);
			final float scale = getResources().getDisplayMetrics().density;
			p = proj.fromPixels(point.x, point.y - (int) (85 * scale));
			if (runnable != null)
				mc.animateTo(p, runnable);
			else
				mc.animateTo(p);
			return true;
		} catch (IndexOutOfBoundsException ex) {
			if (maptraceprogress < 0) {
				Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
				walkthroughtooltip.startAnimation(shake);
				maptraceprogress = 0;
				return false;
			}
			if (maptraceprogress >= routelist.size()) {
				Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
				walkthroughtooltip.startAnimation(shake);
				maptraceprogress = routelist.size() - 1;
				return false;
			}
			return false;
		}
	}

	public synchronized void RouteNodeFailed(final RouteNode rn) {
		if (rn.Cancelled)
			return;
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				synchronized (Lock) {
					runninglist.remove(rn);
					if (runninglist.isEmpty()) {
						TraceCompleted();
					}
					UpdateScanText();
				}
			}
		});

	}

	public class RouteListAdapter implements ListAdapter {

		List<DataSetObserver> obslist = new ArrayList<DataSetObserver>();
		List<RouteNode> list;

		public RouteListAdapter(List<RouteNode> list) {
			this.list = list;
		}

		@Override
		public int getCount() {
			synchronized (Lock) {
				int count = list.size();
				// if (RunningTrace)
				// count++;
				return count;
			}
		}

		@Override
		public Object getItem(int position) {
			synchronized (Lock) {
				if (RunningTrace && position >= list.size()) {
					return null;
				} else {
					try {
						return list.get(position);
					} catch (Exception ex) {
						return null;
					}
				}
			}
		}

		@Override
		public long getItemId(int position) {
			synchronized (Lock) {
				if (RunningTrace && position >= list.size()) {
					return 999;
				} else {
					RouteNode rn = list.get(position);
					if (rn == null)
						return 0;
					return rn.hops;
				}
			}
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			synchronized (Lock) {
				if (convertView == null) {
					convertView = getLayoutInflater().inflate(R.layout.traceroute_item, null);
				}

				final RouteNode rn = (RouteNode) getItem(position);
				if (rn == null)
					return convertView;

				TextView ipaddress = (TextView) convertView.findViewById(R.id.ipaddress);
				TextView nameaddress = (TextView) convertView.findViewById(R.id.nameaddress);
				TextView ms = (TextView) convertView.findViewById(R.id.ms);
				TextView ttl = (TextView) convertView.findViewById(R.id.ttl);
				TextView location = (TextView) convertView.findViewById(R.id.location);
				Button ViewOnMapButton = (Button) convertView.findViewById(R.id.ViewOnMapButton);
				if (ipaddress == null || ms == null)
					return convertView;
				if (rn.HasMapPoint) {
					ViewOnMapButton.setVisibility(View.VISIBLE);
					ViewOnMapButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							SlidingDrawer drawer = (SlidingDrawer) findViewById(R.id.drawer);
							if (drawer.isOpened()) {
								drawer.animateClose();
							}
							mapView.getController().animateTo(new GeoPoint((int) (rn.Latitude * 1E6), (int) (rn.Longitude * 1E6)));
							if (floating.getVisibility() == View.VISIBLE) {
								floating.setVisibility(View.GONE);
								FlyOut(floating);
							}
							if (walkthroughtooltip.getVisibility() == View.VISIBLE) {
								walkthroughtooltip.setVisibility(View.GONE);
								FlyOut(walkthroughtooltip);
							}
						}
					});
				} else {
					ViewOnMapButton.setVisibility(View.GONE);
				}
				location.setText(rn.AreaTitle);
				ttl.setText("" + rn.hops);
				nameaddress.setVisibility(View.VISIBLE);
				ms.setTextColor(Color.GRAY);
				if (rn.TimedOut) {
					ipaddress.setText("Timed Out");
					ms.setText("");
					nameaddress.setVisibility(View.GONE);
				} else if (rn.ErroredOut) {
					ipaddress.setText("Errored Out");
					ms.setText("");
					nameaddress.setVisibility(View.GONE);
				} else {
					ipaddress.setText(rn.addr);
					nameaddress.setText(rn.host);
					if (rn.ms < 0) {
						ms.setText("Roundtrip time: Unknown");
					} else {
						// ms.setText("Roundtrip time: " +
						// NumberFormat.getNumberInstance().format(rn.ms)+"ms");
						ms.setText("Roundtrip time: " + (int) (rn.ms) + "ms");
						if (rn.ms < 150)
							ms.setTextColor(0xFF007700);
						else if (rn.ms < 600)
							ms.setTextColor(0xFF777700);
						else
							ms.setTextColor(0xFF770000);
					}
				}
				return convertView;
			}
			// }
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			obslist.add(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			obslist.remove(observer);
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		public void Refresh() {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					for (DataSetObserver obs : obslist) {
						obs.onChanged();
					}
				}
			});
		}

	}

	public class RouteNodeOverlayItem extends OverlayItem {

		public List<RouteNode> rnlist = new ArrayList<Traceroute.RouteNode>();

		public RouteNodeOverlayItem(GeoPoint point, RouteNode node) {
			super(point, node.AreaTitle, "");
			rnlist.add(node);
		}

		public boolean AddNode(GeoPoint p2, RouteNode node) {
			// RouteNode rn2 = rnlist.get(0);
			GeoPoint p1 = getPoint();
			if (Math.abs(p1.getLatitudeE6() - p2.getLatitudeE6()) < mapView.getLatitudeSpan() / 10) {
				if (Math.abs(p1.getLongitudeE6() - p2.getLongitudeE6()) < mapView.getLongitudeSpan() / 10) {
					rnlist.add(node);
					return true;
				}
			}
			return false;
		}

	}

	public class RouteNode {
		public String target, host, addr = null;
		public int hops;
		public double ms = -1;
		public int mscount = 0;
		public double Latitude, Longitude;
		public boolean TimedOut = false;
		public boolean Finished = false, ErroredOut = false, IsTarget = false, Cancelled = false;
		public boolean HasMapPoint = false;
		public String AreaTitle = "Unknown";
		Future<ShellResult> future;

		// non-root traceroute
		public RouteNode(String targ, int hop) {
			this.target = targ;
			this.hops = hop;

			new Thread() {
				public void run() {
					try {
						synchronized (RouteNode.this) {
							if (Finished)
								return;
							future = ShellPool.submitTaskAsync(new ShellTask(null, "ping -c 3 -n -t " + hops + " " + target, true, false, getApplicationContext()));
						}
						// DataInputStream inputStream = new
						// DataInputStream(process.getInputStream());
						ShellResult r = future.get();
						List<String> lines = r.result;

						for (String str : lines) {
							if (str.contains("Time to live exceeded")) {
								String IP = str.split(" ")[1];
								addr = IP;
								host = IP;
							}
							if (str.contains("64 bytes from ")) {
								if (addr == null) {
									addr = str.split(" ")[3].replace(":", "");
								}
								String[] msparsea = str.split("=");
								double msparse = Double.parseDouble(msparsea[msparsea.length - 1].replace(" ms", ""));
								if (mscount == 0) {
									ms = msparse;
									mscount++;
								} else {
									ms = ((mscount * ms) + msparse) / (mscount + 1);
									mscount++;
								}
								host = target;
								IsTarget = true;
							}
						}

						synchronized (RouteNode.this) {
							if (addr == null) {
								TimedOut = true;
							}
						}
						if (Finished)
							return;
						GetLocation();
						Refresh();

						if (Finished)
							return;
						String hostname = InetAddress.getByName(addr).getHostName();
						host = hostname;
						RouteNodeDone(RouteNode.this);
						Refresh();
						if (mscount > 0) {
							Finished = true;
							return;
						}
						if (Finished) {
							return;
						}

						future = ShellPool.submitTaskAsync(new ShellTask(null, "ping -c 3 -n " + addr, true, false, getApplicationContext()));
						r = future.get();
						lines = r.result;
						for (String str : lines) {
							if (str.contains("64 bytes from ")) {
								String[] msparsea = str.split("=");
								double msparse = Double.parseDouble(msparsea[msparsea.length - 1].replace(" ms", ""));
								if (mscount == 0) {
									ms = msparse;
									mscount++;
								} else {
									ms = ((mscount * ms) + msparse) / (mscount + 1);
									mscount++;
								}
							}
						}

						Refresh();
						Finished = true;
					} catch (Exception ex) {
						synchronized (RouteNode.this) {
							if (future != null) {
								future.cancel(true);
							}
						}
						RouteNodeFailed(RouteNode.this);
						ex.printStackTrace();
					}
				};
			}.start();
		}

		// already populated root traceroute
		public RouteNode(String IP, int hop, double ms) {
			this.addr = IP;
			this.hops = hop;
			this.ms = ms;
			new Thread() {
				public void run() {
					try {
						if (Finished)
							return;
						GetLocation();
						String hostname = InetAddress.getByName(addr).getHostName();
						host = hostname;
						RouteNodeDone(RouteNode.this);
						Finished = true;
					} catch (Exception ex) {
						ex.printStackTrace();
						RouteNodeFailed(RouteNode.this);
					}
				};
			}.start();
		}

		public void Cancel() {
			synchronized (RouteNode.this) {
				if (Finished)
					return;
				Finished = true;
				Cancelled = true;
				if (future != null)
					future.cancel(true);
			}
		}

		public void GetLocation() {
			try {
				String url = "http://www.mobile-dev.co.za/geoip/php/sample_city.php?ip=" + addr;
				JSONObject json = new JSONObject(queryRESTurl(url));
				if (json.has("latitude") && json.has("longitude")) {
					Latitude = json.optDouble("latitude", 0);
					Longitude = json.optDouble("longitude", 0);
					Log.i("loc", "lat:" + Latitude);
					Log.i("loc", "long:" + Longitude);
					if (Latitude != 0 || Longitude != 0) {
						HasMapPoint = true;
					}
				}
				if (json.has("cityName") && !json.optString("cityName").equals("null")) {
					AreaTitle = json.optString("cityName") + ", " + json.optString("countryName");
				} else if (json.has("regionName") && !json.optString("regionName").equals("null")) {
					AreaTitle = json.optString("regionName") + ", " + json.optString("countryName");
				} else if (json.has("countryName") && !json.optString("countryName").equals("null")) {
					AreaTitle = json.optString("countryName");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static String queryRESTurl(String url) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		HttpResponse response;

		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				InputStream instream = entity.getContent();
				StringBuilder builder = new StringBuilder();
				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				instream.close();
				return builder.toString();
			}
		} catch (ClientProtocolException e) {
			Log.e("REST", "There was a protocol based error", e);
		} catch (IOException e) {
			Log.e("REST", "There was an IO Stream related error", e);
		}

		return null;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public void DisplayMapPoints() {
		if (nodeOverlay == null) {
			boolean empty = true;
			for (RouteNode rn : routelist) {
				if (rn.HasMapPoint) {
					empty = false;
				}
			}
			if (empty)
				return;
			List<Overlay> overlays = mapView.getOverlays();
			nodeOverlay = new NodeOverlay();
			overlays.add(nodeOverlay);
			// Updates our nodes when zoom changes
			overlays.add(new ZoomChangeOverlay());
		}

		nodeOverlay.SetNodes(routelist);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mapView.invalidate();
			}
		});
	}

	public class NodeOverlay extends ItemizedOverlay<RouteNodeOverlayItem> {

		List<RouteNodeOverlayItem> overlaylist = new ArrayList<Traceroute.RouteNodeOverlayItem>();

		@Override
		public boolean onTap(GeoPoint p, MapView mapView) {
			try {
				return super.onTap(p, mapView);
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}
		}

		public NodeOverlay() {
			super(boundCenterBottom(getResources().getDrawable(R.drawable.blue_dot)));
		}

		public void SetNodes(List<RouteNode> list) {
			synchronized (overlaylist) {
				overlaylist.clear();
				for (RouteNode rn : list) {
					if (rn.HasMapPoint) {
						GeoPoint gp1 = new GeoPoint((int) (rn.Latitude * 1E6), (int) (rn.Longitude * 1E6));
						boolean added = false;
						for (RouteNodeOverlayItem rni : overlaylist) {
							if (rni.AddNode(gp1, rn)) {
								added = true;
								break;
							}
						}
						if (!added) {
							overlaylist.add(new RouteNodeOverlayItem(gp1, rn));
						}
					}
				}
				populate();
			}
		}

		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
			if (shadow) {
				return super.draw(canvas, mapView, shadow, when);
			}
			Projection projection = mapView.getProjection();

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(Color.RED);
			paint.setStrokeWidth(3);
			paint.setStrokeJoin(Paint.Join.ROUND);
			paint.setStrokeCap(Paint.Cap.ROUND);

			if (mapView.getZoomLevel() < 5) {
				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				Path path = new Path();
				path.setFillType(FillType.INVERSE_EVEN_ODD);
				path.moveTo(-1, 1);
				path.lineTo(4, 4);
				path.lineTo(5, 3);
				path.lineTo(1, 1);
				path.lineTo(10, 1);
				path.lineTo(10, -1);
				path.lineTo(1, -1);
				path.lineTo(5, -3);
				path.lineTo(4, -4);
				path.lineTo(-1, -1);

				PathDashPathEffect dashPath2 = new PathDashPathEffect(path, 20, 20, Style.MORPH);
				paint.setPathEffect(dashPath2);
			}
			paint.setAlpha(120);

			synchronized (overlaylist) {
				Point lp = null;
				for (RouteNodeOverlayItem rni : overlaylist) {
					Point p = new Point();
					projection.toPixels(rni.getPoint(), p);
					if (lp != null) {
						canvas.drawLine(p.x, p.y, lp.x, lp.y, paint);
					}
					lp = p;
				}
			}

			return super.draw(canvas, mapView, shadow, when);
		}

		@Override
		protected RouteNodeOverlayItem createItem(int i) {
			// Log.i("createItem Called", "createItem Called");
			synchronized (overlaylist) {
				return overlaylist.get(i);
			}
		}

		@Override
		public int size() {
			// Log.i("Size Called", "Size Called: "+overlaylist.size());
			synchronized (overlaylist) {
				return overlaylist.size();
			}
		}

		@Override
		protected boolean onTap(int index) {
			RouteNodeOverlayItem rni = overlaylist.get(index);
			Log.i("Tapped", "" + index);
			floating.setVisibility(View.VISIBLE);
			FlyIn(floating);
			mapnavigator.setVisibility(View.GONE);
			FadeOut(mapnavigator);
			adapterfloating.list = new ArrayList<Traceroute.RouteNode>();
			adapterfloating.list.addAll(rni.rnlist);
			adapterfloating.Refresh();
			TextView headerfloating = (TextView) findViewById(R.id.headerfloating);
			headerfloating.setText(rni.getTitle());
			MapController mc = mapView.getController();
			GeoPoint gp = rni.getPoint();
			Projection proj = mapView.getProjection();
			GeoPoint p = new GeoPoint(gp.getLatitudeE6(), gp.getLongitudeE6());
			Point point = proj.toPixels(p, null);
			// 32 = map marker height
			final float scale = getResources().getDisplayMetrics().density;
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				p = proj.fromPixels(point.x, point.y - ((int) (260 * scale) - mapView.getHeight() / 2) - 32);
			} else {
				p = proj.fromPixels(point.x, point.y - ((int) (300 * scale) - mapView.getHeight() / 2) - 32);
			}
			mc.animateTo(p);
			return true;
		}
	}

	@Override
	public void onBackPressed() {
		SlidingDrawer drawer = (SlidingDrawer) findViewById(R.id.drawer);
		if (drawer.isOpened()) {
			drawer.animateClose();
			return;
		}
		if (floating.getVisibility() == View.VISIBLE) {
			floating.setVisibility(View.GONE);
			FlyOut(floating);
			mapnavigator.setVisibility(View.VISIBLE);
			FadeIn(mapnavigator);
			return;
		}
		if (walkthroughtooltip.getVisibility() == View.VISIBLE) {
			walkthroughtooltip.setVisibility(View.GONE);
			FlyOut(walkthroughtooltip);
			mapnavigator.setVisibility(View.VISIBLE);
			FadeIn(mapnavigator);
			return;
		}
		super.onBackPressed();
	}

	public class ZoomChangeOverlay extends Overlay {

		private static final String DEBUG_TAG = "ZoomChangeOverlay";
		private int mPrevZoom = -1;

		public ZoomChangeOverlay() {
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);

			if (shadow)
				return;

			// We need to sync at this point so we know that we won't be
			// checking or
			// updating mPrevZoom while we're already checking/updating them.
			synchronized (this) {
				// Check the zoom level. We only do the callback if this is
				// different.
				int newZoom = mapView.getZoomLevel();
				if (newZoom != mPrevZoom) {
					Log.d(DEBUG_TAG, "Zoom level changed from " + mPrevZoom + " to " + newZoom);
					// Also, we only do the callback after we know the initial
					// zoom.
					if (mPrevZoom != -1) {
						DisplayMapPoints();
					}

					mPrevZoom = newZoom;
				}
			}
		}
	}

	class XmlTracerouteParser {
		XmlPullParserFactory factory;
		String filename;

		public XmlTracerouteParser(String filename) throws XmlPullParserException, FileNotFoundException {
			super();
			this.filename = filename;
			factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
		}

		public List<RouteNode> Parse() throws XmlPullParserException, IOException {

			XmlPullParser xpp;
			xpp = factory.newPullParser();
			FileInputStream fis = new FileInputStream(new File(filename));
			xpp.setInput(new InputStreamReader(fis));
			int eventType = xpp.getEventType();
			// ArrayList<String> HostsAndPorts = new ArrayList<String>();

			List<RouteNode> list = new ArrayList<Traceroute.RouteNode>();

			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_DOCUMENT) {
				} else if (eventType == XmlPullParser.END_DOCUMENT) {
				} else if (eventType == XmlPullParser.START_TAG) {
					String nodeName = xpp.getName();
					// Log.d("-", nodeName);
					if (nodeName.contentEquals("hop")) {
						int ttl = Integer.parseInt(xpp.getAttributeValue(null, "ttl"));
						String ipaddr = xpp.getAttributeValue(null, "ipaddr");
						double rtt = Double.parseDouble(xpp.getAttributeValue(null, "rtt"));
						Log.i("ANT", "Found node " + ipaddr);
						list.add(new RouteNode(ipaddr, ttl, rtt));
					}
				} else if (eventType == XmlPullParser.END_TAG) {
				} else if (eventType == XmlPullParser.TEXT) {
				}
				eventType = xpp.next();
			}
			return list;
		}

	}

	public void FadeIn(View v) {
		AlphaAnimation animation2 = new AlphaAnimation(0.0f, 1.0f);
		animation2.setDuration(500);
		v.startAnimation(animation2);
	}

	public void FadeOut(View v) {
		AlphaAnimation animation2 = new AlphaAnimation(1.0f, 0.0f);
		animation2.setDuration(500);
		v.startAnimation(animation2);
	}

	public void FlyIn(View v) {
		AnimationSet as = new AnimationSet(true);
		TranslateAnimation animation = new TranslateAnimation(0f, 0f, -100f, 0f);
		animation.setDuration(500);
		AlphaAnimation animation2 = new AlphaAnimation(0.0f, 1.0f);
		animation2.setDuration(500);
		as.addAnimation(animation);
		as.addAnimation(animation2);
		v.startAnimation(as);
	}

	public void FlyOut(View v) {
		AnimationSet as = new AnimationSet(true);
		TranslateAnimation animation = new TranslateAnimation(0f, 0f, 0f, -100f);
		animation.setDuration(500);
		AlphaAnimation animation2 = new AlphaAnimation(1.0f, 0.0f);
		animation2.setDuration(500);
		as.addAnimation(animation);
		as.addAnimation(animation2);
		v.startAnimation(as);
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.fadein, R.anim.fadeout);
	}
}

class AnimateStatus {

	boolean Success;

}