package com.zimperium.zanti.cracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zimperium.zanti.cracker.CrackerOptions.Protocol;
import com.zimperium.zanti.plugins.AntiPlugin;

public class Cracker extends Activity {
	
	Typewriter title;
	TextView progress_text;
	TextView close;
	ProgressBar progress;
	ProgressBar circle;
	ListView listview;
	MyCustomBaseAdapter listadapter;

	Process process;
	DataOutputStream outputStream;
	DataInputStream inputStream;
	DataInputStream errorStream;

	String errorMessage = "";

	DefinedCrackerOption crackerOption;

	ArrayList<UserPassPort> passport;

	private class UserPassPort {
		String user;
		String pass;
		Protocol protocol;
	}

	boolean success = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.cracker_output);

		crackerOption = (DefinedCrackerOption) getIntent().getSerializableExtra("DefinedCrackerOption");

		ImageView menu = (ImageView) findViewById(R.id.menu);
		menu.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent sharingIntent = new Intent(Intent.ACTION_SEND);
				sharingIntent.setType("text/plain");
				String tweet;
				if (success)
					tweet = "Successfully cracked password on " + crackerOption.Target
							+ " using Android Network Toolkit (#ANTI3) by @zImperium Get it now: http://zimperium.com/anti.html";
				else
					tweet = "Cracking password on " + crackerOption.Target + " using Android Network Toolkit (#ANTI3) by @zImperium Get it now: http://zimperium.com/anti.html";
				sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, tweet);
				sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Anti");
				startActivity(Intent.createChooser(sharingIntent, "Share using"));
			}
		});

		final TextView xtitle = (TextView) findViewById(R.id.title);
		xtitle.setText("Cracking @ " + crackerOption.Target);
		final TextView xheader = (TextView) findViewById(R.id.header);
		xheader.setText("Status");

		progress_text = (TextView) findViewById(R.id.progress_text);

		passport = new ArrayList<UserPassPort>();

		listview = (ListView) findViewById(R.id.listview);
		listadapter = new MyCustomBaseAdapter(getApplicationContext(), passport);
		listview.setAdapter(listadapter);
		listview.setClickable(true);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

				UserPassPort userpass = (UserPassPort) listview.getItemAtPosition(position);
				int port = userpass.protocol.port;
				String user = userpass.user;
				String pass = userpass.pass;
				Log.d("-Anti-", String.format("Connecting to %s port %d user %s pass %s", crackerOption.Target, port, user, pass));
				Helpers.ExecutePort(getApplicationContext(), crackerOption.Target, port, user, pass);
			}
		});
		FrameLayout header = (FrameLayout) findViewById(R.id.scroll_view);
		title = new Typewriter(this);
		header.addView(title);
		title.animateText("Initializing...");

		close = (TextView) findViewById(R.id.close);
		close.setVisibility(View.INVISIBLE);
		close.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});
		progress = (ProgressBar) findViewById(R.id.progressBar1);
		circle = (ProgressBar) findViewById(R.id.progressBar2);

		try {
			process = Runtime.getRuntime().exec("sh");
			outputStream = new DataOutputStream(process.getOutputStream());
			inputStream = new DataInputStream(process.getInputStream());
			errorStream = new DataInputStream(process.getErrorStream());

			outputStream.writeBytes("export LD_LIBRARY_PATH=" + CrackerOptions.DIR_LIBS + ":$LD_LIBRARY_PATH\n");
			outputStream.writeBytes("cd " + CrackerOptions.DIR_HYDRA + "\n");
			outputStream.flush();

			CrackPort cracker = new CrackPort();
			cracker.execute(crackerOption.cracker_protocols.toArray(new Protocol[0]));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
		try {
			outputStream.writeBytes("\n" + "killall hydra\n");
			outputStream.flush();
			// outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			process.getInputStream().close();
			process.getOutputStream().close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private class CrackPort extends AsyncTask<Protocol, Integer, Void> {

		int max = 0;
		UserPassPort userPassPort;
		String currentService;

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			// showDialog(PROGRESS_DIALOG);
			// progressDialog.incrementProgressBy(1);
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			try {
				int val = values[0];
				switch (val) {
					case 0 :
						break;
					case -1 :
						progress.setMax(max + progress.getProgress());
						break;
					case -2 :
						title.animateText("Waiting for children...");
						break;
					default :
						title.animateText("Cracking " + crackerOption.Target + " service " + currentService);
						break;
				}
				progress.incrementProgressBy(1);
				if (max == 0)
					return;
				progress_text.setText(progress.getProgress() + "/" + max);
			} catch (Exception e) {
				// e.printStackTrace();
			}
			super.onProgressUpdate(values);
		}

		@Override
		protected Void doInBackground(Protocol... params) {
			for (Protocol protocol : params) {
				try {
					// publishProgress(port);
					// String protocol = Port2Protocol(port);
					if (protocol == null || protocol.name == null)
						continue;
					String cmd = CrackerCore.generateHydraCommand(crackerOption.selected_crack_type, crackerOption.crack_incremental_options, protocol.port, crackerOption.Target,
							protocol.name, crackerOption.custom_pass_file);

					Log.d("-Anti-", "cmd= " + cmd);
					outputStream.writeBytes("rm hydra.restore\n");
					outputStream.flush();
					outputStream.writeBytes(cmd + "\n");
					outputStream.flush();

					currentService = protocol.name;
					publishProgress(1);

					String line;
					boolean done = false;
					while (!done) {
						while (((line = inputStream.readLine()) != null)) {
							Log.d("-", line);
							if (line.startsWith("[")) {
								if (line.contains("login: ") && line.contains("password: ")) {
									String[] _user = line.split("login: ", 2);
									String[] _pass = line.split("password: ", 2);
									userPassPort = new UserPassPort();
									userPassPort.user = _user[1].trim().split(" ", 2)[0].trim();
									userPassPort.pass = _pass[1].trim();
									userPassPort.protocol = protocol;
									Log.d("-Anti-", "found user/pass for " + crackerOption.Target + " port " + protocol.port + " : " + userPassPort.user + "/" + userPassPort.pass);
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											passport.add(userPassPort);
											listadapter.notifyDataSetChanged();
										}
									});

									success = true;
								} else if (line.startsWith("[STATUS] attack finished")) {
									publishProgress(-2);
								} else if (line.startsWith("[DATA]") && line.contains("login tries")) {
									String[] _commas = line.split(",", 3);
									long nmax = Long.valueOf(_commas[2].trim().split(" ", 2)[0].trim());
									nmax += max;
									if (nmax > Integer.MAX_VALUE)
										max = Integer.MAX_VALUE;
									else
										max = (int) nmax;
									Log.d("-Anti-", "max=" + max);
									publishProgress(-1);
								}
							} else if (line.startsWith("Hydra (http://www.thc.org/thc-hydra) finished")) {
								break;
							} else if (line.startsWith("DONE")) {
								done = true;
								break;
							} else if (line.startsWith("Error: ")) {
								if (!errorMessage.contains(line))
									errorMessage += line + "\n";
								break;
							} else if (line.startsWith("Segmentation fault")) {
								break;
							}
							publishProgress(0);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			return null;
		}

	}

	public class Typewriter extends TextView {

		private CharSequence mText;
		private int mIndex;
		private long mDelay = 30;

		public Typewriter(Context context) {
			super(context);
			setTextSize(34);
			setTextColor(Color.GREEN);
			setTypeface(Typeface.createFromAsset(getAssets(), "DS-DIGI.TTF"), Typeface.BOLD);
		}

		public Typewriter(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		private Handler mHandler = new Handler();
		private Runnable characterAdder = new Runnable() {
			@Override
			public void run() {
				setText(mText.subSequence(0, mIndex++));
				if (mIndex <= mText.length()) {
					mHandler.postDelayed(characterAdder, mDelay);
				}
			}
		};

		@Override
		public void setText(CharSequence text, BufferType type) {
			super.setText(text, type);
			// mText = text;
		}

		public void animateText(CharSequence text) {
			mText = text;
			mIndex = 0;

			setText("");
			mHandler.removeCallbacks(characterAdder);
			mHandler.postDelayed(characterAdder, mDelay);
		}

		public void setCharacterDelay(long millis) {
			mDelay = millis;
		}
	}

	public class MyCustomBaseAdapter extends BaseAdapter {
		private ArrayList<UserPassPort> userpass_array;

		private LayoutInflater mInflater;

		public MyCustomBaseAdapter(Context context, ArrayList<UserPassPort> results) {
			userpass_array = results;
			mInflater = LayoutInflater.from(context);
		}

		public int getCount() {
			return userpass_array.size();
		}

		public Object getItem(int position) {
			return userpass_array.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			UserPassPort holder;
			final TextView port;
			ImageView icon;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.cracker_result_entry, null);
				holder = new UserPassPort();

				convertView.setTag(holder);
			}

			port = (TextView) convertView.findViewById(R.id.port);
			TextView username = (TextView) convertView.findViewById(R.id.username);
			TextView password = (TextView) convertView.findViewById(R.id.password);
			icon = (ImageView) convertView.findViewById(R.id.icon);
			TextView target = (TextView) convertView.findViewById(R.id.target);

			Protocol _protocol = userpass_array.get(position).protocol;
			// port.setText(_port + "/" + Port2Protocol(_port));
			port.setText(_protocol.name);
			username.setText(userpass_array.get(position).user);
			password.setText(userpass_array.get(position).pass);
			icon.setImageResource(Helpers.getIconForPort(_protocol.port));
			target.setText(crackerOption.Target);

			return convertView;
		}

	}

}
