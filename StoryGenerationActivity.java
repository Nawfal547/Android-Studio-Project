package com.example.photoapp_final;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StoryGenerationActivity extends AppCompatActivity {

    private EditText searchInput;
    private CheckBox includeSketchesCheckbox;
    private ListView itemListView;
    private Button storyButton, backButton;
    private TextView storyOutput;

    private SQLiteDatabase photoDb, sketchDb;
    private ArrayList<String> selectedTags = new ArrayList<>();
    private final int MAX_SELECTION = 3;
    private final String API_URL = "https://api.textcortex.com/v1/texts/social-media-posts";
    private final String API_KEY = "gAAAAABnNSqItsH82KT1ZCZxPSNMrpS0QE0aDRleYE_cTKZEUeMdUxLKyCh_QI4q2rq5Z30S8-7AMBjzkC2doKIra16m_O_1z61V6up2PDemFKJjR9B1oB9EU5xnPlFER9vIFb_nYpuO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_generation);

        // Initialize UI components
        searchInput = findViewById(R.id.searchInput);
        includeSketchesCheckbox = findViewById(R.id.includeSketchesCheckbox);
        itemListView = findViewById(R.id.itemListView);
        storyButton = findViewById(R.id.storyButton);
        backButton = findViewById(R.id.backButton);
        storyOutput = findViewById(R.id.storyOutput); // Initialize storyOutput here

        // Open databases
        photoDb = openOrCreateDatabase("PhotoApp.db", MODE_PRIVATE, null);
        sketchDb = openOrCreateDatabase("Sketches.db", MODE_PRIVATE, null);

        // Load initial data
        loadItems("");

        itemListView.setOnItemClickListener((parent, view, position, id) -> {
            PhotoItem selectedItem = (PhotoItem) parent.getItemAtPosition(position);
            String selectedTag = selectedItem.getTags();

            if (selectedTags.contains(selectedTag)) {
                // Deselect item
                selectedTags.remove(selectedTag);
                view.setBackgroundColor(0); // Remove highlight
            } else {
                if (selectedTags.size() < MAX_SELECTION) {
                    // Select item
                    selectedTags.add(selectedTag);
                    view.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light)); // Highlight item
                } else {
                    // Prevent selection and notify the user
                    Toast.makeText(this, "You can select up to 3 items.", Toast.LENGTH_SHORT).show();
                    return; // Exit early to prevent selection
                }
            }

            // Update the dynamic message
            updateSelectedMessage();
        });


        // Checkbox listener
        includeSketchesCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String query = searchInput.getText().toString().trim();
            loadItems(query); // Reload items when checkbox state changes
        });

        // Find button logic
        findViewById(R.id.findButton).setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            loadItems(query); // Reload items based on the search query
        });

        // Story button logic
        storyButton.setOnClickListener(v -> {
            if (selectedTags.isEmpty()) {
                Toast.makeText(this, "Please select at least one item.", Toast.LENGTH_SHORT).show();
            } else {
                generateStory();
            }
        });

        // Back button logic
        backButton.setOnClickListener(v -> finish());
    }



    private void loadItems(String query) {
        ArrayList<PhotoItem> items = new ArrayList<>();

        // Query photos from the PhotoApp database
        Cursor photoCursor = photoDb.rawQuery(
                "SELECT image, tags, date, time FROM photos WHERE tags LIKE ?",
                new String[]{"%" + query + "%"}
        );
        while (photoCursor.moveToNext()) {
            byte[] imageBytes = photoCursor.getBlob(photoCursor.getColumnIndexOrThrow("image"));
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            String tags = photoCursor.getString(photoCursor.getColumnIndexOrThrow("tags"));
            String date = photoCursor.getString(photoCursor.getColumnIndexOrThrow("date"));
            String time = photoCursor.getString(photoCursor.getColumnIndexOrThrow("time"));

            items.add(new PhotoItem(bitmap, tags, date, time));
        }
        photoCursor.close();

        // Query sketches from the Sketches database
        if (includeSketchesCheckbox.isChecked()) {
            Cursor sketchCursor = sketchDb.rawQuery(
                    "SELECT image, tags FROM sketches WHERE tags LIKE ?",
                    new String[]{"%" + query + "%"}
            );
            while (sketchCursor.moveToNext()) {
                byte[] imageBytes = sketchCursor.getBlob(sketchCursor.getColumnIndexOrThrow("image"));
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                String tags = sketchCursor.getString(sketchCursor.getColumnIndexOrThrow("tags"));

                items.add(new PhotoItem(bitmap, tags, null, null));
            }
            sketchCursor.close();
        }

        // Update ListView with filtered items
        CombinedAdapter adapter = new CombinedAdapter(this, items);
        itemListView.setAdapter(adapter);
        itemListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    private void updateSelectedMessage() {
        if (selectedTags.isEmpty()) {
            Toast.makeText(this, "No items selected.", Toast.LENGTH_SHORT).show();
        } else {
            String message = "You selected: " + String.join(", ", selectedTags);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }


    private void generateStory() {
        // Check if there are selected tags
        if (selectedTags.isEmpty()) {
            Toast.makeText(this, "Please select at least one item.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Prepare the JSON data for the API request
            JSONObject data = new JSONObject();
            data.put("context", "Story based on selected tags");
            data.put("keywords", new JSONArray(selectedTags)); // Add selected tags
            data.put("formality", "default");
            data.put("max_tokens", 2048);
            data.put("mode", "twitter");
            data.put("model", "claude-3-haiku");

            // Log the request payload for debugging
            Log.d("StoryAPI Request", data.toString());
            Log.d("SelectedTags", selectedTags.toString());

            // Create the API request
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, API_URL, data,
                    response -> {
                        try {
                            // Extract and display the story from the API response
                            Log.d("StoryAPI Response", response.toString());
                            String story = response.getJSONObject("data").getJSONArray("outputs").getJSONObject(0).getString("text");
                            storyOutput.setText(story); // Display story in the TextView
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error parsing the response.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        // Handle errors in the API request
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            String responseBody = new String(error.networkResponse.data);
                            Log.e("StoryAPI Error", "Status Code: " + statusCode + " Response: " + responseBody);
                            Toast.makeText(this, "Error generating story. Status Code: " + statusCode, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("StoryAPI Error", "Network error: " + error.toString());
                            Toast.makeText(this, "Error generating story. Please check your network.", Toast.LENGTH_SHORT).show();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    // Add API headers
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "Bearer " + API_KEY); // Replace with your actual API key
                    return headers;
                }
            };

            // Add the request to the Volley queue
            Volley.newRequestQueue(this).add(request);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparing the request.", Toast.LENGTH_SHORT).show();
        }
    }
}