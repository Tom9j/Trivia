package com.example.trivia;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.ImageButton;

public class PlayFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play, container, false);

        ImageButton playButton = view.findViewById(R.id.play_button_fragment);
        playButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TriviaActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
