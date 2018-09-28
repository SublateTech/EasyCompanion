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

import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import com.sublate.scheduleprovider.model.Event;
import com.sublate.scheduleprovider.model.Schedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Manager {

    public static final int REPEAT_TYPE_HOURLY      = 1;
    public static final int REPEAT_TYPE_DAILY       = 2;
    public static final int REPEAT_TYPE_WEEKLY      = 3;
    public static final int REPEAT_TYPE_MONTHLY     = 4;
    public static final int REPEAT_TYPE_YEARLY      = 5;
    public static final int REPEAT_TYPE_NONE        = 6;
	public static final int REPEAT_TYPE_MINUTELY    = 7;

    public static final String STATE_ON             = "ON";
    public static final String STATE_OFF            = "OFF";

	public static final String ACTION_ALARM_TRIGGER     = "com.sublate.schedulelistener.ALARM_TRIGGER";


    private static Manager m_instance;

	private Callback m_samCallback;

    private Context m_context;
    private boolean m_initialized;
    private String m_versionName;
	private boolean m_invokeCallback;
	private int m_suspendCallbackCount;
	private Receiver m_ServiceReceiver;
	private List<Schedule> m_ScheduleList;
	private long m_LastEventScheduled = 0;
	private long m_currentInterval = 0;
	static private Boolean m_broadcastReceiver = true;




    /**
     * Description:
     * 		Get the singleton instance of the Schedule Alarm Manager
     * 	    If it has already been constructed, the passed in parameters have no effect
     * @param p_context: The application context
     * @return The singleton instance
     */
    public static Manager getInstance(Context p_context) {
        if (m_instance == null ) {
            m_instance = new Manager(p_context);
        }
        return m_instance;
    }


    private Manager(Context p_context) {
        m_context = p_context;
        m_initialized = false;
		m_samCallback = null;
		m_invokeCallback = true;
    }

    /**
     * Description:
     * 		Initialize the Schedule Alarm Manager
     * @return boolean - true if successful, false other wise
     */
    public boolean init() {
		m_context.registerReceiver(m_ServiceReceiver, new IntentFilter(
				Manager.ACTION_ALARM_TRIGGER));

		m_ScheduleList = new ArrayList<Schedule>();
		m_initialized = true;
		return true;
    }

    /**
     * Helper method which takes an input time and moves it forward to the very
     * next time that an event occurs given the current time and the repeat type
     */
    private void adjustToNextAlarmTime(Calendar timeToAdjust, int repeatType) {
        Calendar currTime = Calendar.getInstance();
		while (timeToAdjust.getTimeInMillis() < currTime.getTimeInMillis()) {
            switch (repeatType) {
				case Manager.REPEAT_TYPE_MINUTELY:
					timeToAdjust.add(Calendar.MINUTE, 10);
					break;
                case Manager.REPEAT_TYPE_HOURLY:
                    timeToAdjust.add(Calendar.HOUR, 1);
                    break;
                case Manager.REPEAT_TYPE_DAILY:
                    timeToAdjust.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case Manager.REPEAT_TYPE_WEEKLY:
                    timeToAdjust.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case Manager.REPEAT_TYPE_MONTHLY:
                    timeToAdjust.add(Calendar.MONTH, 1);
                    break;
                case Manager.REPEAT_TYPE_YEARLY:
                    timeToAdjust.add(Calendar.YEAR, 1);
                    break;
            }
        }

    }

	/**
	 * Description:
	 *  This method sets the callback.
	 *
	 *  @param samCallback - The callback instance. Once set can't be changed
	 *  @param replace - If true, a current callback will be replaced with this one
	 *                   If false and a callback is already set, the new callback will
	 *                   be ignored.
	 */
	public void setCallback(Callback samCallback, boolean replace) {
		if (replace || m_samCallback == null) {
			m_samCallback = samCallback;
		} else {
			throw new UnsupportedOperationException("The callback has already been set");
		}
	}

	/**
	 * Description:
	 *  Retrieves the callback instance.
	 *  Note: A callback can only be set once.
	 */
	public Callback getCallback() {
		return m_samCallback;
	}

	/**
	 * Description:
	 * 		Suspend callbacks. This is useful when adding multiple schedules
	 */
	public void suspendCallbacks() {
		if (m_suspendCallbackCount <= 0) {
			m_invokeCallback = false;
			m_suspendCallbackCount = 1;
		} else {
			m_suspendCallbackCount++;
		}
	}

	/**
	 * Description:
	 * 		Resume callbacks. Undo the suspension of callbacks
	 */
	public void resumeCallbacks() {
		if (m_suspendCallbackCount > 0) {
			m_suspendCallbackCount--;
		}
		if (m_suspendCallbackCount == 0) {
			m_invokeCallback = true;
		}
	}

	public void firedEvents(long exeEvent, String command)
	{
		m_samCallback.onScheduleEventFired(exeEvent,command);
	}


	public Event setNextScheduledEvent()
	{
		return getNextScheduledEvent(m_currentInterval);

	}

	public Event getNextFtpScheduledEvent(int eventFTPType, long mInterval)
	{

		Event event = null;
		switch (eventFTPType)
		{
			case Event.EventFTPType.AFTER_EACH_SCHEDULE:
				mInterval = 0;
				long mCurScheduleId = getTodayCurrentSchedule();
				Schedule mSchedule = getScheduleById(mCurScheduleId);
				String EndTime = mSchedule.getCompleteEndTime();
				Calendar StartDate = DateUtils.convertStringToCalendar(mSchedule.getCompleteEndTime(), "yyyy-MM-dd HH:mm:ss");
				event = new Event(mCurScheduleId,StartDate,"FTP");
				break;
			case Event.EventFTPType.AFTER_LAST_SCHEDULE:
				Schedule m_Schedule = getTodayLastSchedule();
				Calendar date = Calendar.getInstance();

				if (m_Schedule.getDateToMillisecsFromString(m_Schedule.getCompleteEndTime()) <  (new Date()).getTime() )
					return null;
				Calendar startDate = DateUtils.convertStringToCalendar(m_Schedule.getCompleteEndTime(), "yyyy-MM-dd HH:mm:ss");
				event = new Event(m_Schedule.getId(),startDate,"FTP");
				break;
			case Event.EventFTPType.REPEAT_INTERVAL:
				break;
		}

		m_currentInterval = mInterval;

		m_LastEventScheduled = event.getAlarmTime().getTimeInMillis();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
		Log.i("SAM Manager", "getNextFtpScheduledTime: " + sdf.format(m_LastEventScheduled));
		return event;
	}

	public Event getNextScheduledEvent(long mInterval)
	{
		m_currentInterval = mInterval;
		Event event = getTodayNextEvent(mInterval);
		if (event == null)
		{
			//get the next day schedule
			event = getNextDaySchedule();

		}
	//	event.startAlarm(m_context);
		m_LastEventScheduled = event.getAlarmTime().getTimeInMillis();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");

		//Log.i("SAM Manager", "getNextScheduledTime: " + sdf.format(m_LastEventScheduled));
		return event;
	}

	private Schedule getScheduleById(long mScheduleId)
	{
		if (m_ScheduleList ==null)
			return null;

		for (Schedule entry : m_ScheduleList)
		{
			if (entry.getId() == mScheduleId)
				return entry;
		}
		return null;
	}


	/*
	protected void onResume() {
		super.onResume();
		registerReceiver(mReceiver, mIntentFilter);
	}

	protected void onPause() {
		if(mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		super.onPause();
	}
	*/
	public boolean addSchedule(Schedule mSchedule)
	{
		m_ScheduleList.add(mSchedule);
		return true;
	}

	private long getTodayCurrentSchedule()
	{
		if (m_ScheduleList ==null)
			return 0;

		for (Schedule entry : m_ScheduleList)
		{

			long startMiliSecsDate = milliseconds(entry.getStartTime());
			long endMiliSecsDate = milliseconds(entry.getEndTime());
			long nowMiliSecsDate = new Date().getTime();

			if (startMiliSecsDate <= nowMiliSecsDate && nowMiliSecsDate <=endMiliSecsDate )
				return entry.getId();

		}
		return 0;
	}

	private Event getTodayNextEvent(long mInterval)
	{
		if (m_ScheduleList ==null)
			return null;

		for (Schedule entry : m_ScheduleList)
		{
			Event event = entry.getNextScheduleEvent(mInterval);
			if (event!=null) {
				event.setScheduleID(entry.getId());
				return event;
			}
		}

		return getNextTodaySchedule();
	}

	private Event getNextTodaySchedule()
	{

		if (m_ScheduleList ==null)
			return null;


		long mScheduleId=0;
		long mMinSchedule = 0;
		Calendar date = Calendar.getInstance();

		for (Schedule entry : m_ScheduleList)
		{
			if (mMinSchedule==0)
				mMinSchedule = date.getTimeInMillis();

			if (entry.completeFromMilliseconds(entry.getStartTime()) > mMinSchedule) {
				mMinSchedule = entry.completeFromMilliseconds(entry.getStartTime());
				mScheduleId = entry.getId();
				break;
			}
		}
		if (mScheduleId==0)
			return null;

		date.setTimeInMillis(mMinSchedule);

		Event event = new Event(date,0,"START");
		event.setScheduleID(mScheduleId);

		return event;
	}

	private Event getNextDaySchedule()
	{
		if (m_ScheduleList ==null)
			return null;

		long mScheduleId=0;
		long mMinSchedule = 0;
		for (Schedule entry : m_ScheduleList)
		{
			if (mMinSchedule==0)
				mMinSchedule = entry.completeFromMilliseconds("24:00");

			if (entry.completeFromMilliseconds(entry.getStartTime()) < mMinSchedule) {
				mMinSchedule = entry.completeFromMilliseconds(entry.getStartTime());
				mScheduleId = entry.getId();
			}
		}

		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(mMinSchedule);
		DateUtils.addDays(date,1);
		Event event = new Event(date,0,"START");
		event.setScheduleID(mScheduleId);

		return event;
	}

	private Schedule getTodayLastSchedule()
	{
		if (m_ScheduleList ==null)
			return null;

		Schedule mSchedule = null;
		long mMaxSchedule = 0;
		for (Schedule entry : m_ScheduleList)
		{
			if (entry.completeFromMilliseconds(entry.getEndTime()) >= mMaxSchedule) {
				mMaxSchedule = entry.completeFromMilliseconds(entry.getEndTime());
				mSchedule = entry;
			}
		}
		return mSchedule;
	}

	public long milliseconds(String HourMin)
	{
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd "); //SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //dd/MM/yyyy
		Date now = new Date();
		String strDate = sdfDate.format(now);

		strDate += HourMin +":00";

		//String date_ = date;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try
		{
			Date mDate = sdf.parse(strDate);
			long timeInMilliseconds = mDate.getTime();
			return timeInMilliseconds;
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	public void setBroadcastReceiver(Boolean broadcastReceiver)
	{
		m_broadcastReceiver = broadcastReceiver;

	}

	static public Boolean getbroadcastReceiver() {
		return m_broadcastReceiver;
	}

}
