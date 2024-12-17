package com.example.photoapp_final;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView largeImageView;
    private SQLiteDatabase db;
    private Bitmap capturedPhoto;
    private ListView photoListView;
    private ArrayList<PhotoItem> photoList;
    private EditText tagInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Creates the application

        largeImageView = findViewById(R.id.largeImageView);
        photoListView = findViewById(R.id.photoListView);
        tagInput = findViewById(R.id.tagInput);

        db = openOrCreateDatabase("PhotoApp.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS photos (ID INTEGER PRIMARY KEY AUTOINCREMENT, image BLOB, tags TEXT, date TEXT, time TEXT)");
        // creates the database
        showPhotoList();
    }

    public void startCamera(View view) {
        Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camIntent, 1);
    } // starts the camera

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            capturedPhoto = (Bitmap) extras.get("data");
            largeImageView.setImageBitmap(capturedPhoto);
        }
    }

    public void saveImageWithTags(View view) { //saves the images with the tags below
        if (capturedPhoto == null) {
            Toast.makeText(this, "No photo to save. Please capture a photo first.", Toast.LENGTH_SHORT).show();
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        capturedPhoto.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        String tags = tagInput.getText().toString();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        ContentValues contentValues = new ContentValues();
        contentValues.put("image", byteArray);
        contentValues.put("tags", tags);
        contentValues.put("date", date);
        contentValues.put("time", time);

        long result = db.insert("photos", null, contentValues);
        if (result != -1) {
            Toast.makeText(this, "Photo saved!", Toast.LENGTH_SHORT).show();
            showPhotoList();
        } else {
            Toast.makeText(this, "Error saving photo.", Toast.LENGTH_SHORT).show();
        }
    }

    public void autoTagImage(View view) {
        if (capturedPhoto != null) {
            new Thread(() -> {
                try {
                    List<String> labels = getLabelsFromVisionAPI(capturedPhoto);
                    runOnUiThread(() -> {
                        if (labels.isEmpty()) {
                            Toast.makeText(this, "No tags found.", Toast.LENGTH_SHORT).show();
                        } else {
                            tagInput.setText(String.join(", ", labels));
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Error with API call: " + e.getMessage(), Toast.LENGTH_LONG).show()); // error handling
                }
            }).start();
        } else {
            Toast.makeText(this, "No photo to analyze. Please capture a photo first.", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getLabelsFromVisionAPI(Bitmap bitmap) throws IOException {
        // Step 1: Encode image
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        Image myImage = new Image();
        myImage.encodeContent(bout.toByteArray());

        // Step 2: Prepare AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myImage);

        Feature feature = new Feature();
        feature.setType("LABEL_DETECTION");
        feature.setMaxResults(5);
        annotateImageRequest.setFeatures(Collections.singletonList(feature));

        // Step 3: Build Vision API client
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyD4LnK3clP07YsfegDbnavzOuaErAD25HM")); // Replace with your actual API key
        Vision vision = builder.build();

        // Step 4: Call Vision.Images.Annotate
        BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
        batchRequest.setRequests(Collections.singletonList(annotateImageRequest));
        Vision.Images.Annotate annotate = vision.images().annotate(batchRequest);
        BatchAnnotateImagesResponse response = annotate.execute();

        // Extract labels from response
        List<String> labels = new ArrayList<>();
        if (response.getResponses() != null && !response.getResponses().isEmpty()) {
            for (EntityAnnotation annotation : response.getResponses().get(0).getLabelAnnotations()) {
                labels.add(annotation.getDescription());
            }
        }

        Log.v("MYTAG", "Labels found: " + labels);
        return labels;
    }
    public void findPhoto(View view) {
        String searchTag = ((EditText) findViewById(R.id.searchTagInput)).getText().toString().trim();

        // Check if the search field is empty
        if (searchTag.isEmpty()) {
            Toast.makeText(this, "Please enter a tag to search.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query the database to find photos with matching tags
        Cursor cursor = db.rawQuery("SELECT * FROM photos WHERE tags LIKE ? ORDER BY ID DESC", new String[]{"%" + searchTag + "%"});

        ArrayList<PhotoItem> searchResults = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("image"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));

                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                searchResults.add(new PhotoItem(image, tags, date, time));
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Update the ListView with search results
        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No photos found with the specified tag.", Toast.LENGTH_SHORT).show();
        } else {
            PhotoAdapter adapter = new PhotoAdapter(this, searchResults);
            photoListView.setAdapter(adapter);
        }
    }



    public void showPhotoList() {
        Cursor cursor = db.rawQuery("SELECT * FROM photos ORDER BY ID DESC", null);
        photoList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("image"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));

                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                photoList.add(new PhotoItem(image, tags, date, time));
            } while (cursor.moveToNext());
        }
        cursor.close();

        PhotoAdapter adapter = new PhotoAdapter(this, photoList);
        photoListView.setAdapter(adapter);
    }
}
