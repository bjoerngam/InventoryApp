package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.inventoryapp.data.InventoryContract;

import static com.example.android.inventoryapp.R.id.productName;
import static com.example.android.inventoryapp.R.id.productPrice;
import static com.example.android.inventoryapp.R.id.productQuantity;

/**
 * Created by bjoern on 28.02.17.
 *
 * @author <a href="mailto:mail@bjoern.cologne">Bjoern Gam</a>
 * @link <a href="http://bjoern.cologne">Webpage </a>
 * <p>
 * Description:
 */
public class InventoryCursorAdapter extends CursorAdapter
{
    /** For debugging */
    public static final String LOG_TAG = InventoryCursorAdapter.class.getSimpleName();

    private TextView productNameView;
    private TextView productPriceView;
    private TextView productQuantityView;
    private ImageView productPhotoView;
    private Button btSell;

    /***
     * The default constructor
     * @param context
     * @param c
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Find the different views at our list_item layout
        productNameView = (TextView) view.findViewById(productName);
        productPriceView = (TextView) view.findViewById(productPrice);
        productQuantityView = (TextView) view.findViewById(productQuantity);
        productPhotoView = (ImageView) view.findViewById(R.id.imageBig);
        btSell = (Button) view.findViewById(R.id.sellListButton);

        //By using the current position of the cursor we're getting the content of the row
        int productNameIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_NAME);
        int productPriceIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_PRICE);
        int productQuantityIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_QUANTITY);
        int productImageIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_PHOTO);
        int hasPhotoIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_HAS_PHOTO);

        //Assigning the Strings
        String pName = cursor.getString(productNameIndex);
        int pPrice = cursor.getInt(productPriceIndex);
        final int pQuantity = cursor.getInt(productQuantityIndex);
        int photoSet = cursor.getInt(hasPhotoIndex);
        int idColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry._ID);
        final long id = cursor.getLong(idColumnIndex);


        // Selling direct the products
        btSell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (pQuantity > 0) {

                    ContentValues values = new ContentValues();
                    int oldQuantity = pQuantity;
                    int pQuantity = oldQuantity - 1;

                    Log.v("new quantity", "after click" + pQuantity);

                    values.put(InventoryContract.InventoryEntry.COLUMN_QUANTITY, pQuantity);

                    Uri uri = ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI, id);
                    view.getContext().getContentResolver().update(uri, values, null, null);

                    view.getContext().getContentResolver().notifyChange(InventoryContract.InventoryEntry.CONTENT_URI, null);

                }
            }
        });

        // The photo magic
        if (photoSet == 1) {
            byte[] photoByte = cursor.getBlob(productImageIndex);
            Bitmap photoBitmap = BitmapFactory.decodeByteArray(photoByte, 0, photoByte.length);
            productPhotoView.setImageBitmap(photoBitmap);
        }

        productNameView.setText(pName);
        productQuantityView.setText("Quantity: " +cursor.getString(productQuantityIndex));
        productPriceView.setText("Price: " +pPrice + " €");
    }
}
