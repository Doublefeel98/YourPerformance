package com.itc.thaithang.yourperformance.fragment;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.itc.thaithang.Constant;
import com.itc.thaithang.yourperformance.R;
import com.itc.thaithang.yourperformance.apdapter.ScheduleAdapter;
import com.itc.thaithang.yourperformance.customView.CustomScrollView;
import com.itc.thaithang.yourperformance.model.DataItem;
import com.itc.thaithang.yourperformance.model.Report;
import com.itc.thaithang.yourperformance.model.ScheduleItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.support.constraint.Constraints.TAG;


public class SetTimeFragment extends Fragment {

    private View view;

    //view
    private FloatingActionButton fabAdd;
    private CalendarView calendarView;
    private RecyclerView recyclerView;
    private TextView tvNoSchedule;
    private Dialog dialogNotif, dialogSetTime;
    private TextView tvDate;
    private Toolbar toolbar;
    private CardView cardView;
    private CustomScrollView scrollView;

    //view in dialog set timeMemory
    private Button btnCreate, btnCancelCreate;
    private LinearLayout linearStartTime;
    private RelativeLayout relativeDateDialog;
    private TextView tvDateDialog;
    private TextView tvTimeStart;
    private EditText edtNote;

    //varible
    private String idUser;
    private Report report;
    private ScheduleItem newScheduleItem;
    private List<ScheduleItem> listNewScheduleItem;
    private DataItem newDataItem;
    private List<ScheduleItem> listCreatedSchedule; //lưu danh sách công việc trên firebase
    private DataItem createdData;//lưu data item lấy từ firebase về
    private String dateMemory;//date ở bộ nhớ
    private String dateCalendar;//date dùng để hiện thị lên dialog settime
    private String timeMemory;//lưu lại thời gian ở time picker
    //private Calendar calendar;

    //Init firebase
    private FirebaseFirestore db;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_settime, container, false);

        initView();
        //
        initFireBase();
        //gán giá trị mặc đinh là ngày hiện tại
        setDate(0, 0, 0);
        //lấy dữ liệu từ firebase
        getData();
        //kiểm tra report
        checkReportExist();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        onDateChangeCalenderView();
        onClickFabAdd();
        onScrollChangeScrollView();
        super.onViewCreated(view, savedInstanceState);
    }

    private void initView() {
        calendarView = view.findViewById(R.id.calendar);
        fabAdd = view.findViewById(R.id.fabAdd);
        recyclerView = view.findViewById(R.id.recyclerView);
        toolbar = view.findViewById(R.id.toolBar);
        tvDate = view.findViewById(R.id.tvDate);
        cardView = view.findViewById(R.id.cardViewCalendar);
        scrollView = view.findViewById(R.id.scrollView);
        tvNoSchedule = view.findViewById(R.id.tvNoSchedule);
    }

    private void initFireBase() {
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null)
            idUser = firebaseUser.getUid();
    }

    private void setDate(int year, int month, int dayOfMonth) {
        final Calendar calendar = Calendar.getInstance();
        //date format để hiện thị cho TextView
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDateView = new SimpleDateFormat("d MMMM yyyy");
        //date format để lưu xuống firebase
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDateDB = new SimpleDateFormat("dd-MM-yyyy");

        if (year != 0) {
            calendar.set(year, month, dayOfMonth);
            dateCalendar = formatDateView.format(calendar.getTime());
            dateMemory = formatDateDB.format(calendar.getTime());
        }
        else{
            dateCalendar = formatDateView.format(calendar.getTime());
            dateMemory = formatDateDB.format(calendar.getTime());
        }
    }

    private void getData() {
        DocumentReference mDocRef = db.collection(idUser).document(dateMemory);

        mDocRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    recyclerView.setVisibility(View.INVISIBLE);
                    tvNoSchedule.setVisibility(View.VISIBLE);
                    tvNoSchedule.setText("ERROR CONNECT FIREBASE");
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvNoSchedule.setVisibility(View.INVISIBLE);

                    createdData = new DataItem();
                    createdData.setDate(dateMemory);
                    listCreatedSchedule = new ArrayList<>();
                    Log.d(TAG, "Current Data: " + documentSnapshot.getData());

                    for (Map.Entry<String, Object> entry : Objects.requireNonNull(documentSnapshot.getData()).entrySet()) {
                        ScheduleItem scheduleItem = new ScheduleItem();
                        scheduleItem.setTimeStart(entry.getKey());
                        Map<String, String> nestedData = (Map<String, String>) entry.getValue();

                        scheduleItem.setNote(nestedData.get(Constant.Schedule.NOTE_KEY));
                        scheduleItem.setStatus(nestedData.get(Constant.Schedule.STATUS_KEY));
                        scheduleItem.setAlarm(nestedData.get(Constant.Schedule.ALARM_KEY));
                        scheduleItem.setRequestCode(nestedData.get(Constant.Schedule.REQUEST_CODE_KEY));

                        listCreatedSchedule.add(scheduleItem);
                    }

                    if (listCreatedSchedule.isEmpty()) {
                        scrollView.setEnableScrolling(false);
                        recyclerView.setVisibility(View.INVISIBLE);
                        tvNoSchedule.setVisibility(View.VISIBLE);
                        tvNoSchedule.setText("No Schedule");
                        deleteDocumentEmpTy(idUser, dateMemory);
                    } else {
                        //sắp xếp lại theo thứ tự thời gian
                        Collections.sort(listCreatedSchedule);
                        createdData.setScheduleItems(listCreatedSchedule);
                        ScheduleAdapter scheduleAdapter = new ScheduleAdapter(createdData, getContext(), idUser);
                        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
                        recyclerView.setLayoutManager(layoutManager);
                        recyclerView.setItemAnimator(new DefaultItemAnimator());
                        recyclerView.setAdapter(scheduleAdapter);
                    }

                } else {
                    recyclerView.setVisibility(View.INVISIBLE);
                    tvNoSchedule.setVisibility(View.VISIBLE);
                    tvNoSchedule.setText("No Schedule");
                }
            }
        });
    }

    private void onDateChangeCalenderView() {
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                setDate(year, month, dayOfMonth);
                getData();
            }
        });
    }

    private void onClickFabAdd() {
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //init dialog
                dialogSetTime = new Dialog(Objects.requireNonNull(getContext()));
                dialogSetTime.setContentView(R.layout.dialog_set_time);

                //init view dialog
                linearStartTime = dialogSetTime.findViewById(R.id.linearStartTime);
                tvTimeStart = dialogSetTime.findViewById(R.id.tvTimeStart);
                btnCreate = dialogSetTime.findViewById(R.id.btnCreate);
                btnCancelCreate = dialogSetTime.findViewById(R.id.btnCancelCreate);
                edtNote = dialogSetTime.findViewById(R.id.edtNote);
                relativeDateDialog = dialogSetTime.findViewById(R.id.relativeDateDialog);
                tvDateDialog = dialogSetTime.findViewById(R.id.tvDateDialog);

                relativeDateDialog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDatePicker();
                    }
                });

                linearStartTime.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showTimePicker(tvTimeStart);
                    }
                });

                btnCreate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createNewScheduleItem();
                    }
                });

                btnCancelCreate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogSetTime.cancel();
                    }
                });

                Objects.requireNonNull(dialogSetTime.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                //dialog.setCanceledOnTouchOutside(false);
                dialogSetTime.show();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void onScrollChangeScrollView() {
        scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY > oldScrollY) {
                    fabAdd.setVisibility(View.INVISIBLE);
                    //cardViewCalendar.setVisibility(View.INVISIBLE);
                    toolbar.setVisibility(View.VISIBLE);
                    tvDate.setText(dateCalendar);
                } else if (scrollY < oldScrollY) {
                    fabAdd.setVisibility(View.VISIBLE);
                    cardView.setVisibility(View.VISIBLE);
                    toolbar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void deleteDocumentEmpTy(String idUser, String dateMemory) {
        db.collection(idUser).document(dateMemory)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting document", e);
                    }
                });
    }

    private void createNewScheduleItem() {
        newDataItem = new DataItem();
        listNewScheduleItem = new ArrayList<>();
        newScheduleItem = new ScheduleItem();
        try {
            //String requestCode = System.currentTimeMillis() % 1000000000 + "";
            String requestCode = createRequestCode(timeMemory, dateMemory);
            newScheduleItem.setTimeStart(timeMemory);
            newScheduleItem.setNote(edtNote.getText().toString());
            newScheduleItem.setAlarm(Constant.Schedule.ON_ALARM);
            newScheduleItem.setStatus(Constant.Schedule.NOT_READY_STATUS);
            newScheduleItem.setRequestCode(requestCode);//thêm request code để cập nhật report
            listNewScheduleItem.add(newScheduleItem);
            newDataItem.setScheduleItems(listNewScheduleItem);
            newDataItem.setDate(dateMemory);

            String error = checkLogic(newDataItem);
            if (error == null) {
                //update report
                updateNumberOfWork();
                insertData(newDataItem);
                dialogSetTime.cancel();
            } else {
                showDialogError(error);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private String createRequestCode(String timeMemory, String dateMemory) {
        String[] arrTime = timeMemory.split(":");
        String[] arrDate = dateMemory.split("-");

        int year = Integer.valueOf(arrDate[2]) % 100;

        Log.d(TAG, "arrTime size: " + arrTime.length);
        Log.d(TAG, "arrDate size: " + arrDate.length);

        return arrDate[1] + arrDate[0]  + year + arrTime[0] + arrTime[1];
    }

    private void showDialogError(String error) {
        dialogNotif = new Dialog(Objects.requireNonNull(getContext()));
        dialogNotif.setContentView(R.layout.dialog_error_time);
        TextView txtNotif = dialogNotif.findViewById(R.id.tvNotif);
        txtNotif.setText(error);
        //dialogNotif.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(dialogNotif.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogNotif.show();
        Button btnOk = dialogNotif.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogNotif.cancel();
            }
        });
    }

    private void insertData(final DataItem dataItem) {
        String date = dataItem.getDate();
        DocumentReference mDocRef = db.collection(idUser).document(date);
        mDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    updateDate(dataItem);
                    Log.d(TAG, "EXISTS: Yes");
                } else {
                    Log.d(TAG, "EXISTS: No");
                    setData(dataItem);
                }
            }
        });
    }

    private void updateDate(DataItem dataItem) {
        Map<String, Object> docData = new HashMap<>();
        Map<String, String> nestedData = new HashMap<>();

        nestedData.put(Constant.Schedule.NOTE_KEY, dataItem.getScheduleItems().get(0).getNote());
        nestedData.put(Constant.Schedule.STATUS_KEY, dataItem.getScheduleItems().get(0).getStatus());
        nestedData.put(Constant.Schedule.ALARM_KEY, dataItem.getScheduleItems().get(0).getAlarm());
        nestedData.put(Constant.Schedule.REQUEST_CODE_KEY, dataItem.getScheduleItems().get(0).getRequestCode());

        docData.put(dataItem.getScheduleItems().get(0).getTimeStart(), nestedData);

        db.collection(idUser).document(dataItem.getDate())
                .update(docData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                showDialogStatus(R.layout.dialog_success_status, R.id.tvStatusSuccess, R.string.status_success_written);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("AAA", "Error writing document", e);
                showDialogStatus(R.layout.dialog_error_status, R.id.tvStatusError, R.string.status_error_write);
            }
        });
    }

    private void setData(DataItem dataItem) {
        Map<String, Object> docData = new HashMap<>();
        Map<String, String> nestedData = new HashMap<>();

        nestedData.put(Constant.Schedule.NOTE_KEY, dataItem.getScheduleItems().get(0).getNote());
        nestedData.put(Constant.Schedule.STATUS_KEY, dataItem.getScheduleItems().get(0).getStatus());
        nestedData.put(Constant.Schedule.ALARM_KEY, dataItem.getScheduleItems().get(0).getAlarm());
        nestedData.put(Constant.Schedule.REQUEST_CODE_KEY, dataItem.getScheduleItems().get(0).getRequestCode());

        docData.put(dataItem.getScheduleItems().get(0).getTimeStart(), nestedData);

        db.collection(idUser).document(dataItem.getDate())
                .set(docData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        showDialogStatus(R.layout.dialog_success_status, R.id.tvStatusSuccess, R.string.status_success_written);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("AAA", "Error writing document", e);
                        showDialogStatus(R.layout.dialog_error_status, R.id.tvStatusError, R.string.status_error_write);
                    }
                });
    }

    private void showDialogStatus(int dialog, int textView, int status) {
        final Dialog dialogStatus = new Dialog(Objects.requireNonNull(getContext()));
        dialogStatus.setContentView(dialog);
        dialogStatus.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(dialogStatus.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView txtStatus = dialogStatus.findViewById(textView);
        //dialogStatus.setCanceledOnTouchOutside(false);
        txtStatus.setText(status);
        dialogStatus.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogStatus.cancel();
            }
        }, 1369);
    }

    private String checkLogic(DataItem dataItem) throws ParseException {
        String error = null;
        String startTime = dataItem.getScheduleItems().get(0).getTimeStart();
        String date = dataItem.getDate();
        String note = dataItem.getScheduleItems().get(0).getNote();

        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        Calendar calendar = Calendar.getInstance();
        Date currentDate = dateFormat.parse(dateFormat.format(calendar.getTime()));
        Date currentTime = timeFormat.parse(timeFormat.format(calendar.getTime()));

        if (note.isEmpty()) {
            error = "Note is empty";
        } else if (startTime == null) {
            error = "Start time is empty";
        } else if (dateFormat.parse(date).before(currentDate)) {
            error = "Date must not be earlier current date";
        } else if (dateFormat.parse(date).equals(currentDate)) {
            if (currentTime.after(timeFormat.parse(startTime)) || currentTime.equals(timeFormat.parse(startTime))) {
                error = "Start time must be later than current time";
            }
        }

        return error;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH);
        final int date = calendar.get(Calendar.DATE);
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar calendarDate = Calendar.getInstance();
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDate = new SimpleDateFormat("d MMMM yyyy");
                @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                if (year != 0) {
                    calendarDate.set(year, month, dayOfMonth);
                }
                newDataItem.setDate(format.format(calendarDate.getTime()));
                tvDateDialog.setText(formatDate.format(calendarDate.getTime()));
                Toast.makeText(getContext(), formatDate.format(calendarDate.getTime()), Toast.LENGTH_SHORT).show();
            }
        }, year, month, date);
        datePickerDialog.show();
    }

    private void showTimePicker(final TextView textView) {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);

        TimePickerDialog pickerDialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar calendarTime = Calendar.getInstance();
                calendarTime.set(0, 0, 0, hourOfDay, minute);
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm");
                timeMemory = formatTime.format(calendarTime.getTime());
                textView.setText(timeMemory);
            }
        }, hour, min, true);

        pickerDialog.show();
    }

    //update report
    private void updateNumberOfWork() {
        int request = Integer.valueOf(report.getNumberOfWork());
        request++;
        String requestID = String.valueOf(request);
        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put(Constant.Schedule.numberOfWork, requestID);

        db.collection(idUser).document(Constant.Schedule.REPORT_KEY).update(nestedData);
    }

    private void checkReportExist() {
        final DocumentReference docRef = db.collection(idUser).document(Constant.Schedule.REPORT_KEY);

        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    report = new Report();
                    Log.d("check_report", snapshot.getData().get(Constant.Schedule.numberOfWork).toString());
                    report.setNumberOfWork(snapshot.getData().get(Constant.Schedule.numberOfWork).toString());
                } else {
                    Log.d("check_report", "NULL");
                    Map<String, String> nestedData = new HashMap<>();
                    nestedData.put(Constant.Schedule.numberOfWork, "0");
                    db.collection(idUser).document(Constant.Schedule.REPORT_KEY).set(nestedData);
                }

            }
        });
    }
}
