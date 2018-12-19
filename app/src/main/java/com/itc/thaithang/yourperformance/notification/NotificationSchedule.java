package com.itc.thaithang.yourperformance.notification;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.itc.thaithang.Constant;
import com.itc.thaithang.yourperformance.model.DataItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.ALARM_SERVICE;
import static android.support.constraint.Constraints.TAG;

public class NotificationSchedule {

    public static void setAlarm(Context context, Class<?> cls, DataItem dataItem, int position, String idUser) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm");
        Date date;
        Date time;
        try {
            //cancel already schedule reminders
            cancelAlarm(context, cls, Integer.valueOf(dataItem.getScheduleItems().get(position).getRequestCode()));

            //enable a receiver
            ComponentName receiver = new ComponentName(context, cls);
            PackageManager pm = context.getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            //init time reminder
            String getTime = dataItem.getScheduleItems().get(position).getTimeStart();
            String getDate = dataItem.getDate();
            date = formatDate.parse(getDate);
            time = formatTime.parse(getTime);

            int hour = time.getHours();
            int min = time.getMinutes();
            int day = date.getDate();
            @SuppressLint("SimpleDateFormat") String formatYear = new SimpleDateFormat("yyyy").format(date);
            int month = date.getMonth();
            int year = Integer.parseInt(formatYear);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, min);
            calendar.set(Calendar.SECOND, 0);

            //cộng thêm 1 để giống với tháng hiện tại
            int monthCurrent = month + 1;

            Intent intent = new Intent(context, cls);

            //truyền dữ liệu cho intent
            intent.putExtra(Constant.Schedule.TITLE_KEY, day + "/" + monthCurrent + "/" + year + "   " + hour + ":" + min);
            intent.putExtra(Constant.Schedule.ID_USER_KEY, idUser);
            intent.putExtra(Constant.Schedule.DATE_KEY, dataItem.getDate());
            intent.putExtra(Constant.Schedule.TIME_KEY, dataItem.getScheduleItems().get(position).getTimeStart());
            intent.putExtra(Constant.Schedule.ALARM_KEY, dataItem.getScheduleItems().get(position).getAlarm());
            intent.putExtra(Constant.Schedule.STATUS_KEY, dataItem.getScheduleItems().get(position).getStatus());
            intent.putExtra(Constant.Schedule.NOTE_KEY, dataItem.getScheduleItems().get(position).getNote());
            intent.putExtra(Constant.Schedule.REQUEST_CODE_KEY, dataItem.getScheduleItems().get(position).getRequestCode());

            //tạo thông báo alarm receiver
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    Integer.valueOf(dataItem.getScheduleItems().get(position).getRequestCode()),
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            Log.d("ALARM", "SET_ALARM");
            Log.d("ALARM", "DATE: " + day + "/" + monthCurrent + "/" + year + "  " + hour + ":" + min);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void cancelAlarm(Context context, Class<?> cls, int requestCode) {
        Log.d(TAG, "cancelAlarm: ");

        //disable a receiver
        ComponentName receiver = new ComponentName(context, cls);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        //tắt thông báo Alarm receiver
        Intent intent = new Intent(context, cls);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    public static void updateAlarm(String idUser, String date, String time, String note, String requestCode) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final Map<String, Object> docData = new HashMap<>();
        Map<String, String> nestedData = new HashMap<>();
        nestedData.put(Constant.Schedule.NOTE_KEY, note);
        nestedData.put(Constant.Schedule.STATUS_KEY, Constant.Schedule.MISSED_STATUS);
        nestedData.put(Constant.Schedule.ALARM_KEY, Constant.Schedule.OFF_ALARM);
        nestedData.put(Constant.Schedule.REQUEST_CODE_KEY, requestCode);

        docData.put(time, nestedData);
        db.collection(idUser).document(date)
                .update(docData);
    }

    public static void showNotification(Context context, Class<?> cls,
                                        String idUser, String date, String time, String alarm,
                                        String status, String requestCode, String title, String note) {
        //lấy đường dẫn nhạc chuông thông báo
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        Intent intent = new Intent(context, cls);
        intent.putExtra(Constant.Schedule.ID_USER_KEY, idUser);
        intent.putExtra(Constant.Schedule.DATE_KEY, date);
        intent.putExtra(Constant.Schedule.TIME_KEY, time);
        intent.putExtra(Constant.Schedule.ALARM_KEY, alarm);
        intent.putExtra(Constant.Schedule.STATUS_KEY, status);
        intent.putExtra(Constant.Schedule.REQUEST_CODE_KEY, requestCode);
        intent.putExtra(Constant.Schedule.NOTE_KEY, note);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        //tạo task mới trong back task để mở một activity tùy ý khi nhấn vào notification
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(cls);
        stackBuilder.addNextIntent(intent);

        int requestID = Integer.valueOf(requestCode);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(requestID, PendingIntent.FLAG_UPDATE_CURRENT);

        //tạo một notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = null;
        String id = "id_chanel";

        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("ALARM", "1");
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(id);
            if (notificationChannel == null) {
                notificationChannel = new NotificationChannel(id, title, importance);
                notificationChannel.enableVibration(true);
                notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                notificationManager.createNotificationChannel(notificationChannel);
            }
            builder = new NotificationCompat.Builder(context, id);
            builder.setContentTitle(title)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentText(note)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setSound(alarmSound)
                    .setContentIntent(pendingIntent)
                    .setTicker(title)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        } else {

            Log.d("ALARM", "2");
            builder = new NotificationCompat.Builder(context, id);
            builder.setContentTitle(title)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentText(note)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(title)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                    .setPriority(Notification.PRIORITY_HIGH);
        }

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_INSISTENT | Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(requestID, notification);
    }
}
