package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract;

public class OverviewActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /** for debugging **/
    public static final String LOG_TAG = OverviewActivity.class.getSimpleName();

    /** Identifier for the product data loader */
    private static final int INVENTORY_LOADER = 0;

    /** Adapter for our cursor adapter */
    InventoryCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        // Adding the add FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(OverviewActivity.this, ProductActivity.class);
                startActivity(intent);
            }
        });
        //Assigning the InventoryCursorAdapter
        ListView list = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        list.setEmptyView(emptyView);

        mCursorAdapter = new InventoryCursorAdapter(this, null);
        list.setAdapter(mCursorAdapter);

        // Setup the item click listener
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(OverviewActivity.this, ProductActivity.class);

                // Form the content URI that represents the specific product that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link productEntry#CONTENT_URI}.
                // For example, the URI would be "content://com.example.android.InventoryApp/InventoryApp/1"
                // if the product with ID 1 was clicked on.
                Uri currentProductUri = ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentProductUri);

                // Launch the {@link EditorActivity} to display the data for the current product.
                startActivity(intent);
            }
        });
        // Start the Loader
        getLoaderManager().initLoader(INVENTORY_LOADER, null, this);

    }

    /**
     * Everything regarding our menu at the overview activity
     *
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_overview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            case R.id.action_about:
                // User clicked the about dialog in the menu
                aboutDialog();
                return true;
            case R.id.action_delete_all_items:
                // User clicked the all item delete in the menu
                deleteDialog();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * The different dialog in our app
     * 1. The about dialog
     * 2. The delete dialog for removing every entry of the database
     */

    private void aboutDialog (){
        // Creating the about dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.about_dialog_message));
        builder.setPositiveButton(getString(R.string.about_dialog_okay_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteDialog (){
        // Creating the dialog for the deleting all of the files
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.delete_dialog_message));
        builder.setPositiveButton(getString(R.string.delete_dialog_okay), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteInventory();
            }
        });
        builder.setNegativeButton(getString(R.string.delete_dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                InventoryContract.InventoryEntry.ID,
                InventoryContract.InventoryEntry.COLUMN_NAME,
                InventoryContract.InventoryEntry.COLUMN_PRICE,
                InventoryContract.InventoryEntry.COLUMN_QUANTITY,
                InventoryContract.InventoryEntry.COLUMN_PHOTO,
                InventoryContract.InventoryEntry.COLUMN_HAS_PHOTO,
                InventoryContract.InventoryEntry.COLUMN_SUPPLIER_PHONENUMBER,
                InventoryContract.InventoryEntry.COLUMN_SUPPLIER_EMAIL
        };
        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,                               // Parent activity context
                InventoryContract.InventoryEntry.CONTENT_URI,       // Provider content URI to query
                projection,                                         // Columns to include in the resulting Cursor
                null,                                               // No selection clause
                null,                                               // No selection arguments
                null);                                              // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link ProductCursorAdapter} with this new cursor containing updated product data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }

    /**
     * Perform the delete operation of the complete inventory in the database.
     */

    private void deleteInventory() {
        // Only perform the delete if this is an existing product.
        if (InventoryContract.InventoryEntry.CONTENT_URI != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(InventoryContract.InventoryEntry.CONTENT_URI, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.delete_everything_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.delete_everything_done),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
