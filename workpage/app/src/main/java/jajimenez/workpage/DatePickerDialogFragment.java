package jajimenez.workpage;

import java.util.Calendar;

import android.os.Bundle;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DatePickerDialog;
import android.widget.DatePicker;

public class DatePickerDialogFragment extends DialogFragment {
    private DatePickerDialog dialog;
    private Calendar calendar;
    private OnDateSetListener onDateSetListener;

    public DatePickerDialogFragment() {
        dialog = null;
        calendar = Calendar.getInstance();

        onDateSetListener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        long time = arguments.getLong("time");
        calendar.setTimeInMillis(time);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int month, int day) {
                // There is a bug in DatePickerDialog and TimePickerDialog
                // that makes always onDateSet/onTimeSet methods be
                // called twice (when the "Done" button is clicked and
                // when the dialog is dismissed). A workaround is to
                // check if the view is visible.
                if (view.isShown() && DatePickerDialogFragment.this.onDateSetListener != null) {
                    DatePickerDialogFragment.this.onDateSetListener.onDateSet(year, month, day);
                }
            }

        }, year, month, day);

        return dialog;
    }

    public void setOnDateSetListener(OnDateSetListener listener) {
        onDateSetListener = listener;
    }

    public static interface OnDateSetListener {
        void onDateSet(int year, int month, int day);
    }
}