package com.example.photoapp_final;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.extensions.android.http.AndroidHttp;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SketchTaggerActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private MyDrawingPlace drawingArea;
    private ListView sketchListView;
    private EditText tagInput;
    private ArrayList<SketchItem> sketchList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivty_mainn);

        drawingArea = findViewById(R.id.drawing_area);
        tagInput = findViewById(R.id.tagInput);
        sketchListView = findViewById(R.id.sketchListView);

        db = openOrCreateDatabase("Sketches.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS sketches (ID INTEGER PRIMARY KEY AUTOINCREMENT, image BLOB, tags TEXT)");

        showSketchList();
    }

    public void saveDrawing(View view) {
        Bitmap bitmap = drawingArea.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        String tags = tagInput.getText().toString();

        ContentValues contentValues = new ContentValues();
        contentValues.put("image", byteArray);
        contentValues.put("tags", tags);

        long result = db.insert("sketches", null, contentValues);
        if (result != -1) {
            Toast.makeText(this, "Sketch saved!", Toast.LENGTH_SHORT).show();
            showSketchList();
        } else {
            Toast.makeText(this, "Error saving sketch.", Toast.LENGTH_SHORT).show();
        }
    }

    public void autoTagSketch(View view) { // new thread to help it work
        Bitmap bitmap = drawingArea.getBitmap();
        new Thread(() -> {
            try {
                List<String> labels = getLabelsFromVisionAPI(bitmap);
                runOnUiThread(() -> {
                    if (labels.isEmpty()) {
                        Toast.makeText(this, "No tags found.", Toast.LENGTH_SHORT).show();
                    } else {
                        tagInput.setText(String.join(", ", labels));
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error with API call: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private List<String> getLabelsFromVisionAPI(Bitmap bitmap) throws IOException {
       // Encode Image
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, bout);
        Image myImage = new Image();
        myImage.encodeContent(bout.toByteArray());
        // Preapre request
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myImage);

        Feature feature = new Feature();
        feature.setType("LABEL_DETECTION");
        feature.setMaxResults(5);
        annotateImageRequest.setFeatures(Collections.singletonList(feature));

        Vision.Builder builder = new Vision.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory.getDefaultInstance(), null);
        builder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyD4LnK3clP07YsfegDbnavzOuaErAD25HM")); // My key
        Vision vision = builder.build();

        // cALL VISION.IMAGES.ANNOTATE
        BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
        batchRequest.setRequests(Collections.singletonList(annotateImageRequest));
        BatchAnnotateImagesResponse response = vision.images().annotate(batchRequest).execute();

        // Extract the actual labels
        List<String> labels = new ArrayList<>();
        if (response.getResponses() != null && !response.getResponses().isEmpty()) {
            for (EntityAnnotation annotation : response.getResponses().get(0).getLabelAnnotations()) {
                labels.add(annotation.getDescription());
            }
        }

        Log.v("SKETCH_TAGS", "Labels found: " + labels);
        return labels;
    }

    public void showSketchList() { // shows the different sketches in order
        Cursor cursor = db.rawQuery("SELECT * FROM sketches ORDER BY ID DESC", null);
        sketchList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("image"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));

                sketchList.add(new SketchItem(imageBytes, tags));
            } while (cursor.moveToNext());
        }
        cursor.close();

        SketchAdapter adapter = new SketchAdapter(this, sketchList);
        sketchListView.setAdapter(adapter);
    }

    public void goBackToOpening(View view) {
        finish();
    }

    public void clearTheDrawing(View view) {
        drawingArea.clearDrawing();
    }

    public void findDrawing(View view) {
        String searchTag = ((EditText) findViewById(R.id.searchTagInput)).getText().toString();
        Cursor cursor;

        if (searchTag.isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM sketches ORDER BY ID DESC", null);
        } else {
            cursor = db.rawQuery("SELECT * FROM sketches WHERE tags LIKE ? ORDER BY ID DESC", new String[]{"%" + searchTag + "%"});
        }

        sketchList.clear();
        if (cursor.moveToFirst()) {
            do {
                byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("image"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));

                sketchList.add(new SketchItem(imageBytes, tags));
            } while (cursor.moveToNext());
        }
        cursor.close();

        SketchAdapter adapter = new SketchAdapter(this, sketchList);
        sketchListView.setAdapter(adapter);
    }
}
