package com.itc.thaithang.yourperformance.fragment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;


import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.itc.thaithang.Constant;
import com.itc.thaithang.yourperformance.R;
import com.itc.thaithang.yourperformance.model.DataItem;
import com.itc.thaithang.yourperformance.model.ScheduleItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.support.constraint.Constraints.TAG;

public class ReportFragment extends Fragment {

    private View view;
    private BarChart barChart;
    private PieChart pieChart;
    private TextView tvDateReport;
    private ImageView imgCalendar;

    //Init firebase
    private FirebaseFirestore db;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;

    private String idUser;
    private List<ScheduleItem> listScheduleBarChart;
    private List<ScheduleItem> listSchedulePieChart;
    private String dateMemory;
    private List<BarEntry> entries;
    private List<String> labels;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_report, container, false);

        initView();

        initFireBase();

        setDate(0, 0, 0);
        getData();

        return view;
    }

    private void initView() {
        barChart = view.findViewById(R.id.barChart);
        pieChart = view.findViewById(R.id.pieChart);
        tvDateReport = view.findViewById(R.id.tvDateReport);
        imgCalendar = view.findViewById(R.id.imgCalendar);
    }

    private void getData() {
        DocumentReference mDocRef = db.collection(idUser).document(dateMemory);
        mDocRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    pieChart.setData(null);
                    pieChart.invalidate();
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {

                    String status;

                    listSchedulePieChart = new ArrayList<>();
                    Log.d(TAG, "Current Data: " + documentSnapshot.getData());
                    for (Map.Entry<String, Object> entry : Objects.requireNonNull(documentSnapshot.getData()).entrySet()) {
                        ScheduleItem scheduleItem = new ScheduleItem();
                        scheduleItem.setTimeStart(entry.getKey());
                        Map<String, String> nestedData = (Map<String, String>) entry.getValue();

                        status = nestedData.get(Constant.Schedule.STATUS_KEY);

                        if (!Constant.Schedule.NOT_READY_STATUS.equals(status)) {

                            scheduleItem.setNote(nestedData.get(Constant.Schedule.NOTE_KEY));
                            scheduleItem.setStatus(status);
                            scheduleItem.setAlarm(nestedData.get(Constant.Schedule.ALARM_KEY));
                            scheduleItem.setRequestCode(nestedData.get(Constant.Schedule.REQUEST_CODE_KEY));

                            listSchedulePieChart.add(scheduleItem);
                        }
                    }
                    Collections.sort(listSchedulePieChart);
                    createPieChart();

                } else {
                    pieChart.setData(null);
                    pieChart.invalidate();
                }
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        imgCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateDialog();
            }
        });
        createBarChart();
        super.onViewCreated(view, savedInstanceState);
    }

    private void showDateDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                setDate(year, month, dayOfMonth);
                getData();
            }
        }, year, month, date);

        datePickerDialog.show();
    }

    private void setDate(int year, int month, int dayOfMonth) {
        final Calendar calendar = Calendar.getInstance();
        //date format để hiện thị cho TextView
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDateView = new SimpleDateFormat("d MMMM yyyy");
        //date format để lưu xuống firebase
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDateDB = new SimpleDateFormat("dd-MM-yyyy");

        if (year != 0) {
            calendar.set(year, month, dayOfMonth);
            dateMemory = formatDateDB.format(calendar.getTime());
            tvDateReport.setText(formatDateView.format(calendar.getTime()));
        } else {
            dateMemory = formatDateDB.format(calendar.getTime());
            tvDateReport.setText(formatDateView.format(calendar.getTime()));
        }
    }

    private void createChart() {
        createBarChart();
        createPieChart();
    }

    private void createPieChart() {

        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        //set animation
        pieChart.animateY(1000, Easing.EasingOption.EaseInOutCubic);

        if (listSchedulePieChart.isEmpty()) {
            pieChart.setData(null);
            pieChart.invalidate();
        } else {
            List<PieEntry> entries = createDataPieChart();

            PieDataSet set = new PieDataSet(entries, "Time");
            set.setSliceSpace(3f);
            set.setSelectionShift(5f);
            set.setColors(ColorTemplate.JOYFUL_COLORS);

            Description description = new Description();
            description.setText("This is the statistical chart of the workload of a day");
            description.setTextSize(15f);
            pieChart.setDescription(description);

            PieData data = new PieData(set);
            data.setValueTextColor(R.color.text_pie_chart);
            data.setValueTextSize(25f);
            pieChart.setData(data);
            pieChart.invalidate();
        }
    }

    private List<PieEntry> createDataPieChart() {
        List<PieEntry> entries = new ArrayList<>();

        float workingTime = 0f, breakTime = 24f, totalWorkingTime = 0f;

        int seconds = 0;
        String status;
        for (ScheduleItem item : listSchedulePieChart) {
            status = item.getStatus();
            if (!Constant.Schedule.NOT_READY_STATUS.equals(status)) {
                if (Constant.Schedule.MISSED_STATUS.equals(status)) {
                    seconds = 0;
                } else {
                    seconds = parseSeconds(status);
                }
                totalWorkingTime += seconds;
                float percent = (seconds * 1.0f) / 86400;
                workingTime = percent * 24.0f;
                entries.add(new PieEntry(workingTime, item.getNote()));
            }
        }

        if (totalWorkingTime != 0) {
            float percent = (totalWorkingTime * 1.0f) / 86400;
            breakTime = 24.0f - 24.0f * percent;
        }
        entries.add(new PieEntry(breakTime, "Break"));

        return entries;
    }

    private int parseSeconds(String status) {
        int seconds = 0;

        String[] arr = status.split(":");

        seconds += Integer.valueOf(arr[2]);
        seconds += Integer.valueOf(arr[1]) * 60;
        seconds += Integer.valueOf(arr[0]) * 3600;

        return seconds;
    }

    private void createBarChart() {
        Calendar calendar = Calendar.getInstance();

        entries = new ArrayList<>();

        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDateDB = new SimpleDateFormat("dd-MM-yyyy");


        String date;

        for (int i = 7; i > 0; i--) {
            calendar.add(Calendar.DATE, -1);
            date = formatDateDB.format(calendar.getTime());
            getDataBarChart(date, i);
        }

        labels = new ArrayList<>();
        labels.add("0");

        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDateView = new SimpleDateFormat("dd/MM");
        Calendar calendarView = Calendar.getInstance();
        calendarView.add(Calendar.DATE,-1);
        String seventhDay = formatDateView.format(calendarView.getTime());
        calendarView.add(Calendar.DATE,-1);
        String sixthDay = formatDateView.format(calendarView.getTime());
        calendarView.add(Calendar.DATE,-1);
        String fifthDay = formatDateView.format(calendarView.getTime());
        calendarView.add(Calendar.DATE,-1);
        String fourthDay = formatDateView.format(calendarView.getTime());
        calendarView.add(Calendar.DATE,-1);
        String thirdDay = formatDateView.format(calendarView.getTime());
        calendarView.add(Calendar.DATE,-1);
        String secondDay = formatDateView.format(calendarView.getTime());
        calendarView.add(Calendar.DATE,-1);
        String firstDay = formatDateView.format(calendarView.getTime());

        labels.add(firstDay);
        labels.add(secondDay);
        labels.add(thirdDay);
        labels.add(fourthDay);
        labels.add(fifthDay);
        labels.add(sixthDay);
        labels.add(seventhDay);

    }

    private void getDataBarChart(String date, final int position) {

        Log.d(TAG, "getDataBarChart: " + date + "-" + position);

        final DocumentReference mDocRef = db.collection(idUser).document(date);
        final String[] status = new String[1];
        final float[] workingTime = {0f};


        mDocRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                int seconds = 0;

                if (e != null) {
                    addDataBarChart(position, workingTime[0]);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {

                    listScheduleBarChart = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : Objects.requireNonNull(documentSnapshot.getData()).entrySet()) {
                        ScheduleItem scheduleItem = new ScheduleItem();
                        scheduleItem.setTimeStart(entry.getKey());
                        Map<String, String> nestedData = (Map<String, String>) entry.getValue();

                        status[0] = nestedData.get(Constant.Schedule.STATUS_KEY);

                        if (!Constant.Schedule.NOT_READY_STATUS.equals(status[0])) {

                            scheduleItem.setNote(nestedData.get(Constant.Schedule.NOTE_KEY));
                            scheduleItem.setStatus(status[0]);
                            scheduleItem.setAlarm(nestedData.get(Constant.Schedule.ALARM_KEY));
                            scheduleItem.setRequestCode(nestedData.get(Constant.Schedule.REQUEST_CODE_KEY));

                            listScheduleBarChart.add(scheduleItem);
                        }
                    }

                    Log.d(TAG, "listScheduleBarChart: " + listScheduleBarChart.size());

                    if (!listScheduleBarChart.isEmpty()) {
                        for (ScheduleItem item : listScheduleBarChart) {
                            if (!Constant.Schedule.MISSED_STATUS.equals(item.getStatus())) {
                                seconds += parseSeconds(item.getStatus());
                            }
                        }
                        float percent = (seconds * 1.0f) / 86400;
                        workingTime[0] = percent * 24.0f;
                        addDataBarChart(position, workingTime[0]);
                    }
                } else
                    addDataBarChart(position, workingTime[0]);
            }
        });
    }

    private void addDataBarChart(int position, float workingTime) {
        //Log.d(TAG, "addDataBarChart: " + position +"-"+workingTime);
        entries.add(new BarEntry(position, workingTime));

        if (entries.size() >= 7) {
            BarDataSet dataset = new BarDataSet(entries, "Hours");
            //dataset.setColors(ColorTemplate.COLORFUL_COLORS);
            //
            Log.d(TAG, "Data bar chart: " + entries);
            BarData data = new BarData(dataset);
            data.setBarWidth(0.9f); // set custom bar width

            XAxis xAxis = barChart.getXAxis();
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            barChart.getDescription().setEnabled(false);
            barChart.setData(data);
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            barChart.animateY(5000);
            barChart.setFitBars(true); // make the x-axis fit exactly all bars
            barChart.invalidate(); // refresh
        }
    }

    private void initFireBase() {
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null)
            idUser = firebaseUser.getUid();
    }
}
