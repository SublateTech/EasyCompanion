/* The MIT License (MIT)
 *
 * Copyright (c) 2014 Scalior, Inc
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author:      Eyong Nsoesie (eyongn@scalior.com)
 * Date:        10/05/2014
 */
package com.sublate.scheduleprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Broadcast receiver to process alarms.
 *
 */
public class Receiver extends BroadcastReceiver {

    String TAG="Receiver";
    public Receiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Manager.getbroadcastReceiver()) {
            long EventId = intent.getLongExtra("EventId", 0);
            String mCommand = intent.getStringExtra("command");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
            Log.i("TAG", "currTime " + sdf.format(Calendar.getInstance().getTime().getTime()));
            Log.i(TAG, "EventId " + EventId);


            Manager.getInstance(context).firedEvents(EventId, mCommand);
            // The updateScheduleStates method takes care of scheduling the next event
            // as well as notifying the application of the event that occurred.
            //   AlarmProcessingUtil.getInstance(context).updateScheduleStates(null);
        }
    }
}
