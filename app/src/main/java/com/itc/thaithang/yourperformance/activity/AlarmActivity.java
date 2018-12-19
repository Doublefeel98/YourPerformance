package com.itc.thaithang.yourperformance.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.firebase.firestore.FirebaseFirestore;
import com.itc.thaithang.Constant;
import com.itc.thaithang.yourperformance.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AlarmActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvDate, tvTime, tvNote, tvCountTime;
    private LinearLayout linearGoMenu;
    private ImageView imgEnd;
    private ToggleButton tgStartStop;
    private String idUser, date, time, alarm, status, note, requestCode;
    private long countTime = 0;
    private Runnable timeRunnable;
    private Handler timeHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        GetIntent();

        initView();

        countTimeRunnable();
    }

    private void GetIntent() {
        idUser = getIntent().getStringExtra(Constant.Schedule.ID_USER_KEY);
        date = getIntent().getStringExtra(Constant.Schedule.DATE_KEY);
        time = getIntent().getStringExtra(Constant.Schedule.TIME_KEY);
        alarm = getIntent().getStringExtra(Constant.Schedule.ALARM_KEY);
        status = getIntent().getStringExtra(Constant.Schedule.STATUS_KEY);
        note = getIntent().getStringExtra(Constant.Schedule.NOTE_KEY);
        requestCode = getIntent().getStringExtra(Constant.Schedule.REQUEST_CODE_KEY);

        Log.d("GetIntent", "requestCode: " + requestCode);
    }

    private void initView() {
        tvCountTime = findViewById(R.id.tvCountTime);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvNote = findViewById(R.id.tvNote);
        linearGoMenu = findViewById(R.id.linearGoMenu);
        imgEnd = findViewById(R.id.imgEnd);
        tgStartStop = findViewById(R.id.tgStartStop);

        //
        tvTime.setText(time);
        tvNote.setText(note);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDateCalendar = new SimpleDateFormat("d MMMM yyyy");
        try {
            Date calendarFormatDate = formatDate.parse(date);
            String dateCalendar = formatDateCalendar.format(calendarFormatDate);
            tvDate.setText(dateCalendar);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //
        linearGoMenu.setOnClickListener(this);
        imgEnd.setOnClickListener(this);
        tgStartStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tvCountTime.setVisibility(View.VISIBLE);
                if (isChecked)
                    startOrResume();
                else
                    stop();
            }
        });

    }

    private void countTimeRunnable(){

        timeHandler = new Handler();
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                countTime ++;
                long hours = countTime/3600;
                long minutes = (countTime%3600)/60;
                long seconds = countTime%216000;

                tvCountTime.setText(String.format("%02d:%02d:%02d",hours,minutes,seconds));
                timeHandler.postDelayed(this,1000);
            }
        };

    }

    private void stop() {
        timeHandler.removeCallbacks(timeRunnable);
    }

    private void startOrResume() {
        timeHandler.postDelayed(timeRunnable,0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.linearGoMenu:
                goMainScreen();
                break;
            case R.id.imgEnd:
                showDialogExit();
                break;
        }
    }

    private void showDialogExit() {
        //init dialog
        final Dialog dialogExit = new Dialog(AlarmActivity.this);
        dialogExit.setContentView(R.layout.dialog_exit);
        dialogExit.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(dialogExit.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogExit.setCanceledOnTouchOutside(false);
        dialogExit.show();

        Button btnExit = dialogExit.findViewById(R.id.btnExit);
        Button btnGoBack = dialogExit.findViewById(R.id.btnGoBack);

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogExit.cancel();
                finishAndRemoveTask();
            }
        });
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogExit.cancel();
            }
        });
    }

    private void goMainScreen() {
        Intent intentGoMenu = new Intent(AlarmActivity.this, MainActivity.class);
        startActivity(intentGoMenu);
    }

    private void updateCountTime()
    {
        String statusUpdated = (String) tvCountTime.getText();
        if(statusUpdated.equals("00:00:00"))
        {
            statusUpdated = Constant.Schedule.MISSED_STATUS;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final Map<String, Object> docData = new HashMap<>();
        Map<String, String> nestedData = new HashMap<>();
        nestedData.put(Constant.Schedule.NOTE_KEY, note);
        nestedData.put(Constant.Schedule.STATUS_KEY, statusUpdated);
        nestedData.put(Constant.Schedule.ALARM_KEY, Constant.Schedule.OFF_ALARM);
        nestedData.put(Constant.Schedule.REQUEST_CODE_KEY, requestCode);

        docData.put(time, nestedData);

        db.collection(idUser).document(date).update(docData);
        Log.d("ALARM","Update_Count_Time");
    }

    @Override
    protected void onPause() {
        Log.d("ALARM","onPause");
        updateCountTime();
        super.onPause();
        //stop runnable if want power saved, count time don't work
        //timerHandler.removeCallbacks(timerRunnable);
        //imgStartStop.setImageResource(R.drawable.ic_start);
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ALARM","onResume");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ALARM","onStop");
    }

    @Override
    protected void onDestroy() {
        updateCountTime();
        Log.d("ALARM","onDestroy");
        super.onDestroy();
    }
}
