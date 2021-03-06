package jajimenez.workpage;

import java.util.List;
import java.util.LinkedList;
import java.util.Calendar;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.graphics.Color;

import jajimenez.workpage.logic.TextTool;
import jajimenez.workpage.data.model.TaskTag;
import jajimenez.workpage.data.model.Task;

public class TaskAdapter extends ArrayAdapter<Task> {
    private LinearLayout colorsLinearLayout;
    private TextView titleTextView;
    private TextView tagsTextView;

    private TextView singleValueTextView;

    private TableLayout datesTableLayout;

    private TextView date1TitleTextView;
    private TextView date1ValueTextView;

    private TableRow date2TableRow;
    private TextView date2TitleTextView;
    private TextView date2ValueTextView;

    private TextView space1TextView;
    private TextView space2TextView;
    private TextView space3TextView;

    private Activity activity;
    private int resource;

    private Task task;

    public TaskAdapter(Activity activity, int resource, List<Task> items) {
        super(activity, resource, items);

        this.activity = activity;
        this.resource = resource;
        this.task = null;
    }

    @Override
    public View getView(int position, View itemView, ViewGroup parentViewGroup) {
        LayoutInflater inflater = activity.getLayoutInflater();
        itemView = inflater.inflate(resource, null);

        colorsLinearLayout = itemView.findViewById(R.id.task_list_item_colors);
        titleTextView = itemView.findViewById(R.id.task_list_item_title);
        tagsTextView = itemView.findViewById(R.id.task_list_item_tags);

        singleValueTextView = itemView.findViewById(R.id.task_list_item_single_value);

        datesTableLayout = itemView.findViewById(R.id.task_list_item_dates);

        date1TitleTextView = itemView.findViewById(R.id.task_list_item_date1_title);
        date1ValueTextView = itemView.findViewById(R.id.task_list_item_date1_value);

        date2TableRow = itemView.findViewById(R.id.task_list_item_date2_row);
        date2TitleTextView = itemView.findViewById(R.id.task_list_item_date2_title);
        date2ValueTextView = itemView.findViewById(R.id.task_list_item_date2_value);

        space1TextView = itemView.findViewById(R.id.task_list_item_space1);
        space2TextView = itemView.findViewById(R.id.task_list_item_space2);
        space3TextView = itemView.findViewById(R.id.task_list_item_space3);

        task = getItem(position);

        // Show title
        String title = task.getTitle();
        titleTextView.setText(title);

        // Show tags
        showTags();

        // Show dates
        showDates();

        return itemView;
    }

    private void showTags() {
        TextTool textTool = new TextTool();
        List<TaskTag> tags = task.getTags();

        // Set background color based on tag colors
        int tagCount = 0;
        if (tags != null) tagCount = tags.size();

        if (tagCount > 0) {
            // Tag names
            tagsTextView.setText(textTool.getTagsText(activity, task));

            // Tag colors
            LinkedList<String> colors = new LinkedList<>();

            for (int i = 0; i < tagCount; i++) {
                TaskTag t = tags.get(i);

                String color = t.getColor();
                if (color != null) colors.add(color);
            }

            int colorCount = colors.size();

            // "Color.parseColor" converts the hexadecimal color to int-color.
            // We draw every color (one per tag) with a maximum of 10 colors.
            for (int i = 0; i < colorCount && i < 10; i++) {
                ColorView colorView = new ColorView(activity);

                colorView.setBackgroundColor(Color.parseColor(colors.get(i)));
                colorView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

                colorsLinearLayout.addView(colorView);
            }
        } else {
            tagsTextView.setVisibility(View.GONE);
            space1TextView.setVisibility(View.INVISIBLE);
        }
    }

    private void showDates() {
        TextTool tool = new TextTool();

        Calendar single = task.getSingle();
        Calendar start = task.getStart();
        Calendar end = task.getEnd();

        // Single is defined.
        if (single != null) {
            singleValueTextView.setVisibility(View.VISIBLE);
            datesTableLayout.setVisibility(View.GONE);
            space2TextView.setVisibility(View.INVISIBLE);
            singleValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.SINGLE, true));
        }
        // Any of Start and End is defined.
        else if (start != null || end != null) {
            singleValueTextView.setVisibility(View.GONE);
            datesTableLayout.setVisibility(View.VISIBLE);

            // Both are defined.
            if (start != null && end != null) {
                date1TitleTextView.setText(activity.getString(R.string.start_2));
                date1ValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.START, true));

                date2TitleTextView.setText(activity.getString(R.string.end_2));
                date2ValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.END, true));
            }
            // Only one is defined.
            else {
                date2TableRow.setVisibility(View.GONE);
                space2TextView.setVisibility(View.INVISIBLE);

                if (start != null) {
                    date1TitleTextView.setText(activity.getString(R.string.start_2));
                    date1ValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.START, true));
                }
                else { // end != null
                    date1TitleTextView.setText(activity.getString(R.string.end_2));
                    date1ValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.END, true));
                }
            }
        }
        else { // No date is defined.
            singleValueTextView.setVisibility(View.GONE);
            datesTableLayout.setVisibility(View.GONE);

            space2TextView.setVisibility(View.INVISIBLE);
            space3TextView.setVisibility(View.INVISIBLE);
        }
    }
}
