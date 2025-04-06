package com.example.trivia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity {

    private ImageButton selectedButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        ImageButton clanButton = findViewById(R.id.clan_button);
        ImageButton pvpButton = findViewById(R.id.pvp_button);
        ImageButton storeButton = findViewById(R.id.store_button);
        ImageButton playbutton = findViewById(R.id.play_button);
        playbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePageActivity.this, PvpActivity.class);
                startActivity(intent);
            }
        });

        clanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClick(clanButton);
            }
        });

        pvpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClick(pvpButton);
            }
        });

        storeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClick(storeButton);
            }
        });

        handleButtonClick(pvpButton);
    }

    private void handleButtonClick(ImageButton button) {
        if (selectedButton != null) {
            resetButton(selectedButton);
        }
        animateButton(button);
        selectedButton = button;
    }

    private void animateButton(ImageButton button) {
        button.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).start();
    }

    private void resetButton(ImageButton button) {
        button.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
    }
}