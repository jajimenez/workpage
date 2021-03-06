package jajimenez.workpage;

import java.util.List;
import java.util.LinkedList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.ActionMode;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.graphics.drawable.Drawable;

import jajimenez.workpage.logic.ApplicationLogic;
import jajimenez.workpage.data.model.TaskContext;

public class EditTaskContextsActivity extends AppCompatActivity {
    private ListView listView;
    private ActionMode actionMode;

    private Bundle savedInstanceState;
    private boolean interfaceReady;

    // Broadcast receiver
    private AppBroadcastReceiver appBroadcastReceiver;

    private ApplicationLogic applicationLogic;
    private LoadTaskContextsDBTask contextsDbTask = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_task_contexts);

        Toolbar toolbar = findViewById(R.id.edit_task_contexts_toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.edit_task_contexts_list);
        actionMode = null;

        createContextualActionBar();
        (getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        this.savedInstanceState = savedInstanceState;
        interfaceReady = false;
        applicationLogic = new ApplicationLogic(this);

        updateInterface();

        // Broadcast receiver
        registerBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Broadcast receiver
        unregisterBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {
        appBroadcastReceiver = new AppBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(ApplicationLogic.ACTION_DATA_CHANGED);

        (LocalBroadcastManager.getInstance(this)).registerReceiver(appBroadcastReceiver, intentFilter);
    }

    private void unregisterBroadcastReceiver() {
        (LocalBroadcastManager.getInstance(this)).unregisterReceiver(appBroadcastReceiver);
    }

    private void closeActionBar() {
        // Close the context action bar
        if (actionMode != null) actionMode.finish();
    }

    private void createContextualActionBar() {
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                EditTaskContextsActivity.this.actionMode = mode;

                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.edit_task_contexts_contextual, menu);

                return true;
            }

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
                EditTaskContextsActivity.this.actionMode = null;
            }

            // Returns "true" if this callback handled the event, "false"
            // if the standard "MenuItem" invocation should continue.
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                boolean eventHandled = false;
                final List<TaskContext> selectedContexts = EditTaskContextsActivity.this.getSelectedTaskContexts();

                Bundle arguments;

                switch (item.getItemId()) {
                    case R.id.edit_task_contexts_contextual_menu_edit:
                        // Show an edition dialog
                        EditTaskContextDialogFragment editFragment = new EditTaskContextDialogFragment();

                        arguments = new Bundle();
                        arguments.putLong("context_id", (selectedContexts.get(0)).getId());

                        editFragment.setArguments(arguments);
                        editFragment.show(getFragmentManager(), "edit_task_context");

                        eventHandled = true;
                        break;

                    case R.id.edit_task_contexts_contextual_menu_delete:
                        // Show a deletion confirmation dialog.
                        DeleteTaskContextDialogFragment deleteFragment = new DeleteTaskContextDialogFragment();

                        arguments = new Bundle();
                        int selectedContextCount = selectedContexts.size();
                        long[] selectedContextIds = new long[selectedContextCount];

                        for (int i = 0; i < selectedContextCount; i++) {
                            TaskContext c = selectedContexts.get(i);
                            selectedContextIds[i] = c.getId();
                        }

                        arguments.putLongArray("task_context_ids", selectedContextIds);

                        deleteFragment.setArguments(arguments);
                        deleteFragment.show(getFragmentManager(), "delete_task_context");

                        eventHandled = true;
                        break;
                }

                return eventHandled;
            }

            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                int selectedContextCount = EditTaskContextsActivity.this.listView.getCheckedItemCount();
                if (selectedContextCount > 0) mode.setTitle(EditTaskContextsActivity.this.getString(R.string.selected, selectedContextCount));

                MenuItem editItem = (mode.getMenu()).findItem(R.id.edit_task_contexts_contextual_menu_edit);
                Drawable editItemIcon = editItem.getIcon();

                MenuItem deleteItem = (mode.getMenu()).findItem(R.id.edit_task_contexts_contextual_menu_delete);
                Drawable deleteItemIcon = deleteItem.getIcon();

                int itemCount = EditTaskContextsActivity.this.listView.getCount();

                if (selectedContextCount == 1) {
                    editItem.setEnabled(true);
                    editItemIcon.setAlpha(255);
                }
                else {
                    editItem.setEnabled(false);
                    editItemIcon.setAlpha(127);
                }

                if (selectedContextCount > 0 && selectedContextCount < itemCount) {
                    deleteItem.setEnabled(true);
                    deleteItemIcon.setAlpha(255);
                }
                else {
                    deleteItem.setEnabled(false);
                    deleteItemIcon.setAlpha(127);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        List<Integer> selectedItems = getSelectedItems();
        int selectedItemCount = selectedItems.size();
        int[] selected = new int[selectedItemCount];

        for (int i = 0; i < selectedItemCount; i++) selected[i] = selectedItems.get(i);
        outState.putIntArray("selected_items", selected);

        if (actionMode != null) actionMode.finish();

        super.onSaveInstanceState(outState);
    }

    public void onAddClicked(View view) {
        if (!interfaceReady) return;

        TaskContext newContext = new TaskContext();

        // Show an edition dialog
        EditTaskContextDialogFragment editFragment = new EditTaskContextDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putLong("context_id", newContext.getId());

        editFragment.setArguments(arguments);
        editFragment.show(getFragmentManager(), "edit_task_context");
    }

    private void updateInterface() {
        // Show contexts
        if (contextsDbTask == null || contextsDbTask.getStatus() == AsyncTask.Status.FINISHED) {
            contextsDbTask = new LoadTaskContextsDBTask();
            contextsDbTask.execute();
        }
    }

    private void updateTaskContextListInterface(List<TaskContext> contexts) {
        if (contexts == null) contexts = new LinkedList<>();

        TaskContextAdapter adapter = new TaskContextAdapter(this, R.layout.task_context_list_item, contexts);
        listView.setAdapter(adapter);

        if (savedInstanceState != null) {
            int[] selectedItems = savedInstanceState.getIntArray("selected_items");

            if (selectedItems != null) {
                for (int position : selectedItems) listView.setItemChecked(position, true);
                savedInstanceState.remove("selected_items");
            }
        }
    }

    private List<Integer> getSelectedItems() {
        List<Integer> selectedItems = new LinkedList<>();

        SparseBooleanArray itemSelectedStates = listView.getCheckedItemPositions();
        int itemCount = listView.getCount();

        for (int i = 0; i < itemCount; i++) {
            if (itemSelectedStates.get(i)) {
                // The item with position "i" is selected
                selectedItems.add(i);
            }
        }

        return selectedItems;
    }

    private List<TaskContext> getSelectedTaskContexts() {
        List<TaskContext> selectedContexts = new LinkedList<>();

        TaskContextAdapter adapter = (TaskContextAdapter) listView.getAdapter();
        List<Integer> selectedItems = getSelectedItems();

        for (int position : selectedItems) {
            TaskContext context = adapter.getItem(position);
            selectedContexts.add(context);
        }

        return selectedContexts;
    }

    private class AppBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            EditTaskContextsActivity.this.closeActionBar();
            String action = intent.getAction();

            if (action.equals(ApplicationLogic.ACTION_DATA_CHANGED)) {
                EditTaskContextsActivity.this.updateInterface();
            }
        }
    }

    private class LoadTaskContextsDBTask extends AsyncTask<Void, Void, List<TaskContext>> {
        protected void onPreExecute() {
            EditTaskContextsActivity.this.interfaceReady = false;
            EditTaskContextsActivity.this.listView.setEnabled(false);
        }

        protected List<TaskContext> doInBackground(Void... parameters) {
            return EditTaskContextsActivity.this.applicationLogic.getAllTaskContexts();
        }

        protected void onPostExecute(List<TaskContext> contexts) {
            EditTaskContextsActivity.this.updateTaskContextListInterface(contexts);

            EditTaskContextsActivity.this.listView.setEnabled(true);
            EditTaskContextsActivity.this.interfaceReady = true;
        }
    }
}
