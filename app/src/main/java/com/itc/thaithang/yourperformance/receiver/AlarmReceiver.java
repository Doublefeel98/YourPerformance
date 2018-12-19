package com.itc.thaithang.yourperformance.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.itc.thaithang.Constant;
import com.itc.thaithang.yourperformance.activity.AlarmActivity;
import com.itc.thaithang.yourperformance.notification.NotificationSchedule;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ALARM_KEY RECEIVER", "onReceive: ");

        //getIntent
        String idUser = intent.getStringExtra(Constant.Schedule.ID_USER_KEY);
        String date = intent.getStringExtra(Constant.Schedule.DATE_KEY);
        String time = intent.getStringExtra(Constant.Schedule.TIME_KEY);
        String alarm = intent.getStringExtra(Constant.Schedule.ALARM_KEY);
        String status = intent.getStringExtra(Constant.Schedule.STATUS_KEY);
        String note = intent.getStringExtra(Constant.Schedule.NOTE_KEY);
        String requestCode = intent.getStringExtra(Constant.Schedule.REQUEST_CODE_KEY);
        String title = intent.getStringExtra(Constant.Schedule.TITLE_KEY);

        NotificationSchedule.updateAlarm(idUser, date, time, note, requestCode);
        NotificationSchedule.showNotification(context, AlarmActivity.class, idUser, date, time,
                alarm, status, requestCode, title, note);
    }
}
