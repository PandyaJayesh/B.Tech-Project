package com.example.wordcount;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class WorkerActivity extends AppCompatActivity {

    Thread Thread1 = null;
    EditText etIP, etPort;
    TextView tvMessages;
    Boolean connected = false;
    Socket socket;
    String SERVER_IP;
    int SERVER_PORT;
    OutputStream output;
    InputStream input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);

        etIP = findViewById(R.id.etIP);
        etPort = findViewById(R.id.etPort);
        tvMessages = findViewById(R.id.tvMessages);



        Button btnConnect = findViewById(R.id.btnConnect);

        Button btnReset = findViewById(R.id.btnReset);

        btnReset.setOnClickListener(v -> restartApp());

        btnConnect.setOnClickListener(v -> {

            tvMessages.setText("");
            SERVER_IP = etIP.getText().toString().trim();
            SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());

            connectToServer();

            btnConnect.setEnabled(false);



        });
    }


    public void restartApp() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Restart the activity
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }


    private void connectToServer() {
        Thread1 = new Thread(() -> {
            try {
                // Ensure Wi-Fi is connected
                if (!isWiFiConnected()) {
                    runOnUiThread(() -> tvMessages.append("Please connect to Wi-Fi before starting.\n"));
                    return;
                }

                // Try connecting to the server
                socket = new Socket(SERVER_IP, SERVER_PORT);
                output = socket.getOutputStream();
                input = socket.getInputStream();

                // Ensure UI updates work
                Looper.prepare();
                runOnUiThread(() -> {
                    tvMessages.setText("Connected ");
                    connected = true;
                });

                // Start listening for incoming files from the server
                receiveFileFromServer(socket);
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> tvMessages.append("Connection failed: " + e.getMessage() + "\n"));
            }
        });
        Thread1.start();
    }

    // Helper function to check if Wi-Fi is connected
    private boolean isWiFiConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // For Android 6+ (API 23+)
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            // For older Android versions (Below API 23)
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
    }



    private void sendMessageToServer(final String message) {
        if (message.isEmpty()) return;

        Thread thread = new Thread(() -> {
            try {
                if (output != null) {
                    DataOutputStream dos = new DataOutputStream(output);
                    dos.writeUTF(message); // Send the message
                    dos.flush(); // Ensure message is sent

                    runOnUiThread(() -> tvMessages.append("Client sent: " + message + "\n"));
                } else {
                    Log.e("CLIENT", "Output stream is null. Message not sent: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }



    private void receiveFileFromServer(Socket socket) {
        long totalStartTime = System.currentTimeMillis();
        long totalStartCpuTime = Helpers.getProcessCpuTime();
        float startCurrent = Helpers.getBatteryCurrentNow(this);

        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            // **Receiving File**
            long receiveStartTime = System.currentTimeMillis();
            long receiveStartCpuTime = Helpers.getProcessCpuTime();

            int numberOfFiles = dis.readInt();
            Log.d("CLIENT", "Number of files to receive: " + numberOfFiles);

            File directory = getExternalFilesDir(null);
            if (directory == null) {
                Log.e("CLIENT", "Failed to get external files directory.");
                return;
            }

            File fileToUpdate = null;
            for (int i = 0; i < numberOfFiles; i++) {
                long fileSize = dis.readLong();
                String fileName = dis.readUTF();
                Log.d("CLIENT", "Receiving file: " + fileName + ", size: " + fileSize);

                fileToUpdate = new File(directory, fileName);
                Log.d("CLIENT", "Debug 1" + fileToUpdate);
                FileOutputStream fos = new FileOutputStream(fileToUpdate);



                Log.d("CLIENT", "Debug 2");

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                Log.d("CLIENT", "Debug 3");
                fos.close();
            }
            Log.d("CLIENT", "Debug 4");
            long receiveEndTime = System.currentTimeMillis();
            long receiveEndCpuTime = Helpers.getProcessCpuTime();
            long receiveTime = receiveEndTime - receiveStartTime;
            long receiveCpuTime = receiveEndCpuTime - receiveStartCpuTime;
            Log.d("CLIENT", "Debug 5");
            // **Processing Word Count**
            long processStartTime = System.currentTimeMillis();
            long processStartCpuTime = Helpers.getProcessCpuTime();
            Log.d("CLIENT", "Debug 6");
            int ans = 0;
            if (fileToUpdate != null) {
                WordCount wordCount = new WordCount();
                ans = wordCount.countWords(fileToUpdate.getAbsolutePath());
            }
            Log.d("CLIENT", "Debug 7");
            long processEndTime = System.currentTimeMillis();
            long processEndCpuTime = Helpers.getProcessCpuTime();
            long processTime = processEndTime - processStartTime;
            long processCpuTime = processEndCpuTime - processStartCpuTime;
            double cpuUtilization = Helpers.calculateCPUUtilization(processTime,processCpuTime);

            Log.d("CLIENT", "Debug 8");
            // **Sending Result**
            long sendStartTime = System.currentTimeMillis();
            long sendStartCpuTime = Helpers.getProcessCpuTime();
            Log.d("CLIENT", "Debug 9");
            sendMessageToServer("Word Count is: " + ans + " \n");
            Log.d("CLIENT", "Debug 10");
            long sendEndTime = System.currentTimeMillis();
            long sendEndCpuTime = Helpers.getProcessCpuTime();
            long sendTime = sendEndTime - sendStartTime;
            long sendCpuTime = sendEndCpuTime - sendStartCpuTime;
            Log.d("CLIENT", "Debug 11");
            // **Final Stats**
            long totalEndTime = System.currentTimeMillis();
            long totalEndCpuTime = Helpers.getProcessCpuTime();
            float endCurrent = Helpers.getBatteryCurrentNow(this);
            Log.d("CLIENT", "Debug 12");
            long totalTime = totalEndTime - totalStartTime;
            long totalCpuTime = totalEndCpuTime - totalStartCpuTime;
            float batteryUsed =  Helpers.getBatteryUsage(this, startCurrent,endCurrent,totalTime);
            Log.d("CLIENT", "Debug 13");

            runOnUiThread(() -> {

                tvMessages.append("\n--- Worker Performance Metrics ---\n");
                //tvMessages.append("Receive Time: " + receiveTime + " ms, CPU: " + receiveCpuTime + " ms\n");
                tvMessages.append("Processing Time: " + processTime + " ms, CPU: " + cpuUtilization + " %\n");
                //tvMessages.append("Send Time: " + sendTime + " ms, CPU: " + sendCpuTime + " ms\n");
                //tvMessages.append("Total Time: " + totalTime + " ms, CPU: " + totalCpuTime + " ms\n");
                tvMessages.append("Battery Used: " + batteryUsed + " mWh\n");
            });
            Log.d("CLIENT", "Debug 14");
            if (fileToUpdate != null && fileToUpdate.exists()) {
                boolean deleted = fileToUpdate.delete();
                Log.d("CLIENT", "File deleted: " + deleted);
            }
            Log.d("CLIENT", "Debug 15");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CLIENT", "Error receiving file: " + e.getMessage());
        }
    }




}
