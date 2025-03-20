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

public class EqualActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

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
