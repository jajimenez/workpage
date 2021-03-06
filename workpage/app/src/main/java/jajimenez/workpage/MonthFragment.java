package jajimenez.workpage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jajimenez.workpage.data.model.Task;
import jajimenez.workpage.data.model.TaskContext;
import jajimenez.workpage.data.model.TaskTag;
import jajimenez.workpage.logic.ApplicationLogic;
import jajimenez.workpage.logic.DateTimeTool;
import jajimenez.workpage.logic.TextTool;

public class MonthFragment extends Fragment {
    // Minimum supported date
    public static final int MIN_YEAR = 1900;
    public static final int MIN_MONTH = Calendar.JANUARY;

    // Maximum supported date
    public static final int MAX_YEAR = 10000;
    public static final int MAX_MONTH = Calendar.DECEMBER;

    private TextView title;
    private TableLayout table;
    private TextView listTitle;
    private DateTaskListFragment dateListFragment;

    private LinearLayout selectedDateCell;
    private Calendar selectedDate;

    private Drawable defaultDateDrawable;
    private Drawable taskDateDrawable;
    private Map<TextView, Integer> dateTextColors;
    private Drawable defaultDateNumberDrawable;
    private Drawable selectedDateNumberDrawable;
    private boolean isTablet;

    private Bundle savedInstanceState;

    // Date to represent
    private int currentYear;
    private int currentMonth;
    private Calendar current;

    private LoadTasksDBTask tasksDbTask = null;
    private AppBroadcastReceiver appBroadcastReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isTablet = (getResources()).getBoolean(R.bool.is_tablet);

        this.savedInstanceState = savedInstanceState;
        Bundle arguments = getArguments();

        if (arguments != null) {
            currentYear = arguments.getInt("year", MIN_YEAR);
            currentMonth = arguments.getInt("month", MIN_MONTH);
        } else {
            currentYear = MIN_YEAR;
            currentMonth = MIN_MONTH;
        }

        if (currentYear < MIN_YEAR || (currentYear == MIN_YEAR && currentMonth < MIN_MONTH)) {
            throw new IllegalArgumentException("Date lower than the minimum date supported.");
        }

        if (currentYear > MAX_YEAR || (currentYear == MAX_YEAR && currentMonth > MAX_MONTH)) {
            throw new IllegalArgumentException("Date greater than the maximum date supported.");
        }

        TextTool textTool = new TextTool();
        DateTimeTool dateTool = new DateTimeTool();

        // Date to represent
        current = Calendar.getInstance();
        current.set(Calendar.YEAR, currentYear);
        current.set(Calendar.MONTH, currentMonth);
        current.set(Calendar.DAY_OF_MONTH, 1);
        dateTool.clearTimeFields(current);

        View view = inflater.inflate(R.layout.month, container, false);

        title = view.findViewById(R.id.month_title);
        title.setText(textTool.getMonthYearName(current));

        table = view.findViewById(R.id.month_table);

        if (isTablet) {
            listTitle = view.findViewById(R.id.month_list_title);
        } else {
            listTitle = null;
        }

        dateListFragment = (DateTaskListFragment) (getChildFragmentManager()).findFragmentById(R.id.month_date_list);

        // Drawables
        TableRow row = (TableRow) table.getChildAt(1);
        LinearLayout cell = (LinearLayout) row.getChildAt(0);
        TextView text = (TextView) cell.getChildAt(0);

        Resources resources = getResources();
        defaultDateDrawable = cell.getBackground();
        taskDateDrawable = resources.getDrawable(R.drawable.task_date);

        dateTextColors = new HashMap<>();
        defaultDateNumberDrawable = text.getBackground();
        selectedDateNumberDrawable = resources.getDrawable(R.drawable.selected_date_number);

        if (savedInstanceState != null) selectedDate = getSavedSelectedDate(savedInstanceState);

        // Initial task load
        loadTasks();

        // Broadcast receiver
        registerBroadcastReceiver();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Broadcast receiver
        unregisterBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {
        appBroadcastReceiver = new AppBroadcastReceiver();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getContext());

        IntentFilter intentFilter1 = new IntentFilter(ApplicationLogic.ACTION_DATA_CHANGED);
        IntentFilter intentFilter2 = new IntentFilter(ApplicationLogic.ACTION_DATA_CHANGED_TASK_DELETED);

        manager.registerReceiver(appBroadcastReceiver, intentFilter1);
        manager.registerReceiver(appBroadcastReceiver, intentFilter2);
    }

    private void unregisterBroadcastReceiver() {
        (LocalBroadcastManager.getInstance(getContext())).unregisterReceiver(appBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (selectedDate != null) outState.putLong("selected_date", selectedDate.getTimeInMillis());
        super.onSaveInstanceState(outState);
    }

    private Calendar getSavedSelectedDate(Bundle savedInstanceState) {
        Calendar selectedDate = null;

        if (savedInstanceState != null) {
            long selectedDateTime = savedInstanceState.getLong("selected_date");

            selectedDate = Calendar.getInstance();
            selectedDate.setTimeInMillis(selectedDateTime);
        }

        return selectedDate;
    }

    private void setupWeekDayViews() {
        ApplicationLogic logic = new ApplicationLogic(getContext());
        int weekStartDay = logic.getWeekStartDay();

        TextTool textTool = new TextTool();
        TableRow row = (TableRow) table.getChildAt(0);

        String[] names = textTool.getWeekDayShortNames();
        int count = names.length;

        for (int i = 0; i < count; i++) {
            int j = (weekStartDay + i) % count;

            TextView cell = (TextView) row.getChildAt(i);
            cell.setText(names[j]);
        }
    }

    private void updateInterface(final List<Task> tasks) {
        setupWeekDayViews();

        ApplicationLogic logic = new ApplicationLogic(getContext());
        int weekStartDay = logic.getWeekStartDay();

        Resources resources = getResources();
        DateTimeTool dateTool = new DateTimeTool();

        int currentMonthDayCount = current.getActualMaximum(Calendar.DAY_OF_MONTH);

        // We get the cell indexes of the first day and the last day of the month.
        // The DAY_OF_WEEK value is between 1 and 7, so we subtract 1.
        int currentFirstDayIndex = (current.get(Calendar.DAY_OF_WEEK) - 1 ) - weekStartDay;
        if (currentFirstDayIndex < 0) currentFirstDayIndex = 7 + currentFirstDayIndex;

        int currentLastDayIndex = currentFirstDayIndex + currentMonthDayCount - 1;

        // Previous month
        Calendar prev = (Calendar) current.clone();
        prev.add(Calendar.MONTH, -1);
        int prevMonthDayCount = prev.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Later month
        Calendar later = (Calendar) current.clone();
        later.add(Calendar.MONTH, 1);

        // Today
        Calendar today = Calendar.getInstance();
        dateTool.clearTimeFields(today);

        int i = 0;
        int rowCount = 7; // 7 = 1 header row + 6 regular rows
        int cellsPerRow = 7;

        for (int r = 1; r < rowCount; r++) {
            TableRow row = (TableRow) table.getChildAt(r);

            for (int c = 0; c < cellsPerRow; c++) {
                LinearLayout cell = (LinearLayout) row.getChildAt(c);
                TextView cellText = cell.findViewById(R.id.month_cell_day);

                int monthDay;
                final Calendar date;

                if (i < currentFirstDayIndex) {
                    // The day belongs to the previous month
                    monthDay = prevMonthDayCount - currentFirstDayIndex + i + 1;
                    date = getDate(prev, monthDay);

                    if (date.getTimeInMillis() == today.getTimeInMillis()) {
                        cellText.setTextColor(resources.getColor(R.color.today));
                        cellText.setTypeface(null, Typeface.BOLD);
                    } else {
                        cellText.setTextColor(resources.getColor(R.color.disabled_text_color));
                        cellText.setTypeface(null, Typeface.NORMAL);
                    }

                } else if (i > currentLastDayIndex) {
                    // The day belong to the next month
                    monthDay = i - (currentFirstDayIndex + currentMonthDayCount) + 1;
                    date = getDate(later, monthDay);

                    if (date.getTimeInMillis() == today.getTimeInMillis()) {
                        cellText.setTextColor(resources.getColor(R.color.today));
                        cellText.setTypeface(null, Typeface.BOLD);
                    } else {
                        cellText.setTextColor(resources.getColor(R.color.disabled_text_color));
                        cellText.setTypeface(null, Typeface.NORMAL);
                    }

                } else {
                    // The day belongs to the current month
                    monthDay = i - currentFirstDayIndex + 1;
                    date = getDate(current, monthDay);

                    if (date.getTimeInMillis() == today.getTimeInMillis()) {
                        cellText.setTextColor(resources.getColor(R.color.today));
                        cellText.setTypeface(null, Typeface.BOLD);
                    } else {
                        cellText.setTextColor(resources.getColor(R.color.text_color));
                        cellText.setTypeface(null, Typeface.NORMAL);
                    }
                }

                cellText.setText(String.valueOf(monthDay));
                dateTextColors.put(cellText, cellText.getCurrentTextColor());

                if ((logic.getDateTasks(tasks, date)).size() > 0) cell.setBackground(taskDateDrawable);
                else cell.setBackground(defaultDateDrawable);

                cell.setOnClickListener(new LinearLayout.OnClickListener() {
                    public void onClick(View view) {
                        MonthFragment.this.selectCell(tasks, (LinearLayout) view, date);
                    }
                });

                if (selectedDate != null && date.equals(selectedDate)) selectCell(tasks, cell, date);

                i++;
            }
        }
    }

    private void selectCell(List<Task> tasks, LinearLayout cell, Calendar date) {
        Resources resources = getResources();
        ApplicationLogic logic = new ApplicationLogic(getContext());
        TextTool tool = new TextTool();

        TextView cellText = cell.findViewById(R.id.month_cell_day);
        cellText.setBackground(selectedDateNumberDrawable);
        cellText.setTextColor(resources.getColor(R.color.selected_date_text));

        if (selectedDateCell != null && selectedDateCell != cell) clearSelection();

        selectedDateCell = cell;
        selectedDate = date;

        // Tasks
        List<Task> dateTasks = logic.getDateTasks(tasks, date);

        // List title
        if (listTitle != null) {
            if (dateTasks.size() == 0) {
                listTitle.setVisibility(View.GONE);
                listTitle.setText("");
            } else {
                listTitle.setVisibility(View.VISIBLE);

                String dateText = tool.getFormattedDate(getContext(), date, true);
                listTitle.setText(resources.getString(R.string.date_tasks, dateText));
            }
        }

        // List
        dateListFragment.setTasks(dateTasks);
    }

    private void resetSelectedDateCell() {
        if (selectedDateCell != null) {
            TextView t = (TextView) selectedDateCell.getChildAt(0);
            t.setBackground(defaultDateNumberDrawable);
            t.setTextColor(dateTextColors.get(t));
        }
    }

    private Calendar getDate(Calendar c, int monthDay) {
        Calendar date = (Calendar) c.clone();
        date.set(Calendar.DAY_OF_MONTH, monthDay);

        return date;
    }

    private void loadTasks() {
        if (tasksDbTask == null || tasksDbTask.getStatus() == AsyncTask.Status.FINISHED) {
            tasksDbTask = new LoadTasksDBTask();
            tasksDbTask.execute();
        }
    }

    public void clearSelection() {
        if (savedInstanceState != null) savedInstanceState.clear();

        resetSelectedDateCell();
        selectedDateCell = null;
        selectedDate = null;

        if (dateListFragment != null) dateListFragment.clearSelection();
    }

    private class AppBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null &&
                    (action.equals(ApplicationLogic.ACTION_DATA_CHANGED) ||
                     action.equals(ApplicationLogic.ACTION_DATA_CHANGED_TASK_DELETED))) {
                // Get the tasks
                loadTasks();
            }
        }
    }

    private class LoadTasksDBTask extends AsyncTask<Void, Void, List<Task>> {
        protected void onPreExecute() {
            table.setEnabled(false);
            dateListFragment.setEnabled(false);
        }

        protected List<Task> doInBackground(Void... parameters) {
            List<Task> tasks = new LinkedList<>();

            try {
                ApplicationLogic applicationLogic = new ApplicationLogic(MonthFragment.this.getContext());
                TaskContext currentTaskContext = applicationLogic.getCurrentTaskContext();

                // View filters
                String viewStateFilter = applicationLogic.getViewStateFilter();
                boolean includeTasksWithNoTag = applicationLogic.getIncludeTasksWithNoTag();
                List<TaskTag> currentFilterTags = applicationLogic.getCurrentFilterTags();

                switch (viewStateFilter) {
                    case "open":
                        tasks = applicationLogic.getOpenTasksByTags(currentTaskContext,
                                includeTasksWithNoTag,
                                currentFilterTags);
                        break;
                    case "doable_today":
                        tasks = applicationLogic.getDoableTodayTasksByTags(currentTaskContext,
                                includeTasksWithNoTag,
                                currentFilterTags);
                        break;
                    default:
                        tasks = applicationLogic.getClosedTasksByTags(currentTaskContext,
                                includeTasksWithNoTag,
                                currentFilterTags);
                }
            }
            catch (Exception e) {
                // Nothing to do
            }

            return tasks;
        }

        protected void onPostExecute(List<Task> tasks) {
            try {
                MonthFragment.this.updateInterface(tasks);

                MonthFragment.this.table.setEnabled(true);
                MonthFragment.this.dateListFragment.setEnabled(true);
            } catch (Exception e) {
                // Nothing to do
            }
        }
    }
}
