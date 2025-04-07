package com.example.trivia;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TriviaActivity extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();

    private TextView questionText, questionNumberText, scoreText, timerText;
    private MaterialButton option1, option2, option3, option4;
    private ProgressBar progressBar;
    private String correctAnswer;

    private int currentQuestionNumber = 0;
    private final int totalQuestions = 10;
    private int score = 0;

    private CountDownTimer countDownTimer;
    private static final int QUESTION_TIME = 30;          // שניות לשאלה
    private static final long FEEDBACK_DELAY = 5300;      // מ"ש משוב לפני שאלה הבאה

    private Handler handler;
    private boolean answered = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trivia);

        // איתחול רכיבי UI
        questionText = findViewById(R.id.question_text);
        questionNumberText = findViewById(R.id.question_number);
        scoreText = findViewById(R.id.score_text);
        timerText = findViewById(R.id.timer_text);
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        progressBar = findViewById(R.id.progress_bar);

        handler = new Handler(Looper.getMainLooper());

        updateScore();
        loadTriviaQuestion();
    }

    // ----------------------- UI helpers -----------------------

    private void updateQuestionNumber() {
        questionNumberText.setText("שאלה " + currentQuestionNumber + "/" + totalQuestions);
        int progress = (currentQuestionNumber * 100) / totalQuestions;
        progressBar.setProgress(progress);
    }

    private void updateScore() {
        scoreText.setText("ניקוד: " + score);
    }

    // ----------------------- Timer ----------------------------

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(QUESTION_TIME * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                timerText.setText("0");
                disableButtons();
                Toast.makeText(TriviaActivity.this, "נגמר הזמן!", Toast.LENGTH_SHORT).show();
                handler.postDelayed(() -> loadTriviaQuestion(), FEEDBACK_DELAY);
            }
        }.start();
    }

    // ----------------------- Networking & Question Logic ----------------

    private void loadTriviaQuestion() {
        if (currentQuestionNumber >= totalQuestions) {
            showGameOverDialog();
            return;
        }

        answered = false;
        resetButtons();

        currentQuestionNumber++;
        updateQuestionNumber();

        questionText.setText("טוען שאלה...");
        progressBar.setVisibility(View.VISIBLE);

        String url = "https://opentdb.com/api.php?amount=1&type=multiple";

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            JSONArray results = response.getJSONArray("results");
                            JSONObject questionObj = results.getJSONObject(0);

                            String question = questionObj.getString("question");
                            correctAnswer = questionObj.getString("correct_answer");
                            JSONArray incorrectAnswers = questionObj.getJSONArray("incorrect_answers");

                            // הצגת השאלה
                            questionText.setText(Html.fromHtml(question, Html.FROM_HTML_MODE_LEGACY));

                            // ערבוב תשובות
                            String[] answers = new String[4];
                            int correctPosition = (int) (Math.random() * 4);
                            int index = 0;
                            for (int i = 0; i < 4; i++) {
                                if (i == correctPosition) {
                                    answers[i] = correctAnswer;
                                } else {
                                    answers[i] = incorrectAnswers.getString(index++);
                                }
                            }

                            option1.setText(Html.fromHtml(answers[0], Html.FROM_HTML_MODE_LEGACY));
                            option2.setText(Html.fromHtml(answers[1], Html.FROM_HTML_MODE_LEGACY));
                            option3.setText(Html.fromHtml(answers[2], Html.FROM_HTML_MODE_LEGACY));
                            option4.setText(Html.fromHtml(answers[3], Html.FROM_HTML_MODE_LEGACY));

                            setAnswerClickListeners();
                            startTimer();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(TriviaActivity.this, "שגיאה בטעינת השאלה", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TriviaActivity.this, "שגיאה בחיבור לאינטרנט", Toast.LENGTH_SHORT).show();
            }
        });

        // timeout ארוך יותר
        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(request);
    }

    // ----------------------- Button Handling -----------------

    private void resetButtons() {
        MaterialButton[] buttons = {option1, option2, option3, option4};
        for (MaterialButton b : buttons) {
            b.setEnabled(true);
            b.setBackgroundTintList(getColorStateList(R.color.option_button_bg));
        }
    }

    private void setAnswerClickListeners() {
        View.OnClickListener listener = v -> {
            if (answered) return;   // מניעת לחיצות כפולות
            answered = true;

            if (countDownTimer != null) countDownTimer.cancel();

            MaterialButton selectedButton = (MaterialButton) v;
            String selectedAnswer = selectedButton.getText().toString();

            boolean correct = selectedAnswer.equals(correctAnswer) ||
                    Html.fromHtml(correctAnswer, Html.FROM_HTML_MODE_LEGACY).toString().equals(selectedAnswer);

            if (correct) {
                selectedButton.setBackgroundTintList(getColorStateList(R.color.correct_answer));
                Toast.makeText(TriviaActivity.this, "צדקת! זכית בנקודה", Toast.LENGTH_SHORT).show();
                score += 10;
                updateScore();
            } else {
                selectedButton.setBackgroundTintList(getColorStateList(R.color.wrong_answer));
                highlightCorrectAnswer();
                Toast.makeText(TriviaActivity.this, "לא נכון!", Toast.LENGTH_SHORT).show();
            }

            disableButtons();
            handler.postDelayed(() -> loadTriviaQuestion(), FEEDBACK_DELAY);
        };

        option1.setOnClickListener(listener);
        option2.setOnClickListener(listener);
        option3.setOnClickListener(listener);
        option4.setOnClickListener(listener);
    }

    private void highlightCorrectAnswer() {
        MaterialButton[] buttons = {option1, option2, option3, option4};
        for (MaterialButton button : buttons) {
            String text = button.getText().toString();
            if (text.equals(correctAnswer) ||
                    Html.fromHtml(correctAnswer, Html.FROM_HTML_MODE_LEGACY).toString().equals(text)) {
                button.setBackgroundTintList(getColorStateList(R.color.correct_answer));
                break;
            }
        }
    }

    private void disableButtons() {
        option1.setEnabled(false);
        option2.setEnabled(false);
        option3.setEnabled(false);
        option4.setEnabled(false);
    }

    // ----------------------- End of Game ----------------------

    private void updateUserCoinsInFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(auth.getUid());

            int coinsEarned = (score / 10) * 100;

            userRef.child("coins").get().addOnCompleteListener(task -> {
                int currentCoins = 0;
                if (task.isSuccessful() && task.getResult().exists()) {
                    currentCoins = task.getResult().getValue(Integer.class);
                }
                int newCoins = currentCoins + coinsEarned;

                userRef.child("coins").setValue(newCoins)
                        .addOnSuccessListener(unused -> Toast.makeText(TriviaActivity.this, "התווספו לך " + coinsEarned + " מטבעות!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(TriviaActivity.this, "שגיאה בעדכון מטבעות", Toast.LENGTH_SHORT).show());
            });
        } else {
            Toast.makeText(this, "המשתמש לא מחובר!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showGameOverDialog() {
        updateUserCoinsInFirebase(); // קריאה לעדכון המטבעות

        new AlertDialog.Builder(this)
                .setTitle("סיום המשחק")
                .setMessage("הניקוד שלך: " + score)
                .setPositiveButton("אישור", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
