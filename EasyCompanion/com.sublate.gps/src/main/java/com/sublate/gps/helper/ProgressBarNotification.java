package com.sublate.gps.helper;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.sublate.gps.R;

public  class ProgressBarNotification {

		private Context mContext;
		private String mTitle;

		private NotificationManager mNotifyManager;
		private NotificationCompat.Builder mBuilder;
		private int mId = 1;

		public ProgressBarNotification(Context context, String title, int requestId){
			mContext = context;
			mTitle = title;
			mId=requestId;

			mNotifyManager = (NotificationManager)  mContext.getSystemService(Context.NOTIFICATION_SERVICE);
			mBuilder = new NotificationCompat.Builder(mContext);
			mBuilder.setContentTitle(mTitle)
					.setContentText(title+" in progress")
					.setSmallIcon(R.drawable.ic_launcher);

		}

		public void PreExecute () {

			// Displays the progress bar for the first time.
			mBuilder.setProgress(100, 0, false);
			mNotifyManager.notify(mId, mBuilder.build());
		}

		public void ProgressUpdate(int value)
		{
			// Update progress
			mBuilder.setProgress(100, value, false);
			mNotifyManager.notify(mId, mBuilder.build());

		}

		public void PostExecute(boolean result) {
			if (result)
				mBuilder.setContentText(mTitle + " complete");
			else
				mBuilder.setContentText(mTitle + " failure");

			// Removes the progress bar
			mBuilder.setProgress(0, 0, false);
			mNotifyManager.notify(mId, mBuilder.build());

			try {
				// Sleep for 5 seconds
				Thread.sleep(3*1000);
			} catch (InterruptedException e) {
				Log.d("TAG", "sleep failure");
			}

			mNotifyManager.cancel(mId);
		}
	}
