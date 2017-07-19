package jajimenez.workpage;

import java.util.List;
import java.util.LinkedList;
import java.util.Calendar;

import android.support.v7.app.AppCompatActivity;
import android.app.ActionBar;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.graphics.drawable.Drawable;

import jajimenez.workpage.logic.ApplicationLogic;
import jajimenez.workpage.logic.TextTool;
import jajimenez.workpage.data.model.TaskContext;
import jajimenez.workpage.data.model.Task;
import jajimenez.workpage.data.model.TaskTag;

public class TaskActivity extends AppCompatActivity {
    private TextView titleTextView;
    private TextView tagsTextView;
    private TextView whenValueTextView;
    private TableLayout datesTableLayout;

    private TableRow date1TableRow;
    private TextView date1TitleTextView;
    private TextView date1ValueTextView;

    private TableRow date2TableRow;
    private TextView date2TitleTextView;
    private TextView date2ValueTextView;

    private TextView descriptionTextView;

    private ActionBar actionBar;

    private ChangeTaskStatusDialogFragment.OnItemClickListener taskStatusChangeListener;
    private DeleteTaskDialogFragment.OnDeleteListener deleteTaskListener;

    private long currentTaskId;
    private Task currentTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task);

        titleTextView = (TextView) findViewById(R.id.task_title);
        tagsTextView = (TextView) findViewById(R.id.task_tags);
        whenValueTextView = (TextView) findViewById(R.id.task_when_value);
        datesTableLayout = (TableLayout) findViewById(R.id.task_dates);

        date1TableRow = (TableRow) findViewById(R.id.task_date1_row);
        date1TitleTextView = (TextView) findViewById(R.id.task_date1_title);
        date1ValueTextView = (TextView) findViewById(R.id.task_date1_value);

        date2TableRow = (TableRow) findViewById(R.id.task_date2_row);
        date2TitleTextView = (TextView) findViewById(R.id.task_date2_title);
        date2ValueTextView = (TextView) findViewById(R.id.task_date2_value);

        descriptionTextView = (TextView) findViewById(R.id.task_description);

        actionBar = getActionBar();

        taskStatusChangeListener = new ChangeTaskStatusDialogFragment.OnItemClickListener() {
            public void onItemClick() {
                // Update the list view.
                TaskActivity.this.updateInterface();
            }
        };

        deleteTaskListener = new DeleteTaskDialogFragment.OnDeleteListener() {
            public void onDelete() {
                // Close the activity.
                TaskActivity.this.finish();
            }
        };

        if (savedInstanceState != null) {
            ChangeTaskStatusDialogFragment changeTaskStatusFragment = (ChangeTaskStatusDialogFragment) (getFragmentManager()).findFragmentByTag("change_task_status");
            if (changeTaskStatusFragment != null) changeTaskStatusFragment.setOnItemClickListener(taskStatusChangeListener);

            DeleteTaskDialogFragment deleteTaskFragment = (DeleteTaskDialogFragment) (getFragmentManager()).findFragmentByTag("delete_task");
            if (deleteTaskFragment != null) deleteTaskFragment.setOnDeleteListener(deleteTaskListener);
        }

        // Load task data.
        Intent intent = getIntent();
        currentTaskId = intent.getLongExtra("task_id", -1);
        currentTask = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_task, menu);

        // This is necessary because the drawable of the Edit item icon of this activity keeps
        // the last alpha value set for the Edit item icon of MainActivity. MainActivity has
        // the same menu with the same drawables for the items.
        //MenuItem editItem = menu.findItem(R.id.task_menu_edit);
        //Drawable editItemIcon = editItem.getIcon();
        //editItemIcon.setAlpha(255);

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInterface();
    }

    private void updateInterface() {
        ApplicationLogic applicationLogic = new ApplicationLogic(this);
        TextTool textTool = new TextTool();

        // Update Task object after possible changes.
        currentTask = applicationLogic.getTask(currentTaskId);

        if (currentTask.isDone()) setTitle(R.string.task_closed);
        else setTitle(R.string.task_open);

        TaskContext context = applicationLogic.getTaskContext(currentTask.getContextId());
        //actionBar.setSubtitle(context.getName());

        titleTextView.setText(currentTask.getTitle());
        descriptionTextView.setText(currentTask.getDescription());

        // Tags text.
        List<TaskTag> tags = currentTask.getTags();

        int tagCount = 0;
        if (tags != null) tagCount = tags.size();

        if (tagCount == 0) {
            tagsTextView.setText("");
            tagsTextView.setVisibility(View.GONE);
        } else {
            tagsTextView.setText(textTool.getTagsText(this, currentTask));
            tagsTextView.setVisibility(View.VISIBLE);
        }

        // Dates texts.
        TextTool tool = new TextTool();

        Calendar when = currentTask.getWhen();
        Calendar start = currentTask.getStart();
        Calendar deadline = currentTask.getDeadline();

        // When is defined.
        if (when != null) {
            whenValueTextView.setVisibility(View.VISIBLE);
            datesTableLayout.setVisibility(View.GONE);

            whenValueTextView.setText(tool.getTaskDateText(this, currentTask, false, TextTool.WHEN));
        }
        // Any of Start and Deadline is defined.
        else if (start != null || deadline != null) {
            whenValueTextView.setVisibility(View.GONE);
            datesTableLayout.setVisibility(View.VISIBLE);

            // Both are defined.
            if (start != null && deadline != null) {
                date2TableRow.setVisibility(View.VISIBLE);

                date1TitleTextView.setText(getString(R.string.start));
                date1ValueTextView.setText(tool.getTaskDateText(this, currentTask, false, TextTool.START));

                date2TitleTextView.setText(getString(R.string.deadline));
                date2ValueTextView.setText(tool.getTaskDateText(this, currentTask, false, TextTool.DEADLINE));
            }
            // Only one is defined.
            else {
                date2TableRow.setVisibility(View.GONE);

                if (start != null) {
                    date1TitleTextView.setText(getString(R.string.start));
                    date1ValueTextView.setText(tool.getTaskDateText(this, currentTask, false, TextTool.START));
                }
                else { // deadline != null
                    date1TitleTextView.setText(getString(R.string.deadline));
                    date1ValueTextView.setText(tool.getTaskDateText(this, currentTask, false, TextTool.DEADLINE));
                }
            }
        }
        else { // No date is defined.
            whenValueTextView.setVisibility(View.GONE);
            datesTableLayout.setVisibility(View.GONE);
        }
    }

    // Returns "true" if this callback handled the event.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean eventHandled = false;
        //List<Task> tasks = null;
        Bundle arguments;
        long[] taskIds;

        switch (item.getItemId()) {
            case R.id.task_menu_status:
                //tasks = new LinkedList<Task>();
                //tasks.add(currentTask);

                ChangeTaskStatusDialogFragment statusFragment = new ChangeTaskStatusDialogFragment();

                arguments = new Bundle();
                taskIds = new long[1];
                taskIds[0] = currentTask.getId();
                arguments.putLongArray("task_ids", taskIds);

                statusFragment.setArguments(arguments);
                statusFragment.setOnItemClickListener(TaskActivity.this.taskStatusChangeListener);
                statusFragment.show(getFragmentManager(), "change_task_status");

                eventHandled = true;
                break;

            case R.id.task_menu_edit:
                // Open the task edition activity.
                long currentTaskId = currentTask.getId();

                Intent intent = new Intent(this, EditTaskActivity.class);
                intent.putExtra("mode", "edit");
                intent.putExtra("task_id", currentTaskId);

                startActivity(intent);
                eventHandled = true;
                break;

            case R.id.task_menu_delete:
                //tasks = new LinkedList<Task>();
                //tasks.add(currentTask);

                // Show a deletion confirmation dialog.
                DeleteTaskDialogFragment deleteFragment = new DeleteTaskDialogFragment();

                arguments = new Bundle();
                taskIds = new long[1];
                taskIds[0] = currentTask.getId();
                arguments.putLongArray("task_ids", taskIds);

                deleteFragment.setArguments(arguments);
                deleteFragment.setOnDeleteListener(TaskActivity.this.deleteTaskListener);
                deleteFragment.show(getFragmentManager(), "delete_task");

                eventHandled = true;
                break;

            default:
                eventHandled = super.onOptionsItemSelected(item);
                break;
        }

        return eventHandled;
    }
}