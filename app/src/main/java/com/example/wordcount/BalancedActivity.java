package com.example.wordcount;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BalancedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balanced);

        Button btnMasterBalanced = findViewById(R.id.btnMasterBalanced);
        Button btnWorkerBalanced = findViewById(R.id.btnWorkerBalanced);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }


        btnMasterBalanced.setOnClickListener(v -> {
            Intent intent = new Intent(BalancedActivity.this, MasterBalancedActivity.class);
            startActivity(intent);
        });

        btnWorkerBalanced.setOnClickListener(v -> {
            Intent intent = new Intent(BalancedActivity.this, WorkerBalancedActivity.class);
            startActivity(intent);
        });
    }
}
