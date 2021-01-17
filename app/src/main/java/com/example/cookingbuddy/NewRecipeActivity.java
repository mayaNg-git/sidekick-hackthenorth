package com.example.cookingbuddy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.bottomnavigation.BottomNavigationView;


public class NewRecipeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_recipe);
        //hooks to all xml in activity_main.xml
        Button addIngredientBtn = findViewById(R.id.add_ingredient_btn);
        Button addDirectionBtn = findViewById(R.id.add_direction_btn);
        Button addRecipeBtn = findViewById(R.id.save_btn);
        EditText ingredientText = findViewById(R.id.ingredient_text);
        EditText directionText = findViewById(R.id.direction_text);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.page_2:
                        return true;
                    case R.id.page_1:
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        break;

                }
                return true;
            }
        });
    }


}