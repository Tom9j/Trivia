package com.example.trivia;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TriviaActivity extends AppCompatActivity {

    private TextView questionText, questionNumberText, scoreText, timerText;
    private MaterialButton option1, option2, option3, option4, nextButton;
    private ProgressBar progressBar;
    private String correctAnswer;
    private int currentQuestionNumber = 0;
    private int totalQuestions = 10;
    private int score = 0;
    private CountDownTimer countDownTimer;
    private static final int QUESTION_TIME = 30; // 30 שניות לשאלה

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trivia);

        // אתחול רכיבי ה-UI
        questionText = findViewById(R.id.question_text);
        questionNumberText = findViewById(R.id.question_number);
        scoreText = findViewById(R.id.score_text);
        timerText = findViewById(R.id.timer_text);
        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        nextButton = findViewById(R.id.next_button);
        progressBar = findViewById(R.id.progress_bar);

        // הגדרת ערכים התחלתיים
        updateQuestionNumber();
        updateScore();

        // טעינת שאלה ראשונה
        loadTriviaQuestion();

        // הגדרת מאזין לכפתור "שאלה הבאה"
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                loadTriviaQuestion();
            }
        });
    }

    private void updateQuestionNumber() {
        currentQuestionNumber++;
        questionNumberText.setText("שאלה " + currentQuestionNumber + "/" + totalQuestions);
        // עדכון פס ההתקדמות
        int progress = (currentQuestionNumber * 100) / totalQuestions;
        progressBar.setProgress(progress);
    }

    private void updateScore() {
        scoreText.setText("ניקוד: " + score);
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(QUESTION_TIME * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                timerText.setText("0");
                disableButtons();
                Toast.makeText(TriviaActivity.this, "נגמר הזמן!", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void loadTriviaQuestion() {
        // איפוס מצב הכפתורים
        resetButtons();

        // הצגת טעינה
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

                            // חשוב: פענוח HTML entities
                            questionText.setText(Html.fromHtml(question, Html.FROM_HTML_MODE_LEGACY));

                            // הצגת תשובות בערבוב אקראי
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
                            updateQuestionNumber();

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

        queue.add(request);
    }

    private void resetButtons() {
        option1.setEnabled(true);
        option2.setEnabled(true);
        option3.setEnabled(true);
        option4.setEnabled(true);

        option1.setBackgroundTintList(getColorStateList(R.color.option_button_bg));
        option2.setBackgroundTintList(getColorStateList(R.color.option_button_bg));
        option3.setBackgroundTintList(getColorStateList(R.color.option_button_bg));
        option4.setBackgroundTintList(getColorStateList(R.color.option_button_bg));
    }

    private void setAnswerClickListeners() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // עצירת הטיימר
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }

                MaterialButton selectedButton = (MaterialButton) v;
                String selectedAnswer = selectedButton.getText().toString();

                if (selectedAnswer.equals(correctAnswer) ||
                        Html.fromHtml(correctAnswer, Html.FROM_HTML_MODE_LEGACY).toString().equals(selectedAnswer)) {
                    // תשובה נכונה
                    selectedButton.setBackgroundTintList(getColorStateList(R.color.correct_answer));
                    Toast.makeText(TriviaActivity.this, "נכון!", Toast.LENGTH_SHORT).show();
                    score += 10;
                    updateScore();
                } else {
                    // תשובה לא נכונה
                    selectedButton.setBackgroundTintList(getColorStateList(R.color.wrong_answer));

                    // הצגת התשובה הנכונה
                    highlightCorrectAnswer();

                    Toast.makeText(TriviaActivity.this, "לא נכון!", Toast.LENGTH_SHORT).show();
                }

                disableButtons();
            }
        };

        option1.setOnClickListener(listener);
        option2.setOnClickListener(listener);
        option3.setOnClickListener(listener);
        option4.setOnClickListener(listener);
    }

    private void highlightCorrectAnswer() {
        MaterialButton[] buttons = {option1, option2, option3, option4};

        for (MaterialButton button : buttons) {
            String buttonText = button.getText().toString();
            if (buttonText.equals(correctAnswer) ||
                    Html.fromHtml(correctAnswer, Html.FROM_HTML_MODE_LEGACY).toString().equals(buttonText)) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}