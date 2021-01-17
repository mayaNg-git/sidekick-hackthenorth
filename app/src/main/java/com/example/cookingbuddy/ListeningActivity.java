package com.example.cookingbuddy;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Build;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.util.Log;

import android.widget.EditText;

import android.widget.Toast;

import org.json.JSONObject;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;



public class ListeningActivity extends AppCompatActivity {
    // Constant for Speech Recognizer Audio Code
    public static final Integer RecordAudioRequestCode = 1;

    // api
    private OkHttpClient httpClient;
    private HttpUrl.Builder httpBuilder;
    private final String CLIENT_ACCESS_TOKEN = "S4GLMSX2XUCQHWCFRLUC6VZHKDON7GBX";
    private String query;
    private String last_query;
    private String last_answer;


    // Keeping track of recipe representations
    private List<Recipe> recipes;
    private int instrCounter;
    private Recipe currRecipe;

    // Text to Speech
    private TextToSpeech t1;

    // Understanding phones audio -> text query
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    // Reset button if breaks
    private Button button;

    @SuppressLint("ClickableViewAccessibility")


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listening);

        // Ask for Permissions
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        // Initialize some 1 time fields
        button = findViewById(R.id.test_btn);
        button.setOnClickListener(v->initializeSpeechRecognizer());
        last_answer = null;

        // Initialize the Rest of the private fields / states
        initializeFields();


    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
        Log.d("stopped","stopped");
    }

    private void initializeFields() {
        last_query = "";

        // Initialize Speech Recognizer
        initializeSpeechRecognizer();

        // Initialize API Call
        httpClient = new OkHttpClient();
        httpBuilder = HttpUrl.parse("https://api.wit.ai/message").newBuilder();

        // Initialize TextToSpeech
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        // Initialize Local Copy of Recipes
        recipes = new ArrayList<Recipe>();
        fillRecipe();
        instrCounter = 0;
        currRecipe = recipes.get(0);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeSpeechRecognizer() {
        query = "";
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Listener listener = new Listener();
        speechRecognizer.setRecognitionListener(listener);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        Log.d("SR", ""+ SpeechRecognizer.isRecognitionAvailable(this));
        Log.d("Starting", "Starting");
        speechRecognizer.startListening(speechRecognizerIntent);
//        button.setOnTouchListener(new View.OnTouchListener() {
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    speechRecognizer.stopListening();
//                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    Log.d("Down", "down");
//                    speechRecognizer.startListening(speechRecognizerIntent);
//                }
//                return false;
//            }
//        });
    }
    private class Listener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d("Listener", "Ready");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d("SpeechRecognition","Resetting Query");
            query = "";
        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int i) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            initializeSpeechRecognizer();
        }

        @Override
        public void onResults(Bundle bundle) {
            ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            query = data.get(0);

            if (query != null && query != "" && !query.equals(last_query) && !query.equals(last_answer)) {
                GetRequestRunnable runnable = new GetRequestRunnable();
                runnable.setQuery(query);
                last_query = query;
                query = null;
                Log.d("QUERY", "RESET");
                Thread myThread = new Thread(runnable);
                myThread.start();
            }
            speechRecognizer.stopListening();
            speechRecognizer.destroy();

            initializeSpeechRecognizer();
        }

        @Override
        public void onPartialResults(Bundle bundle) {

        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    }

    private class GetRequestRunnable implements Runnable {
        private String query;

        public void setQuery(String query) {
            this.query = query;
        }

        @Override
        public void run() {
            httpBuilder.removeAllQueryParameters("q");
            httpBuilder.addQueryParameter("v", "20200513");
            httpBuilder.addQueryParameter("q", this.query);
            HttpUrl url = httpBuilder.build();

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + CLIENT_ACCESS_TOKEN)
                    .build();

            try {
                Response response = httpClient.newCall(request).execute();
                String jsonData = response.body().string();
                JSONObject Jobject = new JSONObject(jsonData);
                Log.d("Response", Jobject.toString());
                String intent = Jobject.getJSONArray("intents").getJSONObject(0).getString("name");

                Log.d("Intent", Jobject.getJSONArray("intents").getJSONObject(0).toString());
                Log.d("Intent", intent);
                switch(intent) {
                    case "get_ingredient":
                        String ingredient = Jobject.getJSONObject("entities")
                                .getJSONArray("ingredient:ingredient")
                                .getJSONObject(0)
                                .getString("body");
                        Log.d("Ingredient", ingredient);
                        if (currRecipe.ingredients.containsKey(ingredient)) {
                            String message = String.format("Recipe calls for %s", currRecipe.ingredients.get(ingredient));
                            last_answer = message;
                            Log.d("output",""+t1.speak(message, TextToSpeech.QUEUE_FLUSH, null, message));
                        } else {
                            String message = String.format("No %s found in recipe", ingredient);
                            last_answer = message;
                            t1.speak(message, TextToSpeech.QUEUE_FLUSH, null, "");
                        }

                        break;
                    case "go_back":
                        if (instrCounter == 0) {
                            String message = currRecipe.directions.get(0);
                            last_answer = message;
                            t1.speak(message, TextToSpeech.QUEUE_FLUSH, null, "");
                        } else {
                            String message = currRecipe.directions.get(instrCounter - 1);
                            last_answer = message;
                            t1.speak(message, TextToSpeech.QUEUE_FLUSH, null, "");
                        }
                        break;
                    case "next_step":
                        if (instrCounter == currRecipe.directions.size() - 1) {
                            String message = currRecipe.directions.get(instrCounter);
                            last_answer = message;
                            t1.speak(message, TextToSpeech.QUEUE_FLUSH, null, "");
                        } else {
                            String message = currRecipe.directions.get(++instrCounter);
                            last_answer = message;
                            t1.speak(message, TextToSpeech.QUEUE_FLUSH, null, "");
                        }
                        break;
                    case "repeat_step":
                        String message = currRecipe.directions.get(instrCounter);
                        last_answer = message;
                        t1.speak(message, TextToSpeech.QUEUE_FLUSH, null, "");
                        break;
                    default:
                        message = "I did not understand what you asked for";
                        last_answer = message;
                        t1.speak(message, TextToSpeech.QUEUE_FLUSH, null, "");
                }
            } catch (Exception e) {
                Log.d("Response", "FAILED");
            }
        }
    }

    private class Recipe {
        public String name;
        public Map<String, String> ingredients;
        public List<String> directions;

        public Recipe(String name) {
            this.name = name;
            this.ingredients = new HashMap<String, String>();
            this.directions = new ArrayList<String>();
        }

        public void addIngredient(String ingredient, String quantity) {
            ingredients.put(ingredient, quantity);
        }

        public void addDirection(String direction) {
            directions.add(direction);
        }

        public String toString() {
            return this.name;
        }
    }

    private void fillRecipe() {
        Recipe r1 = new Recipe("Grandma's Chocolate Chip Cookies");
        r1.addIngredient("butter", "1 cup salted butter");
        r1.addIngredient("white sugar", "1 cup white granulated sugar");
        r1.addIngredient("brown sugar", "1 cup light brown sugar");
        r1.addIngredient("vanilla extract", "2 teaspoons pure vanilla extract");
        r1.addIngredient("eggs", "2 large eggs");
        r1.addIngredient("flour", "3 cups all purpose flour");
        r1.addIngredient("baking soda", "1 teaspoon baking soda");
        r1.addIngredient("baking powder", "half teaspoon baking powder");
        r1.addIngredient("salt", "1 teaspoon sea salt");
        r1.addIngredient("chocolate chips", "2 cups chocolate chips");

        r1.addDirection("Preheat oven to 375 degrees F and line baking apn with parchment paper");
        r1.addDirection("Mix flour, baking soda, salt, baking powder in a bowl and set aside");
        r1.addDirection("Cream together butter and sugars until combined");
        r1.addDirection("Beat in eggs and vanilla until fluffy");
        r1.addDirection("Mix in the dry ingredients until combined");
        r1.addDirection("Add 12 oz pacakge of chocolate chips and mix well");
        r1.addDirection("Roll cookie dough into balls and place them evenly spaced on your cookie sheets");
        r1.addDirection("Bake in preheated oven for approximately 8-10 minutes");

        recipes.add(r1);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }
}

