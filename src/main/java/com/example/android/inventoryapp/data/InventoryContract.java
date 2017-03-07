package com.example.android.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by bjoern on 28.02.17.
 *
 * @author <a href="mailto:mail@bjoern.cologne">Bjoern Gam</a>
 * @link <a href="http://bjoern.cologne">Webpage </a>
 * <p>
 * Description: The contract for our database
 * contains: The layout of the CONTENT_URI
 *           the databaselayout
 *           the magic for our MIME types
 */

public class InventoryContract {

    public static final String LOG_TAG = InventoryContract.class.getSimpleName();

    // With the default constructor set as private it's not possible
    // to create an object of the class
    private InventoryContract() {}

    // Here we are setting up the default content_authority uri
    public static final String CONTENT_AUTHORITY = "com.example.android.InventoryApp";

    //Here we are build the complete uri int two steps
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_PRODUCT = "InventoryApp";

    public static final class InventoryEntry implements BaseColumns {

        // the complete content_uri for getting access to the database
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCT);

        public static final String TABLE_NAME = "products";                              // the name of the table inside the database

        // Here is the definition of our database layout;
        public static final String ID = BaseColumns._ID;                                // the unique id of the entry
        public static final String COLUMN_NAME = "name";                                // the name of the entry
        public static final String COLUMN_PRICE = "price";                              // the price of the entry
        public static final String COLUMN_QUANTITY = "quantity";                        // the quantity of the entry
        public static final String COLUMN_PHOTO = "photo";                              // here we store the photo of our product
        public static final String COLUMN_HAS_PHOTO = "hasphoto";                       // has the product a image
        public static final String COLUMN_SUPPLIER_PHONENUMBER = "phonenumber";         // here we store the phone number of the supplier
        public static final String COLUMN_SUPPLIER_EMAIL = "email";                     // here we store the mail address of the supplier

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of ware.
         */
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCT;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single pet.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCT;

    }
}
