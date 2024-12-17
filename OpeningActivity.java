package com.example.photoapp_final;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class OpeningActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opening);
    }

    public void openSketchTagger(View view) {
        Intent intent = new Intent(this, MainActivity.class);  // Opens Photo Tagger Activity
        startActivity(intent);
    }

    public void openPhotoTagger(View view) {
        Intent intent = new Intent(this, SketchTaggerActivity.class);  // Opens Sketch Tagger Activity
        startActivity(intent);
    }

    public void openStoryGenerator(View view) {
        Intent intent = new Intent(this, StoryGenerationActivity.class);
        startActivity(intent);
    }

}
