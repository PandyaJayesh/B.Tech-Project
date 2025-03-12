package com.example.wordcount;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class EqualActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equal);

        Button btnMaster = findViewById(R.id.btnMaster);
        Button btnWorker = findViewById(R.id.btnWorker);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }



        btnMaster.setOnClickListener(v -> {
            Intent intent = new Intent(EqualActivity.this, MasterActivity.class);
            startActivity(intent);
        });

        btnWorker.setOnClickListener(v -> {
            Intent intent = new Intent(EqualActivity.this, WorkerActivity.class);
            startActivity(intent);
        });
    }
}
