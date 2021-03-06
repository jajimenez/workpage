package jajimenez.workpage;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import jajimenez.workpage.data.model.Task;
import jajimenez.workpage.data.model.TaskContext;
import jajimenez.workpage.data.model.TaskTag;
import jajimenez.workpage.logic.ApplicationLogic;

public class TaskListFragment extends Fragment implements TaskContainerFragment {
    private ListView list;
    private List<Integer> selectedItemPositions;
    private TextView emptyText;

    private Bundle savedInstanceState;
    private TaskListHostActivity activity;

    private AppBroadcastReceiver appBroadcastReceiver;
    private LoadTasksDBTask tasksDbTask = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            activity = (TaskListHostActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement TaskListHostActivity");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        View view = inflater.inflate(R.layout.task_list, container, false);

        list = view.findViewById(R.id.task_list_list);
        list.setVisibility(View.INVISIBLE);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView l, View v, int position, long id) {
                Task task = (Task) l.getItemAtPosition(position);
                if (TaskListFragment.this.activity != null) TaskListFragment.this.activity.onTaskClicked(task);
            }
        });

        emptyText = view.findViewById(R.id.task_list_empty);
        emptyText.setVisibility(View.GONE);

        createContextualActionBar();

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

    private void createContextualActionBar() {
        selectedItemPositions = new LinkedList<>();

        list.clearChoices();
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                TaskListFragment.this.activity.setActionMode(mode);

                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.task, menu);

                return true;
            }

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
                TaskListFragment.this.activity.setActionMode(null);
            }

            // Returns "true" if this callback handled the event, "false"
            // if the standard "MenuItem" invocation should continue.
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                boolean eventHandled = false;
                final List<Task> selectedTasks = getSelectedTasks();

                switch (item.getItemId()) {
                    case R.id.task_menu_status:
                        if (TaskListFragment.this.activity != null)  {
                            TaskListFragment.this.activity.showChangeTaskStatusDialog(selectedTasks);
                        }

                        eventHandled = true;
                        break;

                    case R.id.task_menu_edit:
                        // Open the task edition activity
                        if (TaskListFragment.this.activity != null)  {
                            TaskListFragment.this.activity.showEditActivity(selectedTasks.get(0));
                        }

                        // Close the context action bar
                        mode.finish();

                        eventHandled = true;
                        break;

                    case R.id.task_menu_delete:
                        // Show a deletion confirmation dialog
                        if (TaskListFragment.this.activity != null)  {
                            TaskListFragment.this.activity.showDeleteTaskDialog(selectedTasks);
                        }

                        eventHandled = true;
                        break;
                }

                return eventHandled;
            }

            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // We store the selected items for later accessing them in the
                // "onSaveInstanceState" as calling "getCheckedItemPositions in
                // "onSaveInstanceState" will return an empty collection.
                TaskListFragment.this.selectedItemPositions = getSelectedItemPositions(TaskListFragment.this.list.getCheckedItemPositions());

                int selectedTaskCount = list.getCheckedItemCount();
                if (selectedTaskCount > 0) mode.setTitle((TaskListFragment.this.getActivity()).getString(R.string.selected, selectedTaskCount));

                MenuItem editItem = (mode.getMenu()).findItem(R.id.task_menu_edit);
                Drawable editItemIcon = editItem.getIcon();

                MenuItem deleteItem = (mode.getMenu()).findItem(R.id.task_menu_delete);
                Drawable deleteItemIcon = deleteItem.getIcon();

                if (selectedTaskCount == 1) {
                    editItem.setEnabled(true);
                    editItemIcon.setAlpha(255);
                }
                else {
                    editItem.setEnabled(false);
                    editItemIcon.setAlpha(127);
                }

                deleteItem.setEnabled(true);
                deleteItemIcon.setAlpha(255);
            }
        });
    }

    private List<Integer> getSelectedItemPositions(SparseBooleanArray stateItemPositions) {
        List<Integer> positions = new LinkedList<>();
        int stateItemPositionCount = stateItemPositions.size();

        for (int i = 0; i < stateItemPositionCount; i++) {
            int pos = stateItemPositions.keyAt(i);
            if (stateItemPositions.get(pos)) positions.add(pos);
        }

        return positions;
    }

    private void updateInterface(List<Task> tasks) {
        TaskAdapter adapter = new TaskAdapter(getActivity(), R.layout.task_list_item, tasks);
        list.setAdapter(adapter);

        if (adapter.isEmpty()) {
            list.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        }
        else {
            // Re-select items
            if (savedInstanceState != null) {
                // Recover selected item positions
                int[] selectedItems = savedInstanceState.getIntArray("list_selected_items");

                if (selectedItems != null) {
                    for (int position : selectedItems) list.setItemChecked(position, true);
                    savedInstanceState.remove("list_selected_items");
                }
            }

            list.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        int selectedItemCount = selectedItemPositions.size();
        int[] selected = new int[selectedItemCount];

        for (int i = 0; i < selectedItemCount; i++) selected[i] = selectedItemPositions.get(i);

        // Store selected item positions
        outState.putIntArray("list_selected_items", selected);

        ActionMode mode = activity.getActionMode();
        if (mode != null) mode.finish();

        super.onSaveInstanceState(outState);
    }

    private List<Task> getSelectedTasks() {
        List<Task> selectedTasks = new LinkedList<>();

        TaskAdapter adapter = (TaskAdapter) list.getAdapter();

        for (int position: selectedItemPositions) {
            Task task = adapter.getItem(position);
            selectedTasks.add(task);
        }

        return selectedTasks;
    }

    @Override
    public void setVisible(boolean visible) {
        View root = getView();

        if (visible) root.setVisibility(View.VISIBLE);
        else root.setVisibility(View.GONE);
    }

    private void closeActionBar() {
        // Close the context action bar
        ActionMode mode = activity.getActionMode();
        if (mode != null) mode.finish();
    }

    private void loadTasks() {
        if (tasksDbTask == null || tasksDbTask.getStatus() == AsyncTask.Status.FINISHED) {
            tasksDbTask = new LoadTasksDBTask();
            tasksDbTask.execute();
        }
    }

    private class AppBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            TaskListFragment.this.closeActionBar();
            String action = intent.getAction();

            if (action != null &&
                    (action.equals(ApplicationLogic.ACTION_DATA_CHANGED) ||
                     action.equals(ApplicationLogic.ACTION_DATA_CHANGED_TASK_DELETED))) {

                    // Get the tasks
                    TaskListFragment.this.loadTasks();
            }
        }
    }

    private class LoadTasksDBTask extends AsyncTask<Void, Void, List<Task>> {
        protected void onPreExecute() {
            TaskListFragment.this.list.setEnabled(false);
        }

        protected List<Task> doInBackground(Void... parameters) {
            List<Task> tasks = new LinkedList<>();

            try {
                ApplicationLogic applicationLogic = new ApplicationLogic(TaskListFragment.this.getContext());
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
                TaskListFragment.this.updateInterface(tasks);
                TaskListFragment.this.list.setEnabled(true);
            } catch (Exception e) {
                // Nothing to do
            }
        }
    }
}
