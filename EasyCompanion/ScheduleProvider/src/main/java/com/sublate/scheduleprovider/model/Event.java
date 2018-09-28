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
package com.sublate.scheduleprovider.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sublate.scheduleprovider.Receiver;
import com.sublate.scheduleprovider.Manager;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;

/*
 * This holds an event.
 * An event is one of the outcomes of a schedules. For now, it represents either the beginning
 * or the stop of a schedule.
 */
public class Event {
    public static final String ACTION = "com.sublate.schedulelistener.EventAction";
    private long m_id;
    private long m_scheduleID;
    private Calendar m_alarmTime;
    private Calendar m_lastAlarmTime;
    private String m_state;
  //  private Context m_Context;
    private String m_command;
    private AlarmManager m_alarmManager;
    private long  m_interval;
    private long m_ScheduledTime = 0;
    private static int m_CountEventId = 0;



    public Event(Calendar alarmTime, long Interval, String command) {
        m_alarmTime = alarmTime;
        m_id = ++m_CountEventId;
        m_alarmTime = alarmTime;
        m_command = command;
        m_interval = Interval;

   //     m_ScheduledTime = alarmTime.getTimeInMillis() + Interval;
    }


    public Event(long scheduleID, Calendar alarmTime, String command) {
        m_scheduleID = scheduleID;
        m_alarmTime = alarmTime;
        m_command = command;
        m_id = ++m_CountEventId;
        m_interval = 0;
    }

    public long getId() {
        return m_id;
    }

    public void setLastAlarmTime(Calendar m_lastAlarmTime) {
        this.m_lastAlarmTime = m_lastAlarmTime;
    }

    public String getEventType() {
        return m_command;
    }

    public Calendar getLastAlarmTime() {
        return m_lastAlarmTime;
    }

    public void setId(long id) {
        m_id = id;
    }

    public long getScheduleID() {
        return m_scheduleID;
    }

    public void setScheduleID(long scheduleID) {
        m_scheduleID = scheduleID;
    }

    public Calendar getAlarmTime() {
        return m_alarmTime;
    }

    public void setAlarmTime(Calendar alarmTime) {
        m_alarmTime = alarmTime;
    }

    public String getState() {
        return m_state;
    }

    public void setState(String state) {
        m_state = state;
    }
    
    public void startAlarm(Context m_Context) {

        Intent intent = new Intent(Manager.ACTION_ALARM_TRIGGER);
        intent.putExtra("command", m_command);
        intent.putExtra("EventId", getId());

       /* PendingIntent pi = PendingIntent.getBroadcast(m_Context, 1234, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //m_alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, m_alarmTime.getTimeInMillis(), pi);
         //m_ScheduledTime = SystemClock.elapsedRealtime() + m_interval;*/


        PendingIntent pi = this.getDistinctPendingIntent(m_Context, intent, (int) getId());

        m_alarmManager =  (AlarmManager) m_Context.getSystemService(ALARM_SERVICE);

        stopAlarm(m_Context);


        m_ScheduledTime = m_alarmTime.getTimeInMillis();
        m_alarmManager.set(AlarmManager.RTC_WAKEUP, m_ScheduledTime, pi);


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
        Log.i("startAlarm","set Alarm Event to " + sdf.format(m_alarmTime.getTime()));
 //       Log.d("startAlarm","Other Way " + sdf.format(SystemClock.elapsedRealtime() + m_interval));


    }

    protected PendingIntent getDistinctPendingIntent(Context m_Context, Intent intent, int requestId)
    {
        PendingIntent pi =
                PendingIntent.getBroadcast(
                        m_Context, 	//context
                        requestId, 	//request id
                        intent, 		//intent to be delivered
                        0);

        //pending intent flags
        //PendingIntent.FLAG_ONE_SHOT);
        return pi;
    }

    public void stopAlarm(Context m_Context) {

        if (m_CountEventId -1 != 0) {
            Intent i = new Intent(m_Context, Receiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(m_Context, m_CountEventId - 1, i, 0);
            m_alarmManager.cancel(pi);
        }
    }

    public long getScheduledTime()
    {
         return m_ScheduledTime;
    }

    public static class EVENT_ACTION {
        public static final int EVENT_STOP = 0;
        public static final int EVENT_TICK = 1;
        public static final int EVENT_PAUSE = 2;
        public static final int EVENT_STATUS = 3;
        public static final int EVENT_UNPAUSE = 4;
        public static final int EVENT_ID = 5;
        public static final int EVENT_STARTFOREGROUND = 6;
        public static final int EVENT_CHECK_SCHEDULE = 7;
        public static final int EVENT_START = 8;
        public static final int EVENT_STOP_LISTENERS = 9;
        public static final int EVENT_START_SCHEDULE = 10;
        public static final int EVENT_STOP_SCHEDULE = 11;
    }

    public static class EventType
    {
        public static final int ONCE = 0;
        public static final int REPEAT = 1;

        private long Interval = 0;
        private String Name;
    }

    public static class EventFTPType
    {
        public static final int AFTER_LAST_SCHEDULE = 0;
        public static final int AFTER_EACH_SCHEDULE = 1;
        public static final int REPEAT_INTERVAL = 2;
    }

}
