package com.example.photoapp_final;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class CombinedAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<PhotoItem> items;

    public CombinedAdapter(Context context, ArrayList<PhotoItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_combined, parent, false);
        }

        PhotoItem item = (PhotoItem) getItem(position);
        ImageView imageView = convertView.findViewById(R.id.itemImage);
        TextView tagsView = convertView.findViewById(R.id.itemTags);

        // Directly set the Bitmap to the ImageView
        Bitmap bitmap = item.getImage();
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
        }

        // Set the tags text
        tagsView.setText(item.getTags());

        return convertView;
    }
}