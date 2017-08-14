package org.smartregister.immunization.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.smartregister.domain.Alert;
import org.smartregister.domain.db.Event;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.R;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.util.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by raihan on 13/03/2017.
 */

public class ImmunizationRowCard extends LinearLayout {
    private static final String TAG = "ImmunizationRowCard";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private Context context;
    private Button statusIV;
    private TextView nameTV;
    private TextView StatusTV;
    private Button undoB;
    private State state;
    private OnVaccineStateChangeListener onVaccineStateChangeListener;
    private VaccineWrapper vaccineWrapper;
    public boolean editmode;

    public ImmunizationRowCard(Context context, boolean editmode) {
        super(context);
        this.editmode = editmode;
        init(context);
    }

    public static enum State {
        DONE_CAN_BE_UNDONE,
        DONE_CAN_NOT_BE_UNDONE,
        DUE,
        NOT_DUE,
        OVERDUE,
        EXPIRED
    }

    public ImmunizationRowCard(Context context) {
        super(context);
        init(context);
    }

    public ImmunizationRowCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImmunizationRowCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ImmunizationRowCard(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_immunization_row_card, this, true);
        statusIV = (Button) findViewById(R.id.status_iv);
        StatusTV = (TextView) findViewById(R.id.status_text_tv);
        nameTV = (TextView) findViewById(R.id.name_tv);
        undoB = (Button) findViewById(R.id.undo_b);
    }

    public void setVaccineWrapper(VaccineWrapper vaccineWrapper) {
        this.vaccineWrapper = vaccineWrapper;
        updateState();
    }

    public VaccineWrapper getVaccineWrapper() {
        return this.vaccineWrapper;
    }

    public void updateState() {
        this.state = State.NOT_DUE;
        if (vaccineWrapper != null) {
            Date dateDone = getDateDone();
            boolean isSynced = isSynced();
            String status = getStatus();

            if (dateDone != null) {// Vaccination was done
                if (isSynced) {
                    this.state = State.DONE_CAN_NOT_BE_UNDONE;
                } else {
                    this.state = State.DONE_CAN_BE_UNDONE;
                }
            } else {// Vaccination has not been done
                if (status != null) {
                    if ("due".equalsIgnoreCase(status)) {
                        Alert alert = getAlert();
                       if ("normal".equalsIgnoreCase(alert.status().value())) {
                            state = State.DUE;
                        } else if ("urgent".equalsIgnoreCase(alert.status().value())) {
                            state = State.OVERDUE;
                        } else if ("expired".equalsIgnoreCase(alert.status().value())) {
                            state = State.EXPIRED;
                        }
                    } else if ("expired".equalsIgnoreCase(vaccineWrapper.getStatus())) {
                        state = State.EXPIRED;
                    }
                }

//                Calendar today = Calendar.getInstance();
//                today.set(Calendar.HOUR_OF_DAY, 0);
//                today.set(Calendar.MINUTE, 0);
//                today.set(Calendar.SECOND, 0);
//                today.set(Calendar.MILLISECOND, 0);
//                if (getDateDue().getTime() > (today.getTimeInMillis() + TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS))) {
//                    // Vaccination due more than one day from today
//                    this.state = State.NOT_DUE;
//                } else if (getDateDue().getTime() < (today.getTimeInMillis() - TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS))) {
//                    // Vaccination overdue
//                    this.state = State.OVERDUE;
//                } else {
//                    this.state = State.DUE;
//                }
            }
        }

        updateStateUi();
    }

    public void setOnVaccineStateChangeListener(OnVaccineStateChangeListener onVaccineStateChangeListener) {
        this.onVaccineStateChangeListener = onVaccineStateChangeListener;
    }

    public State getState() {
        updateState();
        return this.state;
    }

    private void updateStateUi() {
        boolean status_for_more_than_three_months = false;
        VaccineRepository vaccineRepository = ImmunizationLibrary.getInstance().vaccineRepository();
        if (vaccineWrapper.getDbKey() != null) {
            Vaccine vaccine = vaccineRepository.find(vaccineWrapper.getDbKey());
            EventClientRepository db = ImmunizationLibrary.getInstance().eventClientRepository();

            Event event = null;
            if (vaccine.getEventId() != null) {
                event = db.convert(db.getEventsByEventId(vaccine.getEventId()), Event.class);
            } else if (vaccine.getFormSubmissionId() != null) {
                event = db.convert(db.getEventsByFormSubmissionId(vaccine.getFormSubmissionId()), Event.class);
            }

            if (event != null) {
                Date vaccine_create_date = event.getDateCreated().toDate();
                status_for_more_than_three_months = DateUtil.checkIfDateThreeMonthsOlder(vaccine_create_date);
            }
        }
//        boolean status_for_more_than_three_months = false;
        switch (state) {
            case NOT_DUE:
                setBackgroundResource(R.drawable.vaccine_card_background_white);
                statusIV.setBackgroundResource(R.drawable.vaccine_card_background_white);
                undoB.setVisibility(INVISIBLE);
                nameTV.setVisibility(VISIBLE);
                nameTV.setTextColor(context.getResources().getColor(R.color.silver));
                nameTV.setText(getVaccineName());
                StatusTV.setText(DATE_FORMAT.format(getDateDue()));
                setClickable(false);
                break;
            case DUE:
                setBackgroundResource(R.drawable.vaccine_card_background_white);
                statusIV.setBackgroundResource(R.drawable.vaccine_card_background_blue);
                undoB.setVisibility(INVISIBLE);
                nameTV.setVisibility(VISIBLE);
                nameTV.setText(getVaccineName());
                StatusTV.setText(DATE_FORMAT.format(getDateDue()));
                setClickable(false);
                break;
            case DONE_CAN_BE_UNDONE:
                setBackgroundResource(R.drawable.vaccine_card_background_white);
                statusIV.setBackgroundResource(R.drawable.vaccine_card_background_green);
                if (editmode && !status_for_more_than_three_months) {
                    undoB.setVisibility(VISIBLE);
                } else {
                    undoB.setVisibility(INVISIBLE);
                }
                nameTV.setVisibility(VISIBLE);
                nameTV.setText(getVaccineName());
                StatusTV.setText(DATE_FORMAT.format(getDateDone()));
                setClickable(false);
                break;
            case DONE_CAN_NOT_BE_UNDONE:
                setBackgroundResource(R.drawable.vaccine_card_background_white);
                statusIV.setBackgroundResource(R.drawable.vaccine_card_background_green);
                if (editmode && !status_for_more_than_three_months) {
                    undoB.setVisibility(VISIBLE);
                } else {
                    undoB.setVisibility(INVISIBLE);
                }
                nameTV.setVisibility(VISIBLE);
                nameTV.setText(getVaccineName());
                StatusTV.setText(DATE_FORMAT.format(getDateDone()));
                setClickable(false);
                break;
            case OVERDUE:
                setBackgroundResource(R.drawable.vaccine_card_background_white);
                statusIV.setBackgroundResource(R.drawable.vaccine_card_background_red);
                undoB.setVisibility(INVISIBLE);
                nameTV.setVisibility(VISIBLE);
                nameTV.setText(getVaccineName());
                StatusTV.setText(DATE_FORMAT.format(getDateDue()));
                setClickable(false);
                break;
            case EXPIRED:
                setBackgroundResource(R.drawable.vaccine_card_background_white);
                statusIV.setBackgroundResource(R.drawable.vaccine_card_background_white);
                undoB.setVisibility(INVISIBLE);
                nameTV.setText(getVaccineName());
                StatusTV.setText("Expired");
                StatusTV.setTextColor(context.getResources().getColor(R.color.silver));
                setClickable(false);
                break;
            default:
                break;
        }
    }

    private String getVaccineName() {
        if (vaccineWrapper != null) {
            return vaccineWrapper.getName();
        }
        return null;
    }

    private Date getDateDue() {
        if (vaccineWrapper != null) {
            DateTime vaccineDate = vaccineWrapper.getVaccineDate();
            if (vaccineDate != null) return vaccineDate.toDate();
        }
        return null;
    }

    private Date getDateDone() {
        if (vaccineWrapper != null) {
            DateTime dateDone = vaccineWrapper.getUpdatedVaccineDate();
            if (dateDone != null) return dateDone.toDate();
        }

        return null;
    }

    private boolean isSynced() {
        if (vaccineWrapper != null) {
            return vaccineWrapper.isSynced();
        }
        return false;
    }

    private Alert getAlert() {
        if (vaccineWrapper != null) {
            return vaccineWrapper.getAlert();
        }
        return null;
    }

    private String getStatus() {
        if (vaccineWrapper != null) {
            return vaccineWrapper.getStatus();
        }
        return null;
    }

    public static interface OnVaccineStateChangeListener {
        void onStateChanged(final State newState);
    }

    public Button getUndoB() {
        return undoB;
    }
}
