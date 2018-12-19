package com.itc.thaithang.yourperformance.apdapter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.support.v7.widget.PopupMenu;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.itc.thaithang.Constant;
import com.itc.thaithang.yourperformance.R;
import com.itc.thaithang.yourperformance.model.DataItem;
import com.itc.thaithang.yourperformance.model.Report;
import com.itc.thaithang.yourperformance.model.ScheduleItem;
import com.itc.thaithang.yourperformance.notification.NotificationSchedule;
import com.itc.thaithang.yourperformance.receiver.AlarmReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    //firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    //view
    private DataItem dataItem;
    private List<ScheduleItem> scheduleItems;
    private Context context;
    private String idUser;
    private Report report;

    //dialog
    private Dialog dialogUpdate;
    private Dialog dialogDelete;

    public ScheduleAdapter(DataItem dataItem, Context context, String idUser) {
        this.dataItem = dataItem;
        this.context = context;
        this.idUser = idUser;

        scheduleItems = dataItem.getScheduleItems();
        checkReportExist();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = layoutInflater.inflate(R.layout.schedule_item_card, viewGroup, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        final ScheduleItem scheduleItem = scheduleItems.get(i);
        final String status = scheduleItem.getStatus();
        viewHolder.tvStartTime.setText(scheduleItem.getTimeStart());
        viewHolder.tvNote.setText(scheduleItem.getNote());

        if (Constant.Schedule.OFF_ALARM.equals(scheduleItem.getAlarm())) {
            viewHolder.cardViewSchedule.setCardBackgroundColor(Color.parseColor("#e8e8e8"));
            //viewHolder.tvStartTime.setTextColor(Color.parseColor("#e8e8e8"));
            //viewHolder.tvNote.setTextColor(Color.parseColor("#e8e8e8"));

        }
        if (Constant.Schedule.ON_ALARM.equals(scheduleItem.getAlarm()) && Constant.Schedule.NOT_READY_STATUS.equals(scheduleItem.getStatus())) {
            viewHolder.tvStartTime.setTextColor(Color.parseColor("#757575"));
            viewHolder.tvNote.setTextColor(Color.parseColor("#757575"));
        }

        //set on off alarm reciver
        initNotificationAlarm(scheduleItem, i);

        //set event item
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Constant.Schedule.NOT_READY_STATUS.equals(status)) {
                    showDialogUpdate(scheduleItem, viewHolder.getAdapterPosition());
                }
            }
        });

        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showPopupMenu(v, status, scheduleItem, viewHolder.getAdapterPosition());
                return true;
            }
        });

        viewHolder.imgOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v, status, scheduleItem, viewHolder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataItem.getScheduleItems().size();
    }

    @SuppressLint("RestrictedApi")
    private void showPopupMenu(View v, String status, final ScheduleItem scheduleItem, final int i) {
        if (Constant.Schedule.NOT_READY_STATUS.equals(status)) {
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.itemUpdate) {
                        //show dialog update
                        showDialogUpdate(scheduleItem, i);
                    } else if (item.getItemId() == R.id.itemDelete) {
                        //show dialog delete
                        showDialogDelete(scheduleItem, i);
                    }
                    return true;
                }
            });
            @SuppressLint("RestrictedApi")
            MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popupMenu.getMenu(), v);
            menuHelper.setForceShowIcon(true);
            menuHelper.setGravity(Gravity.END);
            menuHelper.show();
        } else {
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu_status_missed, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.itemDeleteNote) {
                        //show dialog delete
                        showDialogDelete(scheduleItem, i);
                    }
                    return true;
                }
            });
            @SuppressLint("RestrictedApi")
            MenuPopupHelper menuHelper = new MenuPopupHelper(context, (MenuBuilder) popupMenu.getMenu(), v);
            menuHelper.setForceShowIcon(true);
            menuHelper.setGravity(Gravity.END);
            menuHelper.show();
        }
    }

    private void ShowTimePicker(final TextView textView) {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar calendarTime = Calendar.getInstance();
                calendarTime.set(0, 0, 0, hourOfDay, minute);
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm");
                String time = formatTime.format(calendarTime.getTime());
                textView.setText(time);
            }
        }, hour, min, true);
        timePickerDialog.show();
    }

    private void showDialogUpdate(final ScheduleItem scheduleItem, final int i) {
        //init dialog
        dialogUpdate = new Dialog(context);
        dialogUpdate.setContentView(R.layout.dialog_update);
        Objects.requireNonNull(dialogUpdate.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogUpdate.setCanceledOnTouchOutside(false);

        //init view dialog
        Button btnUpdate = dialogUpdate.findViewById(R.id.btnUpdate);
        Button btnCancel = dialogUpdate.findViewById(R.id.btnCancelUpdate);
        LinearLayout linearStartTime = dialogUpdate.findViewById(R.id.linearStartTimeUpdate);
        final TextView tvStartTime = dialogUpdate.findViewById(R.id.tvStartTimeUpdate);
        final EditText edtNoteUpdate = dialogUpdate.findViewById(R.id.edtNoteUpdate);


        tvStartTime.setText(scheduleItem.getTimeStart());
        edtNoteUpdate.setText(scheduleItem.getNote());

        dialogUpdate.show();
        //Event
        linearStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowTimePicker(tvStartTime);
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //xử lý update
                try {
                    String error = checkLogic(dataItem, tvStartTime.getText().toString(), edtNoteUpdate.getText().toString());

                    if (error == null) {
                        //kiểm tra có thay đổi thời gian ko?
                        //nếu đổi thì xóa lịch biểu cũ đi
                        if (scheduleItem.getTimeStart().equals(tvStartTime.getText().toString())) {
                            updateData(dataItem, tvStartTime.getText().toString(), edtNoteUpdate.getText().toString(), i);
                            dialogUpdate.cancel();
                        } else {
                            updateData(dataItem, tvStartTime.getText().toString(), edtNoteUpdate.getText().toString(), i);
                            deleteData(dataItem, i, false);
                            dialogUpdate.cancel();
                        }
                    } else {
                        showDialogError(error);
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogUpdate.cancel();
            }
        });
    }

    private void showDialogDelete(ScheduleItem scheduleItem, final int i) {
        //init dialog
        dialogDelete = new Dialog(context);
        dialogDelete.setContentView(R.layout.dialog_delete);
        Objects.requireNonNull(dialogDelete.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogDelete.setCanceledOnTouchOutside(false);
        dialogDelete.show();

        //set event dialog
        Button btnCancel = dialogDelete.findViewById(R.id.btnCancelDel);
        Button btnDelete = dialogDelete.findViewById(R.id.btnDel);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogDelete.cancel();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteData(dataItem, i, true);
                dialogDelete.cancel();
            }
        });
    }

    private void showDialogError(String error) {
        final Dialog dialogNotif = new Dialog(Objects.requireNonNull(context));
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

    private void showDialogStatus(int dialog, int textView, int status) {
        final Dialog dialogStatus = new Dialog(context);
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

    public void updateData(DataItem dataItem, String startTime, String note, int i) {
        final Map<String, Object> docData = new HashMap<>();
        Map<String, String> nestedData = new HashMap<>();
        nestedData.put(Constant.Schedule.NOTE_KEY, note);
        nestedData.put(Constant.Schedule.STATUS_KEY, Constant.Schedule.NOT_READY_STATUS);
        nestedData.put(Constant.Schedule.ALARM_KEY, dataItem.getScheduleItems().get(i).getAlarm());
        nestedData.put(Constant.Schedule.REQUEST_CODE_KEY, dataItem.getScheduleItems().get(i).getRequestCode());

        docData.put(startTime, nestedData);

        db.collection(idUser).document(dataItem.getDate())
                .update(docData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        showDialogStatus(R.layout.dialog_success_status, R.id.tvStatusSuccess, R.string.status_success_update);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showDialogStatus(R.layout.dialog_error_status, R.id.tvStatusError, R.string.status_error_update);
                    }
                });
    }

    private void deleteData(DataItem dataItem, final int i, final boolean show) {

        if (Constant.Schedule.ON_ALARM.equals(dataItem.getScheduleItems().get(i).getAlarm())) {
            NotificationSchedule.cancelAlarm(context, AlarmReceiver.class, Integer.parseInt(dataItem.getScheduleItems().get(i).getRequestCode()));
        }

        DocumentReference docRef = db.collection(idUser).document(dataItem.getDate());
        Map<String, Object> updates = new HashMap<>();
        updates.put(dataItem.getScheduleItems().get(i).getTimeStart(), FieldValue.delete());

        docRef.update(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (show) {
                    updateNumberOfWork();
                    showDialogStatus(R.layout.dialog_success_status, R.id.tvStatusSuccess, R.string.status_success_delete);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (show)
                    showDialogStatus(R.layout.dialog_error_status, R.id.tvStatusError, R.string.status_error_delete);
            }
        });
    }

    private void initNotificationAlarm(ScheduleItem scheduleItem, int position) {
        if (Constant.Schedule.OFF_ALARM.equals(scheduleItem.getAlarm()) && Constant.Schedule.NOT_READY_STATUS.equals(scheduleItem.getStatus())) {
            NotificationSchedule.cancelAlarm(context, AlarmReceiver.class, Integer.parseInt(scheduleItem.getRequestCode()));
            Log.d("ALARM", "SET_OFF");
        }
        if (Constant.Schedule.ON_ALARM.equals(scheduleItem.getAlarm()) && Constant.Schedule.NOT_READY_STATUS.equals(scheduleItem.getStatus())) {
            NotificationSchedule.setAlarm(context, AlarmReceiver.class, dataItem, position, idUser);
            Log.d("ALARM", "SET_ON: position" + Integer.valueOf(dataItem.getScheduleItems().get(position).getRequestCode()));
        }
    }

    private String checkLogic(DataItem dataItem, String startTime, String note) throws ParseException {
        String date = dataItem.getDate();
        String error = null;

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

    //update report
    private void updateNumberOfWork() {
        int request = Integer.valueOf(report.getNumberOfWork());
        request--;
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
                    report.setNumberOfWork(snapshot.getData().get(Constant.Schedule.numberOfWork).toString());
                    Log.d("check_report", snapshot.getData().get(Constant.Schedule.numberOfWork).toString());
                } else {
                    Log.d("check_report", "NULL");
                    Map<String, String> nestedData = new HashMap<>();
                    nestedData.put(Constant.Schedule.numberOfWork, "0");
                    db.collection(idUser).document(Constant.Schedule.REPORT_KEY).set(nestedData);
                }

            }
        });
    }

    //class view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStartTime;
        TextView tvNote;
        ImageView imgOption;
        CardView cardViewSchedule;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardViewSchedule = itemView.findViewById(R.id.cardViewSchedule);
            tvStartTime = itemView.findViewById(R.id.tvStartSchedule);
            tvNote = itemView.findViewById(R.id.tvNoteSchedule);
            imgOption = itemView.findViewById(R.id.imgOptionSchedule);
        }
    }
}
