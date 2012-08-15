package com.zimperium.zanti.traceroute;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ShellPool {

	final static int MAX_THREADS = 4;
	final static int MAX_TASK_QUEUE = 1000;
	private static ShellPool _instance;

	public static int NumTasksCompleted = 0;

	public final static String ACTION_SHELLPOOL_REFRESH = "zImperium.com.anti.shellpool.refresh";

	private List<ShellTask> tasks = new ArrayList<ShellPool.ShellTask>();

	ExecutorService shellthreadpool;

	public static boolean isShellTaskRunning() {
		if (_instance == null)
			return false;
		return !_instance.tasks.isEmpty();
	}

	public static List<ShellTask> getShellTaskList() {
		if (_instance != null) {
			return new ArrayList<ShellPool.ShellTask>(_instance.tasks);
		}
		return new ArrayList<ShellPool.ShellTask>();
	}

	public static synchronized void Destroy() {
		NumTasksCompleted = 0;
		if (_instance == null) {
			return;
		}
		_instance.shellthreadpool.shutdownNow();
		_instance = null;
	}

	public static synchronized ShellPool getInstance() {
		if (_instance == null) {
			_instance = new ShellPool();
		}
		return _instance;
	}

	public static ShellResult submitTask(ShellTask task) {
		try {
			Future<ShellResult> future = submitTaskAsync(task);
			return future.get();
		} catch (Exception ex) {
			ShellResult sr = new ShellResult();
			sr.finished = false;
			sr.success = false;
			sr.result = new ArrayList<String>();
			sr.result.add(ex.getLocalizedMessage());
			return new ShellResult();
		}
	}

	public static Future<ShellResult> submitTaskAsync(final ShellTask task) {
		final ShellPool instance = getInstance();
		instance.tasks.add(task);
		task.Status = ShellTask.IN_QUEUE;
		sendRefresh(task.appContext);
		Future<ShellResult> future = instance.shellthreadpool.submit(new Callable<ShellResult>() {

			@Override
			public ShellResult call() throws Exception {
				task.Status = ShellTask.RUNNING;
				sendRefresh(task.appContext);
				Log.i("ShellPool", "Executing '" + task.command + "'");
				Process process = null;
				StringBuilder buf = null;
				//if (task.returnresult) {
					buf = new StringBuilder();
				//}
				ShellResult result = new ShellResult();
				task.result = result;
				try {
					ProcessBuilder pb;
					if (task.useroot)
						pb = new ProcessBuilder("su");
					else
						pb = new ProcessBuilder("sh");
					
					pb.redirectErrorStream(true);
					process = pb.start();					
					
					DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
					DataInputStream inputStream = new DataInputStream(process.getInputStream());

					if (task.path != null) {
						outputStream.writeBytes("cd " + task.path + "\n");
						outputStream.flush();
					}

					outputStream.writeBytes(task.command + "\nexit\n");
					outputStream.flush();

					if (process.waitFor() == 0) {
						result.success = true;
					} else {
						result.success = false;
					}
					
					String str;

					if (buf != null) {
						result.result = new ArrayList<String>();
						while (((str = inputStream.readLine()) != null)) {
							Log.i("Shellpool", str);
							result.result.add(str);
						}
					}
					
					Log.i("Shellpool", "Task complete");

					result.finished = true;
					instance.tasks.remove(task);
					task.Status = ShellTask.COMPLETE;
					NumTasksCompleted++;
					sendRefresh(task.appContext);
					return result;
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
				} finally {
					if (process != null)
						process.destroy();
				}
				result.success = false;
				result.finished = false;
				instance.tasks.remove(task);
				task.Status = ShellTask.COMPLETE;
				NumTasksCompleted++;
				sendRefresh(task.appContext);
				return result;
			}
		});
		task.future = future;
		return future;
	}

	private ShellPool() {
		// shellthreadpool = new ThreadPoolExecutor(1, MAX_THREADS, 30,
		// TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		shellthreadpool = Executors.newFixedThreadPool(MAX_THREADS);
	}

	public static class ShellTask {

		public final static int NOT_YET_STARTED = 0;
		public final static int IN_QUEUE = 1;
		public final static int RUNNING = 2;
		public final static int COMPLETE = 3;

		String command, path;
		boolean returnresult = false;
		boolean useroot = false;

		public int Status = 0;
		public String StatusDetail = null;
		public Context appContext;

		public ShellResult result;
		public Future<ShellResult> future;

		public ShellTask(String path, String command, Context appContext) {
			this(path, command, false, true, appContext);
		}

		public ShellTask(String path, String command, boolean returnresult, boolean su, Context appContext) {
			this.command = command;
			this.path = path;
			this.returnresult = returnresult;
			this.appContext = appContext;
			this.useroot = su;
		}

		public void Cancel() {
			if (future == null)
				return;
			future.cancel(true);
		}
	}

	public static class ShellResult {

		public boolean success, finished;
		public List<String> result;

		public String resultAsString() {
			try {
				StringBuilder buf = new StringBuilder();
				for (String str : result) {
					buf.append(str + "\n");
				}
				return buf.toString();
			} catch (Exception ex) {
				return "";
			}
		}

	}

	public static void sendRefresh(Context appContext) {
		if (appContext == null)
			return;
		Intent i = new Intent(ACTION_SHELLPOOL_REFRESH);
		appContext.sendBroadcast(i);
	}
}
