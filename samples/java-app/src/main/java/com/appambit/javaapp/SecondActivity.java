package com.appambit.javaapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    Button btnReturnToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);

        btnReturnToMain = findViewById(R.id.btnReturnMain);
        btnReturnToMain.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        });

    }
}