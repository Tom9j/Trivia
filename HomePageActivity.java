package com.example.trivia.home;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.trivia.ClanFragment;
import com.example.trivia.PlayFragment;
import com.example.trivia.R;
import com.example.trivia.store.StoreFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class HomePageActivity extends AppCompatActivity {

    private ImageButton selectedButton = null;
    private TextView coinsText;
    private DatabaseReference userRef;

    private ImageButton clanButton, pvpButton, storeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.trivia.R.layout.activity_home_page);

        coinsText = findViewById(com.example.trivia.R.id.coins_text);
        loadUserCoins();

        clanButton = findViewById(com.example.trivia.R.id.clan_button);
        pvpButton = findViewById(com.example.trivia.R.id.pvp_button);
        storeButton = findViewById(com.example.trivia.R.id.store_button);

        clanButton.setOnClickListener(v -> {
            handleButtonClick(clanButton);
            replaceFragment(new ClanFragment());
        });

        pvpButton.setOnClickListener(v -> {
            handleButtonClick(pvpButton);
            replaceFragment(new PlayFragment());
        });

        storeButton.setOnClickListener(v -> {
            handleButtonClick(storeButton);
            replaceFragment(new StoreFragment());
        });

        // טעינה ראשונית של ה־Fragment (PVP כברירת מחדל)
        if (savedInstanceState == null) {
            handleButtonClick(pvpButton);
            replaceFragment(new PlayFragment());
        }
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    private void handleButtonClick(ImageButton button) {
        if (selectedButton != null) resetButton(selectedButton);
        animateButton(button);
        selectedButton = button;
    }

    private void animateButton(ImageButton button) {
        button.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).start();
    }

    private void resetButton(ImageButton button) {
        button.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
    }

    private void loadUserCoins() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            coinsText.setText("0");
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(auth.getUid()).child("coins");
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int coins = snapshot.getValue(Integer.class);
                    coinsText.setText(String.valueOf(coins));
                } else {
                    coinsText.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomePageActivity.this, "שגיאה בטעינת המטבעות", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
