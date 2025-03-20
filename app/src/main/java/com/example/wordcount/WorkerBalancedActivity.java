package com.example.wordcount;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;  // Use DataOutputStream instead of WriterOutputStream
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class WorkerBalancedActivity extends AppCompatActivity {

    Thread Thread1 = null;

    Socket socket;
    EditText etIP, etPort;
    OutputStream outputStream = null;
    OutputStream output;
    Socket resultSocket;
    TextView tvMessages;
    Boolean connected = false;
    String SERVER_IP;
    int SERVER_PORT;
    DataOutputStream dos;
    DataOutputStream dosSpeed;  // Use DataOutputStream instead of WriterOutputStream
    DataInputStream dis;
    public static String LOCAL_IP = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_balanced);

        etIP = findViewById(R.id.etIPwb);
        etPort = findViewById(R.id.etPortwb);
        tvMessages = findViewById(R.id.tvMessageswb);
        LOCAL_IP = getLocalIpAddress();
        Button btnConnect = findViewById(R.id.btnConnectwb);

        Button btnReset = findViewById(R.id.btnResetwb);

        btnReset.setOnClickListener(v -> restartApp());

        btnConnect.setOnClickListener(v -> {
            runOnUiThread(() -> { tvMessages.setText("");});
            SERVER_IP = etIP.getText().toString().trim();
            SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());
            connectToServer();
            btnConnect.setEnabled(false);
        });
    }
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
    private void connectToServer() {
        Thread1 = new Thread(() -> {
            try  {
                if (!isWiFiConnected()) {
                    runOnUiThread(() -> tvMessages.append("Please connect to Wi-Fi before starting.\n"));
                    return;
                }
                socket = new Socket(SERVER_IP, SERVER_PORT);
                output = socket.getOutputStream();

                try {
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                dos = new DataOutputStream(outputStream);  // Use DataOutputStream
                try {
                    dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                dosSpeed = new DataOutputStream(outputStream);  // Use DataOutputStream


                runOnUiThread(() -> {
                    tvMessages.setText("Connected \n");
                    connected = true;
                });

                // Create a new thread to measure and send computation speed
                // Measure and send speed here in a separate thread
                new Thread(this::measureAndSendComputationSpeed).start();

                // Receive file from server
                receiveFileFromServer();

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("CLIENT", "Connection error: " + e.getMessage() + "\n");
            }
        });
        Thread1.start();
    }

    public void restartApp() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }
            if (resultSocket != null && !resultSocket.isClosed()) {
                try {
                    resultSocket.close();
                    Log.d("WORKER", " Closed client socket.");
                } catch (IOException e) {
                    Log.e("WORKER", "⚠️ Error closing client socket: " + e.getMessage());
                }
                resultSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Restart the activity
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private String getLocalIpAddress() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            runOnUiThread(() -> tvMessages.append("No IP Address.\n"));
            return "No Network";
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            runOnUiThread(() -> tvMessages.append("No IP Address.\n"));
            return "No Network";
        }

        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        if (linkProperties == null) {
            runOnUiThread(() -> tvMessages.append("No IP Address.\n"));
            return "No IP Address";

        }

        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                return address.getHostAddress();  // IPv4 address
            }
        }
        runOnUiThread(() -> tvMessages.append("No IP Address.\n"));
        return "No IPv4 Address";
    }
    private void measureAndSendComputationSpeed() {
        long startTime = System.currentTimeMillis();

        // Perform sample computation (e.g., word count on a dummy string)
        WordCount wordCount = new WordCount();
        String dummyText = "This is a test string for measuring computation speed.";
        for (int i = 0; i < 1000; i++) {
            wordCount.countWords(dummyText);
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        // Calculate speed as operations per millisecond
        double speed = 1000.0 / elapsedTime;
        Log.d("CLIENT", "Measured computation speed: " + speed + " ops/ms");

        // Send speed to the server
        sendSpeedAndIPToServer(speed,LOCAL_IP);
    }

    private void sendSpeedAndIPToServer(double clientSpeed, String ipAddress) {
        try {
            // Create the message with both IP and speed
            String speedData = "Speed: " + clientSpeed + ", IP: " + ipAddress;

            // Send the message to the server using dosSpeed
            dosSpeed.write(speedData.getBytes());
            dosSpeed.flush();  // Ensure the message is sent immediately

            // Optionally, you can send a "end of message" separator if the server expects it.
            dosSpeed.write("\n".getBytes());  // Sending a newline or special separator

            dosSpeed.flush();  // Flush again to make sure the separator is sent immediately

            // Log the sent data
            Log.d("CLIENT", "Client speed and IP sent to server: " + speedData);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CLIENT", "Error sending speed and IP to server: " + e.getMessage());
        }
    }




    private void receiveFileFromServer() {
        long totalStartTime = System.currentTimeMillis();
        long totalStartCpuTime = Helpers.getProcessCpuTime();
        float startCurrent = Helpers.getBatteryCurrentNow(this);
        try {
            long receiveStartTime = System.currentTimeMillis();
            long receiveStartCpuTime = Helpers.getProcessCpuTime();
            // Receive number of files
            int numberOfFiles = dis.readInt();
            Log.d("CLIENT", "Number of files to receive: " + numberOfFiles);

            // Receive files
            File fileToUpdate = null;
            for (int i = 0; i < numberOfFiles; i++) {
                long fileSize = dis.readLong();  // Use readLong to read file size (use readInt if it's an integer size)
                String fileName = dis.readUTF();
                Log.d("CLIENT", "Receiving file: " + fileName + ", size: " + fileSize);

                // Setup for writing file
                File directory = getExternalFilesDir(null);
                if (directory == null) {
                    Log.e("CLIENT", "Failed to access external files directory.");
                    return;
                }
                fileToUpdate = new File(directory, fileName);
                FileOutputStream fos = new FileOutputStream(fileToUpdate);

                // Read and write file chunks
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                while (totalBytesRead < fileSize &&
                        (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                fos.close();
                Log.d("CLIENT", "File received: " + fileToUpdate.getAbsolutePath());

                long receiveEndTime = System.currentTimeMillis();
                long receiveEndCpuTime = Helpers.getProcessCpuTime();
                long receiveTime = receiveEndTime - receiveStartTime;
                long receiveCpuTime = receiveEndCpuTime - receiveStartCpuTime;


                // Measure time for word count
                WordCount wordCount = new WordCount();

                long processStartTime = System.currentTimeMillis();
                long processStartCpuTime = Helpers.getProcessCpuTime();

                int wordCountResult = wordCount.countWords(fileToUpdate.getAbsolutePath());
                long processEndTime = System.currentTimeMillis();
                long processEndCpuTime = Helpers.getProcessCpuTime();
                long processTime = processEndTime - processStartTime;
                long processCpuTime = processEndCpuTime - processStartCpuTime;
                double cpuUtilization = Helpers.calculateCPUUtilization(processTime,processCpuTime);


                // **Sending Result**
                long sendStartTime = System.currentTimeMillis();
                long sendStartCpuTime = Helpers.getProcessCpuTime();

                // Display results on UI


                // Send results back to server
                sendResultsToServer(wordCountResult, processCpuTime, socket.getInetAddress().getHostAddress());

                long sendEndTime = System.currentTimeMillis();
                long sendEndCpuTime = Helpers.getProcessCpuTime();
                long sendTime = sendEndTime - sendStartTime;
                long sendCpuTime = sendEndCpuTime - sendStartCpuTime;

                // **Final Stats**
                long totalEndTime = System.currentTimeMillis();
                long totalEndCpuTime = Helpers.getProcessCpuTime();
                float endCurrent = Helpers.getBatteryCurrentNow(this);

                long totalTime = totalEndTime - totalStartTime;
                long totalCpuTime = totalEndCpuTime - totalStartCpuTime;
                float batteryUsed =  Helpers.getBatteryUsage(this, startCurrent,endCurrent,totalTime);

                File finalFileToUpdate = fileToUpdate;
                runOnUiThread(() -> {
                    tvMessages.append("File received: " + finalFileToUpdate.getAbsolutePath() + "\n");
                    tvMessages.append("Word Count: " + wordCountResult + ", Time: " + processCpuTime + " ms(time used by CPU)\n");

                    tvMessages.append("\n--- Worker Performance Metrics ---\n");
                    //tvMessages.append("Receive Time: " + receiveTime + " ms, CPU: " + receiveCpuTime + " ms\n");
                    tvMessages.append("Processing Time: " + processTime + " ms, CPU: " + cpuUtilization + " %\n");
                    //tvMessages.append("Send Time: " + sendTime + " ms, CPU: " + sendCpuTime + " ms\n");
                    //tvMessages.append("Total Time: " + totalTime + " ms, CPU: " + totalCpuTime + " ms\n");
                    tvMessages.append("Battery Used: " + batteryUsed + " mWh\n");
                });
            }

            if (fileToUpdate != null && fileToUpdate.exists()) {
                boolean deleted = fileToUpdate.delete();
                Log.d("CLIENT", "File deleted: " + deleted);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CLIENT", "Error receiving file: " + e.getMessage());
        }
    }



    private void sendResultsToServer(int wordCountResult, long computationTime, String masterIP) {
        new Thread(() -> {
            try {
                // 🔹 Connect to Master on port 5001 for results
                resultSocket = new Socket(masterIP, 5001);
                DataOutputStream dos = new DataOutputStream(resultSocket.getOutputStream());

                // 🔹 Send results
                String results = "Word Count: " + wordCountResult + ", Time: " + computationTime + " ms";
                Log.d("WORKER", "🔸 Connecting to Master on port 5001...");
                dos.writeUTF(results);
                dos.flush();
                Log.d("WORKER", "✅ Results sent to server: " + results);

                // 🔹 Wait for Master acknowledgment
                DataInputStream dis = new DataInputStream(resultSocket.getInputStream());
                Log.d("WORKER", "🔹 Waiting for ACK from Master...");
                String ack = dis.readUTF();
                Log.d("WORKER", "✅ Acknowledgment received from Master: " + ack);

                resultSocket.close();  // Close the result socket

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("WORKER", "🚨 Error sending results: " + e.getMessage());
            }
        }).start();
    }








}