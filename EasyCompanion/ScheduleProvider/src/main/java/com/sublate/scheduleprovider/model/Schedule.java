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


import com.sublate.scheduleprovider.ScheduleState;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class is an implementation of a schedule
 */
public class Schedule implements ScheduleState {
    private long m_id;
    private String m_startTime;
	private String m_endTime;
    private long m_duration; // In seconds
    private int m_repeatType;
    private String m_tag;
    private String m_state;
	private boolean m_disabled;
	private Long m_groupId;
	private ScheduleGroup m_group;
	private List<Event> m_EventList;
	private static int  m_CountScheduleId=0;
	private long m_lastTimeEvent = 0;

    public Schedule(Calendar startTime, int duration, int repeatType, String tag) {
        //m_startTime = startTime.getTimeInMillis();
        m_duration = duration;
        m_repeatType = repeatType;
        m_tag = tag;
        m_id = ++m_CountScheduleId;
	    m_disabled = false;
	    m_groupId = null;
	    m_group = null;
		m_EventList = new ArrayList<Event>();
    }

	public static void resetScheduleId ()
	{
		m_CountScheduleId = 0;
	}
	public Schedule(Calendar startTime, Calendar endTime, String tag, long ScheduleId) {
	//	m_endTime  = endTime.getTimeInMillis();
	//	m_startTime = startTime.getTimeInMillis();
		m_duration = (int) (endTime.getTimeInMillis() - startTime.getTimeInMillis()); //calculateDuration(startTime, endTime);
	//	m_repeatType = Manager.REPEAT_TYPE_DAILY;
		m_tag = tag;
		m_id = (ScheduleId==0 ? ++m_CountScheduleId:ScheduleId);
		m_disabled = false;
		m_groupId = null;
		m_group = null;
		m_EventList = new ArrayList<Event>();
	}

	public Schedule(String startTime, String EndTime, String tag, long ScheduleId)
	{
		m_startTime = startTime;
		m_endTime = EndTime;
		m_tag = tag;
		//m_duration = completeFromMilliseconds(completeFromMilliseconds(EndTime)-completeFromMilliseconds(startTime));
		m_id = (ScheduleId==0 ? ++m_CountScheduleId:ScheduleId);
	}

	public static int calculateDuration(Calendar fromDay, Calendar toDay) {
		long from = fromDay.getTimeInMillis();
		long to = toDay.getTimeInMillis();
		return (int) TimeUnit.MILLISECONDS.toHours(to - from);
	}

	// Implementing the ScheduleState interface
	@Override
	public long getScheduleId() {
		return m_id;
	}

	@Override
	public String getStartTime() {
		return m_startTime;
	}

	@Override
	public long getDuration() {
		return m_duration;
	}

	@Override
	public int getRepeatType() {
		return m_repeatType;
	}

	@Override
	public String getTag() {
		return m_tag;
	}

	@Override
	public String getState() {
		return m_state;
	}

	@Override
	public boolean isDisabled() {
		return m_disabled;
	}

	@Override
	public String getGroupTag() {
		if (getGroup() != null) {
			return getGroup().getTag();
		}
		return null;
	}

	@Override
	public boolean isGroupEnabled() {
		if (getGroup() != null) {
			return getGroup().isEnabled();
		}

		// If a group is not found, this is not part of a group so return true
		return true;
	}

	@Override
	public String getGroupState() {
		if (getGroup() != null) {
			return getGroup().getOverallState();
		}
		return null;
	}

	// Other getters and setters
	public long getId() {
        return m_id;
    }

	public Long getGroupId() {
		return m_groupId;
	}

    public void setId(int id) {
        m_id = id;
    }

     public void setStartTime(String startTime) {
        m_startTime = startTime;
    }

	public String getEndTime() {
		return m_endTime;
	}

	public void setDuration(long duration) {
        m_duration = duration;
    }

    public void setRepeatType(int repeatType) {
        m_repeatType = repeatType;
    }

    public void setTag(String tag) {
        m_tag = tag;
    }

    public void setState(String state) {
	    m_state = state;
    }

	public void setDisabled(boolean disabled) {
		m_disabled = disabled;
	}

	public void setGroupId(Long groupId) {
		m_groupId = groupId;
	}

	private ScheduleGroup getGroup() {
		// For this call, the precondition is that the Manager class has been initialized.
		// That being the case, the SAMSQLiteHelper singleton has also been created and it hold
		// a valid context object. Passing in null to get an instance will be fine here.
		if (m_group == null && m_groupId != null && m_groupId > 0) {
			//m_group = SAMSQLiteHelper.getInstance(null).getScheduleGroupById(m_groupId);
		}
		return m_group;
	}

	public boolean addEvent(Event mEvent)
	{
		mEvent.setScheduleID(getScheduleId());
	//	m_EventList.add(mEvent);
		return true;
	}

	public long completeFromMilliseconds(String HourMin)
	{
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd "); //SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //dd/MM/yyyy
		Date now = new Date();
		String strDate = sdfDate.format(now);

		strDate += HourMin +":00";

		return getDateToMillisecsFromString(strDate);

	}

	public String getCompleteStartTime()
	{
		long millisecs = completeFromMilliseconds(m_startTime);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(millisecs);
	}

	public String getCompleteEndTime()
	{
		long millisecs = completeFromMilliseconds(m_endTime);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(millisecs);
	}

	public long getDateToMillisecsFromString(String strDate)
	{
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

	public Event getNextScheduleEvent(long mInterval)
	{
		long nowMiliSecsDate = new Date().getTime();
		if ((getDateToMillisecsFromString(getCompleteStartTime()) <= nowMiliSecsDate) && (nowMiliSecsDate <= getDateToMillisecsFromString(getCompleteEndTime())))
		{

			if (m_lastTimeEvent != 0)
				nowMiliSecsDate = m_lastTimeEvent;


			String mEventType = "";
			if (nowMiliSecsDate + mInterval > getDateToMillisecsFromString(getCompleteEndTime())) {
				mEventType = "END";
				nowMiliSecsDate = getDateToMillisecsFromString(getCompleteEndTime());
			}
			else
			{
				nowMiliSecsDate += mInterval;
				mEventType = "NORMAL";
			}

			Calendar startTime = Calendar.getInstance();
			startTime.setTimeInMillis(nowMiliSecsDate);

			m_lastTimeEvent = nowMiliSecsDate;

			Event event = new Event(getId(), startTime, mEventType);

			return event;
		}

		return null;
	}


}
