package jajimenez.workpage;

import java.util.List;
import java.util.LinkedList;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageView;
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

    private LinearLayout whenInformation;
    private TextView whenValueTextView;
    private ImageView whenDstImageView;

    private TableLayout datesTableLayout;

    private TextView date1TitleTextView;
    private TextView date1ValueTextView;
    private ImageView date1DstImageView;

    private TableRow date2TableRow;
    private TextView date2TitleTextView;
    private TextView date2ValueTextView;
    private ImageView date2DstImageView;

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

        colorsLinearLayout = (LinearLayout) itemView.findViewById(R.id.task_list_item_colors);
        titleTextView = (TextView) itemView.findViewById(R.id.task_list_item_title);
        tagsTextView = (TextView) itemView.findViewById(R.id.task_list_item_tags);

        whenInformation = (LinearLayout) itemView.findViewById(R.id.task_list_item_when_information);
        whenValueTextView = (TextView) itemView.findViewById(R.id.task_list_item_when_value);
        whenDstImageView = (ImageView) itemView.findViewById(R.id.task_list_item_when_dst);

        datesTableLayout = (TableLayout) itemView.findViewById(R.id.task_list_item_dates);

        date1TitleTextView = (TextView) itemView.findViewById(R.id.task_list_item_date1_title);
        date1ValueTextView = (TextView) itemView.findViewById(R.id.task_list_item_date1_value);
        date1DstImageView = (ImageView) itemView.findViewById(R.id.task_list_item_date1_dst);

        date2TableRow = (TableRow) itemView.findViewById(R.id.task_list_item_date2_row);
        date2TitleTextView = (TextView) itemView.findViewById(R.id.task_list_item_date2_title);
        date2ValueTextView = (TextView) itemView.findViewById(R.id.task_list_item_date2_value);
        date2DstImageView = (ImageView) itemView.findViewById(R.id.task_list_item_date2_dst);

        space1TextView = (TextView) itemView.findViewById(R.id.task_list_item_space1);
        space2TextView = (TextView) itemView.findViewById(R.id.task_list_item_space2);
        space3TextView = (TextView) itemView.findViewById(R.id.task_list_item_space3);

        task = getItem(position);

        // Show title.
        String title = task.getTitle();
        titleTextView.setText(title);

        // Show tags.
        showTags();

        // Show dates.
        showDates();

        return itemView;
    }

    private void showTags() {
        TextTool textTool = new TextTool();
        List<TaskTag> tags = task.getTags();

        // Set background color based on tag colors.
        int tagCount = 0;
        if (tags != null) tagCount = tags.size();

        if (tagCount > 0) {
            // Tag names.
            tagsTextView.setText(textTool.getTagsText(activity, task));

            // Tag colors.
            LinkedList<String> colors = new LinkedList<String>();

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

        Calendar when = task.getWhen();
        Calendar start = task.getStart();
        Calendar deadline = task.getDeadline();

        // When is defined.
        if (when != null) {
            whenInformation.setVisibility(View.VISIBLE);
            datesTableLayout.setVisibility(View.GONE);

            space2TextView.setVisibility(View.INVISIBLE);

            whenValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.WHEN));

            TimeZone whenTimeZone = when.getTimeZone();

            if (whenTimeZone.inDaylightTime(when.getTime())) whenDstImageView.setVisibility(View.VISIBLE);
            else whenDstImageView.setVisibility(View.GONE);
        }
        // Any of Start and Deadline is defined.
        else if (start != null || deadline != null) {
            whenInformation.setVisibility(View.GONE);
            datesTableLayout.setVisibility(View.VISIBLE);

            // Both are defined.
            if (start != null && deadline != null) {
                date1TitleTextView.setText(activity.getString(R.string.start_2));
                date1ValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.START));

                date2TitleTextView.setText(activity.getString(R.string.deadline_2));
                date2ValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.DEADLINE));

                TimeZone startTimeZone = start.getTimeZone();
                TimeZone deadlineTimeZone = deadline.getTimeZone();

                if (startTimeZone.inDaylightTime(start.getTime())) date1DstImageView.setVisibility(View.VISIBLE);
                else date1DstImageView.setVisibility(View.GONE);

                if (deadlineTimeZone.inDaylightTime(deadline.getTime())) date2DstImageView.setVisibility(View.VISIBLE);
                else date2DstImageView.setVisibility(View.GONE);
            }
            // Only one is defined.
            else {
                date2TableRow.setVisibility(View.GONE);
                space2TextView.setVisibility(View.INVISIBLE);

                if (start != null) {
                    date1TitleTextView.setText(activity.getString(R.string.start_2));
                    date1ValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.START));

                    TimeZone startTimeZone = start.getTimeZone();

                    if (startTimeZone.inDaylightTime(start.getTime())) date1DstImageView.setVisibility(View.VISIBLE);
                    else date1DstImageView.setVisibility(View.GONE);
                }
                else { // deadline != null
                    date1TitleTextView.setText(activity.getString(R.string.deadline_2));
                    date1ValueTextView.setText(tool.getTaskDateText(activity, task, false, TextTool.DEADLINE));

                    TimeZone deadlineTimeZone = deadline.getTimeZone();

                    if (deadlineTimeZone.inDaylightTime(deadline.getTime())) date2DstImageView.setVisibility(View.VISIBLE);
                    else date2DstImageView.setVisibility(View.GONE);
                }
            }
        }
        else { // No date is defined.
            whenInformation.setVisibility(View.GONE);
            datesTableLayout.setVisibility(View.GONE);

            space2TextView.setVisibility(View.INVISIBLE);
            space3TextView.setVisibility(View.INVISIBLE);
        }
    }
}