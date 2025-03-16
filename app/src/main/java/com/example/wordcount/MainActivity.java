package com.example.wordcount;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnEqual = findViewById(R.id.btnEqual);
        Button btnBalanced = findViewById(R.id.btnBalanced);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }



        btnEqual.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EqualActivity.class);
            startActivity(intent);
        });

        btnBalanced.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BalancedActivity.class);
            startActivity(intent);
        });
    }
}
