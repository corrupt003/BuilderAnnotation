package com.corrupt003.android.builderannotation;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainJavaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_java);

        User user = new UserBuilder()
                .name("corrupt003")
                .age(20)
                .build();

        String text = String.format(
                Locale.getDefault(),
                "User name: %s, age: %d",
                user.name, user.age);

        TextView message = findViewById(R.id.main_java_message);
        message.setText(text);

        UserKotlin userKotlin = new UserKotlinBuilder()
                .name("corrupt003_kotlin")
                .age(50)
                .build();

        String kotlinText = String.format(
                Locale.getDefault(),
                "User name: %s, age: %d",
                userKotlin.getName(), userKotlin.getAge());

        TextView messageKotlin = findViewById(R.id.main_java_message_kotlin);
        messageKotlin.setText(kotlinText);
    }
}
