package jajimenez.workpage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import jajimenez.workpage.data.model.TaskContext;
import jajimenez.workpage.data.model.TaskTag;
import jajimenez.workpage.logic.ApplicationLogic;

public class EditTaskTagsActivity extends AppCompatActivity {
    private ListView listView;
    private TextView emptyTextView;
    private ActionMode actionMode;

    private Bundle savedInstanceState;
    private boolean interfaceReady;

    // Broadcast receiver
    private AppBroadcastReceiver appBroadcastReceiver;

    private LoadTaskTagsDBTask tagsDbTask = null;

    private ApplicationLogic applicationLogic;
    private TaskContext currentTaskContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_task_tags);
        
        Toolbar toolbar = findViewById(R.id.edit_task_tags_toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.edit_task_tags_list);
        emptyTextView = findViewById(R.id.edit_task_tags_empty);
        actionMode = null;

        createContextualActionBar();
        interfaceReady = false;

        applicationLogic = new ApplicationLogic(this);
        currentTaskContext = applicationLogic.getCurrentTaskContext();

        ActionBar bar = getSupportActionBar();

        if (bar != null) {
            bar.setSubtitle(currentTaskContext.getName());
            bar.setDisplayHomeAsUpEnabled(true);
        }

        this.savedInstanceState = savedInstanceState;
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
                EditTaskTagsActivity.this.actionMode = mode;

                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.edit_task_tags_contextual, menu);

                return true;
            }

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
                EditTaskTagsActivity.this.actionMode = null;
            }

            // Returns "true" if this callback handled the event, "false"
            // if the standard "MenuItem" invocation should continue.
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                boolean eventHandled = false;
                final List<TaskTag> selectedTags = EditTaskTagsActivity.this.getSelectedTaskTags();

                Bundle arguments = new Bundle();

                switch (item.getItemId()) {
                    case R.id.editTaskTagsContextualMenu_edit:
                        // Show an edition dialog.
                        EditTaskTagDialogFragment editFragment = new EditTaskTagDialogFragment();
                        long selectedTagId = (selectedTags.get(0)).getId();

                        arguments.putLong("tag_id", selectedTagId);
                        arguments.putLong("context_id", currentTaskContext.getId());

                        editFragment.setArguments(arguments);
                        editFragment.show(getFragmentManager(), "edit_task_tag");

                        eventHandled = true;
                        break;

                    case R.id.editTaskTagsContextualMenu_delete:
                        // Show a deletion confirmation dialog.
                        DeleteTaskTagDialogFragment deleteFragment = new DeleteTaskTagDialogFragment();

                        int selectedTagCount = selectedTags.size();
                        long[] selectedTagIds = new long[selectedTagCount];

                        for (int i = 0; i < selectedTagCount; i++) {
                            TaskTag t = selectedTags.get(i);
                            selectedTagIds[i] = t.getId();
                        }

                        arguments.putLongArray("tag_ids", selectedTagIds);

                        deleteFragment.setArguments(arguments);
                        deleteFragment.show(getFragmentManager(), "delete_task_tag");

                        eventHandled = true;
                        break;
                }

                return eventHandled;
            }

            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                int selectedTagCount = EditTaskTagsActivity.this.listView.getCheckedItemCount();
                if (selectedTagCount > 0) mode.setTitle(EditTaskTagsActivity.this.getString(R.string.selected, selectedTagCount));

                MenuItem editItem = (mode.getMenu()).findItem(R.id.editTaskTagsContextualMenu_edit);
                Drawable editItemIcon = editItem.getIcon();

                MenuItem deleteItem = (mode.getMenu()).findItem(R.id.editTaskTagsContextualMenu_delete);
                Drawable deleteItemIcon = deleteItem.getIcon();

                if (selectedTagCount == 1) {
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

        // Show an edition dialog
        EditTaskTagDialogFragment editFragment = new EditTaskTagDialogFragment();
        Bundle arguments = new Bundle();

        long tagId = -1; // New tag

        arguments.putLong("tag_id", tagId);
        arguments.putLong("context_id", currentTaskContext.getId());

        editFragment.setArguments(arguments);
        editFragment.show(getFragmentManager(), "edit_task_tag");
    }

    private void updateInterface() {
        // Show tags
        if (tagsDbTask == null || tagsDbTask.getStatus() == AsyncTask.Status.FINISHED) {
            tagsDbTask = new LoadTaskTagsDBTask();
            tagsDbTask.execute();
        }
    }

    private void updateTaskTagListInterface(List<TaskTag> tags) {
        if (tags == null) tags = new LinkedList<>();

        TaskTagAdapter adapter = new TaskTagAdapter(this, R.layout.task_tag_list_item, tags);
        listView.setAdapter(adapter);

        if (adapter.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
        }
        else {
            if (savedInstanceState != null) {
                int[] selectedItems = savedInstanceState.getIntArray("selected_items");

                if (selectedItems != null) {
                    for (int position : selectedItems) listView.setItemChecked(position, true);
                    savedInstanceState.remove("selected_items");
                }
            }

            listView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }

    private List<Integer> getSelectedItems() {
        List<Integer> selectedItems = new LinkedList<>();

        SparseBooleanArray itemSelectedStates = listView.getCheckedItemPositions();
        int itemCount = listView.getCount();

        for (int i = 0; i < itemCount; i++) {
            if (itemSelectedStates.get(i)) {
                // The item with position "i" is selected.
                selectedItems.add(i);
            }
        }

        return selectedItems;
    }

    private List<TaskTag> getSelectedTaskTags() {
        List<TaskTag> selectedTags = new LinkedList<>();

        TaskTagAdapter adapter = (TaskTagAdapter) listView.getAdapter();
        List<Integer> selectedItems = getSelectedItems();

        for (int position : selectedItems) {
            TaskTag tag = adapter.getItem(position);
            selectedTags.add(tag);
        }

        return selectedTags;
    }

    private class AppBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            EditTaskTagsActivity.this.closeActionBar();
            String action = intent.getAction();

            if (action.equals(ApplicationLogic.ACTION_DATA_CHANGED)) {
                EditTaskTagsActivity.this.updateInterface();
            }
        }
    }

    private class LoadTaskTagsDBTask extends AsyncTask<Void, Void, List<TaskTag>> {
        protected void onPreExecute() {
            EditTaskTagsActivity.this.interfaceReady = false;
            EditTaskTagsActivity.this.listView.setEnabled(false);
        }

        protected List<TaskTag> doInBackground(Void... parameters) {
            return EditTaskTagsActivity.this.applicationLogic.getAllTaskTags(EditTaskTagsActivity.this.currentTaskContext);
        }

        protected void onPostExecute(List<TaskTag> tags) {
            EditTaskTagsActivity.this.updateTaskTagListInterface(tags);

            EditTaskTagsActivity.this.listView.setEnabled(true);
            EditTaskTagsActivity.this.interfaceReady = true;
        }
    }
}
