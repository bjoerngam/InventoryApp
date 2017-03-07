package com.example.android.inventoryapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ProductActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /** For a proper debugging */
    public static final String LOG_TAG = ProductActivity.class.getSimpleName();

    /** Identifier for the product data loader */
    private static final int PRODUCT_LOADER = 0;

    /** Content URI for the existing product (null if it's a new product) */
    private Uri mCurrentProductURI;

    /** Find the different GUI elements **/
    private EditText mProductName;

    private EditText mProductPrice;

    private EditText mProductQuantity;

    private EditText mSupplierPhoneNumber;

    private EditText mSupplierMailAddress;

    private ImageView mProductImage;

    /** For the phone call */
    public static final int MY_PERMISSIONS_REQUEST_PHONE_CALL = 1;

    /** Boolean flag that keeps track of whether the product has been edited (true) or not (false) */
    private boolean mProductHasChanged = false;

    private int mhasPhoto = 0;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductURI = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductURI == null) {
            // This is a new product, so change the app bar to say "Add a product"
            setTitle(getString(R.string.add_a_new_product));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();

        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit product"
            setTitle(getString(R.string.edit_a_product));

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
        }

        mProductImage = (ImageView) findViewById(R.id.imageBig);
        mProductName = (EditText) findViewById(R.id.productName_detail);
        mProductPrice = (EditText) findViewById(R.id.productPrice_detail);
        mProductQuantity = (EditText) findViewById(R.id.productQuantity_detail);
        mSupplierMailAddress = (EditText) findViewById(R.id.supplierMailAddress);
        mSupplierPhoneNumber = (EditText) findViewById(R.id.supplierPhoneNumber);

        // Adding the sell button
        Button btSell = (Button) findViewById(R.id.btSell);
        btSell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sellProduct();
            }
        });

        // Adding the add quantity button
        Button btAdd = (Button) findViewById(R.id.addQuantityButton);
        btAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addQuantity();
            }
        });

        //Adding the add photo button
        Button btAddPhoto = (Button) findViewById(R.id.btPhoto);
        btAddPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            addPhoto();
            }
        });

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mProductImage.setOnTouchListener(mTouchListener);
        mProductName.setOnTouchListener(mTouchListener);
        mProductPrice.setOnTouchListener(mTouchListener);
        mProductQuantity.setOnTouchListener(mTouchListener);
        mSupplierMailAddress.setOnTouchListener(mTouchListener);
        mSupplierPhoneNumber.setOnTouchListener(mTouchListener);
    }

    /** Everything around our menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_product, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductURI == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            case R.id.action_save:
                // Response to a click on the "save" menu entry
                saveProducts();
                // Back to the overview screen
                finish();
                return true;
                // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(ProductActivity.this);
                    return true;
                }
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(ProductActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
                 // Response to a click on the "delete" menu entry
            case R.id.action_delete:
                deleteSingleProductDialog();
                return true;
                // Response to a click on the "call" menu entry
            case R.id.action_order_mail:
                sendMail();
                return true;
                //Response to a click on the "phone" menu entry
            case R.id.action_order_phone:
                makePhoneCall();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /** Menu end **/

    public void saveProducts() {

        byte[] imageByteArray;
        int price_int;
        int quantity_int;
        ContentValues values = new ContentValues();

        String name = mProductName.getText().toString().trim();
        String price = mProductPrice.getText().toString().trim();
        String quantity = mProductQuantity.getText().toString().trim();
        String phoneNumber = mSupplierPhoneNumber.getText().toString().trim();
        String mailAddress = mSupplierMailAddress.getText().toString().trim();

        if (mCurrentProductURI == null && TextUtils.isEmpty(name) && TextUtils.isEmpty(price)
                && TextUtils.isEmpty(quantity) && TextUtils.isEmpty(phoneNumber)
                && TextUtils.isEmpty(mailAddress)){
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

        // Everything around our picture
        if (mhasPhoto == 1){
            if (null!=mProductImage.getDrawable()) {
                Bitmap imageBitMap = ((BitmapDrawable) mProductImage.getDrawable()).getBitmap();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                imageBitMap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                imageByteArray = outputStream.toByteArray();
                values.put(InventoryContract.InventoryEntry.COLUMN_PHOTO, imageByteArray);
            }
        }

        // Convert the string values into integer
        if (TextUtils.isEmpty(price)){
            price_int = 0;
        }else{ price_int = Integer.parseInt(price);}
        if (TextUtils.isEmpty(quantity)) {
            quantity_int = 0;
        }else{ quantity_int = Integer.parseInt(quantity);}

        // Setting string placeholder
        if (TextUtils.isEmpty(name)){
            name = "not set";
        }
        if (TextUtils.isEmpty(phoneNumber)){
            phoneNumber = "not set";
        }

        if (TextUtils.isEmpty(mailAddress)){
            mailAddress = "not set";
        }

        values.put(InventoryContract.InventoryEntry.COLUMN_NAME, name);
        values.put(InventoryContract.InventoryEntry.COLUMN_PRICE, price_int);
        values.put(InventoryContract.InventoryEntry.COLUMN_QUANTITY, quantity_int);
        values.put(InventoryContract.InventoryEntry.COLUMN_SUPPLIER_PHONENUMBER, phoneNumber);
        values.put(InventoryContract.InventoryEntry.COLUMN_SUPPLIER_EMAIL, mailAddress);
        values.put(InventoryContract.InventoryEntry.COLUMN_HAS_PHOTO, mhasPhoto);

        if (mCurrentProductURI == null){

            Uri newUri = getContentResolver().insert(InventoryContract.InventoryEntry.CONTENT_URI, values);
            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.insert_product_cannot),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.insert_product),
                        Toast.LENGTH_SHORT).show();
            }
        }else {
            // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentProductUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentProductUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentProductURI, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, "Cannot update product.",
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, "Update product.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && resultCode == RESULT_OK && null != data) { // we have bitmap from filesystem!
            Uri selectedImage = data.getData();

            InputStream inputStream = null;

            if (ContentResolver.SCHEME_CONTENT.equals(selectedImage.getScheme())) {
                try {
                    inputStream = this.getContentResolver().openInputStream(selectedImage);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                if (ContentResolver.SCHEME_FILE.equals(selectedImage.getScheme())) {
                    try {
                        inputStream = new FileInputStream(selectedImage.getPath());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            mhasPhoto = 1;
            //Display the photo
            mProductImage.setImageBitmap(bitmap);
        }
    }

    //The photo select dialog
    public void addPhoto() {
        Intent intent =  new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(intent, 0);
    }

    // Selling a product by using the update method
    public void sellProduct() {

        String quantity = mProductQuantity.getText().toString().trim();

        if (mCurrentProductURI == null ) {
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }
        // Convert the string values into integer
        int quantity_int = Integer.parseInt(quantity);

        if (quantity_int <= 0){
            Toast.makeText(this, getString(R.string.stock_error),Toast.LENGTH_LONG).show();
        }

        ContentValues values = new ContentValues();
        values.put(InventoryContract.InventoryEntry.COLUMN_QUANTITY, quantity_int - 1);

        // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentProductUri
        // and pass in the new ContentValues. Pass in null for the selection and selection args
        // because mCurrentProductUri will already identify the correct row in the database that
        // we want to modify.
        int rowsAffected = getContentResolver().update(mCurrentProductURI, values, null, null);

        // Show a toast message depending on whether or not the update was successful.
        if (rowsAffected == 0) {
            // If no rows were affected, then there was an error with the update.
            Toast.makeText(this, getString(R.string.sell_error),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.sell_okay),
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Selling a product by using the update method
    public void addQuantity() {

        String quantity = mProductQuantity.getText().toString().trim();
        if (mCurrentProductURI == null) {
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }
        // Convert the string values into integer
        int quantity_int = Integer.parseInt(quantity);
        if (quantity_int <= 0){
            Toast.makeText(this, getString(R.string.stock_error), Toast.LENGTH_LONG).show();
        }
        ContentValues values = new ContentValues();
        values.put(InventoryContract.InventoryEntry.COLUMN_QUANTITY, quantity_int + 1);
        // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentProductUri
        // and pass in the new ContentValues. Pass in null for the selection and selection args
        // because mCurrentProductUri will already identify the correct row in the database that
        // we want to modify.
        int rowsAffected = getContentResolver().update(mCurrentProductURI, values, null, null);
        // Show a toast message depending on whether or not the update was successful.
        if (rowsAffected == 0) {
            // If no rows were affected, then there was an error with the update.
            Toast.makeText(this, getString(R.string.add_error),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.add_okay),
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Sending the mail
    public void sendMail(){
        if (mSupplierMailAddress.getText().length() < 0){
            Toast.makeText(this, getString(R.string.mail_not_set), Toast.LENGTH_LONG).show();
        } else {
            String mailAddress = mSupplierMailAddress.getText().toString();
            String productName = mProductName.getText().toString();
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mailAddress});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Order of " + productName);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        }
    }

    // Here we are doing the phone call
    public void makePhoneCall(){
        String phonenumber = mSupplierPhoneNumber.getText().toString();
        if (phonenumber.length() < 0){
            Toast.makeText(this, getString(R.string.phone_error), Toast.LENGTH_LONG).show();
        }else {
            String complete_phoneNumber = "tel:" + phonenumber;
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse(complete_phoneNumber));

            if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // Thanks to API 23 we have to add a new permission dialog
                requestPermission(Manifest.permission.CALL_PHONE, MY_PERMISSIONS_REQUEST_PHONE_CALL);
                return;
            }
            startActivity(callIntent);
        }
    }

    // Here we need to deal with permissions for our phone call
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[] , int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_PHONE_CALL:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(ProductActivity.this, getString(R.string.permission_set), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProductActivity.this, getString(R.string.permission_notset), Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permissionName}, permissionRequestCode);
    }

    /**
     * Perform the deletion of the product in the database.
     */

    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductURI != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductURI
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductURI, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.delete_single_product_failure_message),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.delete_single_product_success_message),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */

    private void deleteSingleProductDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_single_product);
        builder.setPositiveButton(R.string.delete_single_product_okay, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.delete_single_product_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_dialog);
        builder.setPositiveButton(R.string.unsaved_dialog_discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.unsaved_dialog_keep, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                InventoryContract.InventoryEntry.ID,
                InventoryContract.InventoryEntry.COLUMN_NAME,
                InventoryContract.InventoryEntry.COLUMN_PRICE,
                InventoryContract.InventoryEntry.COLUMN_QUANTITY,
                InventoryContract.InventoryEntry.COLUMN_PHOTO,
                InventoryContract.InventoryEntry.COLUMN_HAS_PHOTO,
                InventoryContract.InventoryEntry.COLUMN_SUPPLIER_PHONENUMBER,
                InventoryContract.InventoryEntry.COLUMN_SUPPLIER_EMAIL};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,                               // Parent activity context
                mCurrentProductURI,                                // Query the content URI for the current product
                projection,                                       // Columns to include in the resulting Cursor
                null,                                            // No selection clause
                null,                                           // No selection arguments
                null);                                         // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }
        if (cursor.moveToFirst()) {
            // We're getting the proper index values for our different columns
            int productNameIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_NAME);
            int productPriceIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_PRICE);
            int productQuantityIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_QUANTITY);
            int photoIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_PHOTO);
            int supplierPhoneNumber = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_SUPPLIER_PHONENUMBER);
            int supplierMailAddress = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_SUPPLIER_EMAIL);
            int hasPhoto = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_HAS_PHOTO);

            //Getting the values
            String name = cursor.getString(productNameIndex);
            String price = cursor.getString(productPriceIndex);
            String quantity = cursor.getString(productQuantityIndex);
            String phonenumber = cursor.getString(supplierPhoneNumber);
            String mailaddress = cursor.getString(supplierMailAddress);
            int photoSet = cursor.getInt(hasPhoto);

            if (photoSet == 1){
                // The photo magic
                byte[] photoByte = cursor.getBlob(photoIndex);
                Bitmap photoBitmap = BitmapFactory.decodeByteArray(photoByte, 0, photoByte.length);
                mProductImage.setImageBitmap(photoBitmap);
            }

            mProductName.setText(name);
            mProductPrice.setText(price);
            mProductQuantity.setText(quantity);
            mSupplierPhoneNumber.setText(phonenumber);
            mSupplierMailAddress.setText(mailaddress);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mProductName.setText("");
        mProductPrice.setText("");
        mProductQuantity.setText("");
        mSupplierMailAddress.setText("");
        mSupplierPhoneNumber.setText("");
    }
}
