package com.itc.thaithang.yourperformance.model;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScheduleItem implements Comparable<ScheduleItem> {

    private String timeStart;
    private String note;
    private String status;
    private String alarm;

    public ScheduleItem() {
    }

    private String requestCode;

    public ScheduleItem(String timeStart, String note, String status, String alarm, String requestCode) {
        this.timeStart = timeStart;
        this.note = note;
        this.status = status;
        this.alarm = alarm;
        this.requestCode = requestCode;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getAlarm() {
        return alarm;
    }

    public void setAlarm(String alarm) {
        this.alarm = alarm;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int compareTo(@NonNull ScheduleItem o) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm");
        Date date1 = new Date();
        Date date2 = new Date();
        try {
            date1 = formatTime.parse(getTimeStart());
            date2 = formatTime.parse(o.getTimeStart());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date1.compareTo(date2);
    }
}
