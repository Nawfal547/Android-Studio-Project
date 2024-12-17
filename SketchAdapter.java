package com.example.photoapp_final;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class SketchAdapter extends ArrayAdapter<SketchItem> {

    public SketchAdapter(Context context, ArrayList<SketchItem> sketches) {
        super(context, 0, sketches);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_sketch, parent, false);
        }

        SketchItem sketch = getItem(position);

        ImageView imageView = convertView.findViewById(R.id.sketchImageView);
        TextView metadataText = convertView.findViewById(R.id.sketchMetadata);

        Bitmap bitmap = BitmapFactory.decodeByteArray(sketch.getImage(), 0, sketch.getImage().length);
        imageView.setImageBitmap(bitmap);

        metadataText.setText(sketch.getMetadata());

        return convertView;
    }
}