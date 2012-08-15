package com.zimperium.zanti.cracker;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zimperium.zanti.cracker.XmlParser.Port;
import com.zimperium.zanti.plugins.AntiPlugin;

public class CrackerOptions extends Activity {

	static String DIR_LIBS, DIR_HYDRA, DIR_LOGS;

	final static int DIALOG_TYPES = 6;
	final static int DIALOG_NOT_ALLOWED = 7;

	ProtocolListAdapter adapter;
	// static String DIR_LOGS;
	String Target;

	TextView crack_type;
	EditText crack_incremental_options, crack_incremental_username;
	Button show_crack_types;
	CheckBox crack_all_protocols;
	LinearLayout show_protocols;
	RelativeLayout crack_type_holder;
	LinearLayout crack_incremental_holder;
	Button gobutton;

	final static String[] crack_types = new String[]{"Small Dictionary", "Optimized Dictionary", "Big Dictionary", "Huge Dictionary", "Custom Dictionary", "Incremental"};

	String target_network;
	int selected_crack_type = 0;
	int selected_crack_target = 0;
	String custom_pass_file;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DIR_LIBS = getIntent().getStringExtra(AntiPlugin.EXTRA_DIR_LIBS);
		DIR_HYDRA = getIntent().getStringExtra(AntiPlugin.EXTRA_DIR_HYDRA);
		DIR_LOGS = getIntent().getStringExtra(AntiPlugin.EXTRA_DIR_LOGS);

		Target = getIntent().getStringExtra(AntiPlugin.EXTRA_TARGET);
		target_network = getIntent().getStringExtra(AntiPlugin.EXTRA_NETWORK);

		setContentView(R.layout.cracker_options);
		((TextView) findViewById(R.id.title)).setText("Cracker @ " + Target);
		((TextView) findViewById(R.id.header)).setText("Options");

		crack_type = (TextView) findViewById(R.id.crack_type);
		crack_incremental_options = (EditText) findViewById(R.id.crack_incremental_options);
		crack_incremental_username = (EditText) findViewById(R.id.crack_incremental_username);
		show_crack_types = (Button) findViewById(R.id.show_crack_types);
		crack_all_protocols = (CheckBox) findViewById(R.id.crack_all_protocols);
		crack_incremental_holder = (LinearLayout) findViewById(R.id.crack_incremental_holder);
		crack_type_holder = (RelativeLayout) findViewById(R.id.crack_type_holder);
		show_protocols = (LinearLayout) findViewById(R.id.show_protocols);

		crack_type.setText(crack_types[selected_crack_type]);
		final Context ctx = this;

		gobutton = (Button) findViewById(R.id.gobutton);
		crack_all_protocols.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean isChecked = crack_all_protocols.isChecked();
				if (target_has_opened_ports) {
					if (isChecked) {
						ShowProtocolList(false);
					} else {
						ShowProtocolList(true);
					}
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
					builder.setMessage("No available protocols found on target - Switching to manual protocol selection.").setTitle("Automatic mode disabled.")
							.setPositiveButton("Ok", null).show();
					ShowProtocolList(true);
				}
			}
		});

		crack_type_holder.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog(DIALOG_TYPES);
			}
		});

		show_crack_types.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog(DIALOG_TYPES);
			}
		});

		ListView mainMenu = (ListView) findViewById(R.id.main_menu);
		mainMenu.setClipChildren(false);
		adapter = new ProtocolListAdapter();
		mainMenu.setAdapter(adapter);

		new Thread() {
			public void run() {
				try {
					File logsdir = new File(DIR_LOGS);
					logsdir = new File(logsdir, target_network);
					// final String filename = Target.trim().replace('/', '.');
					FilenameFilter filter = new FilenameFilter() {

						@Override
						public boolean accept(File dir, String filenameFilter) {
							// return filenameFilter.contains(filename) &&
							// filenameFilter.contains(".xml");
							return filenameFilter.contains(".xml");
						}
					};
					File[] filelist = logsdir.listFiles(filter);

					if (filelist.length == 0) {
						return;
					}

					for (File file : filelist) {
						try {
							XmlParser parser = new XmlParser(file.getAbsolutePath());
							final ArrayList<HashMap<String, Object>> result = parser.Parse();
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									SetupOpenedPorts(result);
								}
							});
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							ShowProtocolList(!target_has_opened_ports);
						}
					});
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			};
		}.start();

		gobutton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (gobutton.getVisibility() != View.VISIBLE)
					return;

				List<Protocol> protocols = new ArrayList<Protocol>();
				for (Protocol protocol : adapter.list) {
					if (protocol.flagged)
						protocols.add(protocol);
				}

				DefinedCrackerOption crackerOption = new DefinedCrackerOption();

				crackerOption.cracker_protocols = protocols;
				crackerOption.selected_crack_type = selected_crack_type;
				crackerOption.custom_pass_file = custom_pass_file;
				if (selected_crack_type == 5)
					crackerOption.crack_incremental_options = crack_incremental_options.getText().toString();

				crackerOption.Target = Target;
				crackerOption.TargetNetwork = target_network;

				Intent i = new Intent(ctx, Cracker.class);
				i.putExtra("DefinedCrackerOption", crackerOption);
				startActivity(i);
				// finish();
			}
		});
	}

	void ShowProtocolList(boolean show) {
		if (show) {
			crack_all_protocols.setChecked(false);
			show_protocols.setVisibility(View.VISIBLE);
			gobutton.setVisibility(View.GONE);
			Helpers.SlideOutLeft(gobutton, null);
			Helpers.SlideInRight(show_protocols, null);
		} else {
			crack_all_protocols.setChecked(true);
			show_protocols.setVisibility(View.GONE);
			gobutton.setVisibility(View.VISIBLE);
			Helpers.SlideInRight(gobutton, null);
			Helpers.SlideOutLeft(show_protocols, null);
		}

	}

	public String Port2Protocol(int port) {

		switch (port) {
			case 20 :
			case 21 :
				return "ftp";
			case 22 :
				return "ssh";
			case 23 :
				return "telnet";
			case 25 :
				return "smtp";
			case 80 :
			case 8080 :
				return "http-get /";
			case 110 :
				return "pop3";
			case 119 :
				return "nntp";
			case 143 :
			case 220 :
				return "imap";
			case 194 :
				return "irc";
			case 443 :
				return "https-get /";
			case 445 :
				return "smb";
			case 512 :
				return "rexec";
			case 514 :
				return "rsh";
			case 989 :
			case 990 :
				return "ftps";
			case 1433 :
			case 1434 :
				return "mssql";
			case 1521 :
			case 1526 :
			case 2483 :
				return "oracle-listener";
			case 2401 :
				return "cvs";
			case 3050 :
				return "firebird";
			case 3306 :
				return "mysql";
			case 3389 :
				return "rdp";
			case 3690 :
				return "svn";
			case 5190 :
				return "icq";
			case 5222 :
			case 5280 :
			case 5281 :
			case 8010 :
			case 5269 :
				return "xmpp";
			case 5432 :
				return "postgres";
			case 5500 :
			case 5800 :
				return "vnc";
			case 5631 :
				return "pcanywhere";
			case 8767 :
			case 8768 :
				return "teamspeak";
			default :
				return "http-get /";
		}
	}

	boolean target_has_opened_ports = false;

	public void SetupOpenedPorts(ArrayList<HashMap<String, Object>> result) {
		for (HashMap<String, Object> map : result) {
			if (map.get("ip").equals(Target)) {
				@SuppressWarnings("unchecked")
				List<Port> ports = (List<Port>) map.get("ports");
				for (Port port : ports) {
					target_has_opened_ports = true;
					Protocol p = new Protocol(Port2Protocol(port.portnumber), port.portnumber);
					int index = adapter.list.indexOf(p);
					if (index == -1) {
						// adapter.list.add(p);
					} else {
						p = adapter.list.get(index);
						p.flagged = true;
						adapter.list.set(index, p);
					}
				}
				adapter.Update();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 0)
			return;
		selected_crack_type = 4;
		custom_pass_file = data.getStringExtra("SelectedFile");
		// crack_type.setText(crack_types[selected_crack_type]);
		crack_type.setText(custom_pass_file);
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_NOT_ALLOWED) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("GOLD REQUIRED");
			builder.setMessage("Changing dictionary types requires a gold or platinum account.");
			builder.setIcon(R.drawable.icon);
			builder.setPositiveButton("OK", null);
			return builder.create();
		}

		if (id == DIALOG_TYPES) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select Cracking Method");
			builder.setItems(crack_types, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					crack_incremental_holder.setVisibility(View.GONE);
					selected_crack_type = 0;
					crack_type.setText(crack_types[0]);
					showDialog(DIALOG_NOT_ALLOWED);
				}
			});
			return builder.create();
		}

		return super.onCreateDialog(id);
	};

	class ProtocolListAdapter implements ListAdapter {

		final static int NUM_PER_ROW = 2;

		List<Protocol> list = new ArrayList<Protocol>();
		List<DataSetObserver> observers = new ArrayList<DataSetObserver>();
		Display display;
		public ProtocolListAdapter() {
			super();
			display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			for (String protocol : CrackerCore.crack_protocols) {
				Protocol p = new Protocol(protocol, -1);
				list.add(p);
			}
		}

		@Override
		public int getCount() {
			int s = list.size();
			if (s % NUM_PER_ROW == 0)
				return list.size() / NUM_PER_ROW;
			else
				return list.size() / NUM_PER_ROW + 1;
		}

		@Override
		public Object getItem(int arg0) {
			return list.get(arg0);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View imViews[] = null;

			LinearLayout holder = (LinearLayout) convertView;
			if (holder == null) {
				holder = new LinearLayout(CrackerOptions.this);
				holder.setClipChildren(false);
				holder.setOrientation(LinearLayout.HORIZONTAL);

				imViews = new View[NUM_PER_ROW];
				holder.setTag(R.id.imviews, imViews);

				LayoutInflater inflater = LayoutInflater.from(CrackerOptions.this);

				for (int i = 0; i < NUM_PER_ROW; i++) {
					View v = inflater.inflate(R.layout.protocol_item, null);

					v.setBackgroundResource(R.drawable.semi_transparent_background);
					v.setPadding(0, 0, 0, 0);
					int w = display.getWidth() / NUM_PER_ROW;
					LayoutParams params = new LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT);
					params.bottomMargin = params.leftMargin = params.topMargin = params.rightMargin = 0;
					params.rightMargin = 2;
					v.setLayoutParams(params);
					holder.addView(v);
					imViews[i] = v;
					v.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							Protocol p = (Protocol) v.getTag(R.id.mainmenuoption);

							DefinedCrackerOption crackerOption = new DefinedCrackerOption();

							crackerOption.cracker_protocols = new ArrayList<Protocol>();
							crackerOption.cracker_protocols.add(p);
							crackerOption.selected_crack_type = selected_crack_type;
							crackerOption.custom_pass_file = custom_pass_file;
							if (selected_crack_type == 5)
								crackerOption.crack_incremental_options = crack_incremental_options.getText().toString();

							crackerOption.Target = Target;
							crackerOption.TargetNetwork = target_network;

							Intent i = new Intent(CrackerOptions.this, Cracker.class);
							i.putExtra("DefinedCrackerOption", crackerOption);
							startActivity(i);
						}
					});
				}
			} else {
				imViews = (View[]) holder.getTag(R.id.imviews);
			}

			for (int i = 0; i < NUM_PER_ROW; i++) {
				View v = imViews[i];
				int pos = position * NUM_PER_ROW + i;
				if (pos < list.size()) {
					v.setTag(R.id.mainmenuoption, list.get(pos));

					TextView menu_item_title = (TextView) v.findViewById(R.id.title);
					RelativeLayout flag = (RelativeLayout) v.findViewById(R.id.flag);

					menu_item_title.setText("" + list.get(pos).name);
					if (list.get(pos).flagged)
						flag.setVisibility(View.VISIBLE);
					else
						flag.setVisibility(View.INVISIBLE);

					v.clearAnimation();
					v.setVisibility(View.VISIBLE);
					if (i == 0 || i == NUM_PER_ROW - 1) {
						AnimationSet as = new AnimationSet(false);

						if (i == 0) {
							TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF, 0f, Animation.ABSOLUTE, 0f,
									Animation.ABSOLUTE, 0f);
							translateAnimation.setDuration(250);
							as.addAnimation(translateAnimation);
						} else {
							TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f, Animation.ABSOLUTE, 0f,
									Animation.ABSOLUTE, 0f);
							translateAnimation.setDuration(250);
							as.addAnimation(translateAnimation);
						}

						AlphaAnimation animation = new AlphaAnimation(0f, 1f);
						animation.setDuration(350);
						animation.setInterpolator(new DecelerateInterpolator(0.25f));

						as.addAnimation(animation);

						v.startAnimation(as);
					} else {
						AlphaAnimation animation = new AlphaAnimation(0f, 1f);
						animation.setDuration(350);
						animation.setInterpolator(new DecelerateInterpolator(0.25f));
						v.startAnimation(animation);
					}
				} else {
					v.setVisibility(View.INVISIBLE);
				}

			}

			return holder;
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
			return list.isEmpty();
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}

		public void Update() {
			for (DataSetObserver obs : observers) {
				obs.onChanged();
			}
		}
	}

	public static class Protocol implements Serializable {

		private static final long serialVersionUID = 1L;

		public boolean flagged = false;
		final public String name;
		final public int port;

		// Use -1 for port for hydra to use default port for that protocol name
		public Protocol(String name, int port) {
			this.name = name;
			this.port = port;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Protocol) {
				return ((Protocol) o).name.equalsIgnoreCase(name);
			}
			return super.equals(o);
		}
	}

}
