package com.zimperium.zanti.cracker;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;

public class Helpers {

	public static void ExecutePort(Context ctx, String host, int port, String username, String password) {
		Intent intent = null;
		switch (port) {
			case 22 :
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("ssh://" + username + "@" + host + ":" + port + "/#" + username + "@" + host + ":" + port));
				break;
			case 23 :
				if (username != null && password != null)
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("telnet://" + username + ":" + password + "/#" + host + ":" + port));
				else
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("telnet://" + host + ":" + port + "/#" + host + ":" + port));
				break;
			case 80 :
				if (username != null && password != null)
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + username + ":" + password + "@" + host + ":" + port + "/"));
				else
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + host + ":" + port + "/"));
				break;
			case 443 :
				if (username != null && password != null)
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + username + ":" + password + "@" + host + ":" + port + "/"));
				else
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + host + ":" + port + "/"));
				break;
			default :
				if (username != null && password != null)
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("telnet://" + username + ":" + password + "/#" + host + ":" + port));
				else
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("telnet://" + host + ":" + port + "/#" + host + ":" + port));
				break;
		}
		// if (intent == null) {
		// return;
		// }
		if (Helpers.isCallable(ctx, intent)) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ctx.startActivity(intent);
		} else {
			Intent goToMarket = null;
			switch (port) {
				case 22 :
					goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.connectbot"));
					break;
				case 23 :
					goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.connectbot"));
					break;
				default :
					goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.connectbot"));
					break;
			}
			// if (goToMarket == null)
			// return;
			goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ctx.startActivity(goToMarket);
		}
	}
	
	public static boolean isCallable(Context context, Intent intent) {
		List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public static int getIconForPort(int port) {
		switch (port) {
			case 445 :
			case 3389 :
				return R.drawable.windowsxp;
			case 21 :
			case 22 :
			case 23 :
			case 911 :
				return R.drawable.bash;
			case 25 :
			case 109 :
			case 110 :
			case 143 :
			case 220 :
			case 465 :
			case 587 :
			case 993 :
			case 995 :
			case 1109 :
			case 2525 :
			case 3535 :
			case 5108 :
			case 5109 :
				return R.drawable.mail1;
			case 80 :
			case 8080 :
				return R.drawable.globes;
			default :
				return R.drawable.port;
		}
	}
	
	public static void SlideInRight(View v, final Runnable onComplete) {
		v.clearAnimation();
		TranslateAnimation aout = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
				Animation.RELATIVE_TO_SELF, 0f);
		aout.setDuration(300);
		v.startAnimation(aout);
		if (onComplete != null)
			aout.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation arg0) {
				}

				@Override
				public void onAnimationRepeat(Animation arg0) {
				}

				@Override
				public void onAnimationEnd(Animation arg0) {
					onComplete.run();
				}
			});
	}

	public static void SlideInLeft(View v, final Runnable onComplete) {
		v.clearAnimation();
		TranslateAnimation aout = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF, 0f,
				Animation.RELATIVE_TO_SELF, 0f);
		aout.setDuration(300);
		v.startAnimation(aout);
		if (onComplete != null)
			aout.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation arg0) {
				}

				@Override
				public void onAnimationRepeat(Animation arg0) {
				}

				@Override
				public void onAnimationEnd(Animation arg0) {
					onComplete.run();
				}
			});
	}

	public static void SlideOutLeft(View v, final Runnable onComplete) {
		v.clearAnimation();
		TranslateAnimation aout = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF, 0f,
				Animation.RELATIVE_TO_SELF, 0f);
		aout.setDuration(300);
		aout.setFillAfter(true);
		v.startAnimation(aout);
		if (onComplete != null)
			aout.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation arg0) {
				}

				@Override
				public void onAnimationRepeat(Animation arg0) {
				}

				@Override
				public void onAnimationEnd(Animation arg0) {
					onComplete.run();
				}
			});
	}

	public static void SlideUp(View up, float distance) {
		TranslateAnimation aout = new TranslateAnimation(0, 0, distance, 0);
		aout.setDuration(300);
		up.startAnimation(aout);
	}

	public static void SlideDown(View down, float distance) {
		TranslateAnimation aout = new TranslateAnimation(0, 0, -distance, 0);
		aout.setDuration(300);
		down.startAnimation(aout);
	}
}
