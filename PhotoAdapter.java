package com.example.photoapp_final;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class PhotoAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<PhotoItem> photos;

    public PhotoAdapter(Context context, ArrayList<PhotoItem> photos) {
        this.context = context;
        this.photos = photos;
    }

    @Override
    public int getCount() { return photos.size(); }

    @Override
    public Object getItem(int position) { return photos.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.photo_item, parent, false);
        }

        PhotoItem photoItem = (PhotoItem) getItem(position);
        ImageView imageView = convertView.findViewById(R.id.imageView);
        TextView tagsView = convertView.findViewById(R.id.tagsView);
        TextView dateView = convertView.findViewById(R.id.dateView);
        TextView timeView = convertView.findViewById(R.id.timeView);

        imageView.setImageBitmap(photoItem.getImage());
        tagsView.setText("Tags: " + photoItem.getTags());
        dateView.setText("Date: " + photoItem.getDate());
        timeView.setText("Time: " + photoItem.getTime());

        return convertView;
    }
}
