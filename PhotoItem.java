package com.example.photoapp_final;

import android.graphics.Bitmap;

public class PhotoItem {
    private Bitmap image;
    private String tags;
    private String date;
    private String time;

    public PhotoItem(Bitmap image, String tags, String date, String time) {
        this.image = image;
        this.tags = tags;
        this.date = date;
        this.time = time;
    }

    public Bitmap getImage() { return image; }
    public String getTags() { return tags; }
    public String getDate() { return date; }
    public String getTime() { return time; }
}
