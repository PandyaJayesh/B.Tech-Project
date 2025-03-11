package com.example.wordcount;

import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WorkerBalancedActivity extends AppCompatActivity {

    Thread Thread1 = null;
    EditText etIP, etPort;
    TextView tvMessages;
    Boolean connected = false;
    String SERVER_IP;
    int SERVER_PORT;
    DataOutputStream dos , dosSpeed;  // Use DataOutputStream instead of WriterOutputStream
    DataInputStream dis , disSpeed;  // Keep using DataInputStream for reading input stream
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

        btnConnect.setOnClickListener(v -> {
            tvMessages.setText("");
            SERVER_IP = etIP.getText().toString().trim();
            SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());
            connectToServer();
            btnConnect.setEnabled(false);
        });
    }

    //    private void connectToServer() {
//        Thread1 = new Thread(() -> {
//            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
//                OutputStream outputStream = socket.getOutputStream();
//                dos = new DataOutputStream(outputStream);  // Use DataOutputStream
//                dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
//
//                runOnUiThread(() -> {
//                    tvMessages.setText("Connected ");
//                    connected = true;
//                });
//
//                // Measure computation speed and send it to the server immediately after connection
//                measureAndSendComputationSpeed();
//                // Receive file from server
//                receiveFileFromServer();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                Log.e("CLIENT", "Connection error: " + e.getMessage());
//            }
//        });
//        Thread1.start();
//    }
    private void connectToServer() {
        Thread1 = new Thread(() -> {
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
                OutputStream outputStream = socket.getOutputStream();
                dos = new DataOutputStream(outputStream);  // Use DataOutputStream
                dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dosSpeed = new DataOutputStream(outputStream);  // Use DataOutputStream
                disSpeed = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

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


    private String getLocalIpAddress() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            tvMessages.append("No IP Address.\n");
            return "No Network";
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            tvMessages.append("No IP Address.\n");
            return "No Network";
        }

        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        if (linkProperties == null) {
            tvMessages.append("No IP Address.\n");
            return "No IP Address";

        }

        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                return address.getHostAddress();  // IPv4 address
            }
        }
        tvMessages.append("No IP Address.\n");
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
        try {
            // Receive number of files
            int numberOfFiles = dis.readInt();
            Log.d("CLIENT", "Number of files to receive: " + numberOfFiles);

            // Receive files
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
                File fileToUpdate = new File(directory, fileName);
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

                // Measure time for word count
                WordCount wordCount = new WordCount();
                long startTime = System.currentTimeMillis();
                int wordCountResult = wordCount.countWords(fileToUpdate.getAbsolutePath());
                long endTime = System.currentTimeMillis();
                long computationTime = endTime - startTime;

                // Display results on UI
                runOnUiThread(() -> {
                    tvMessages.append("File received: " + fileToUpdate.getAbsolutePath() + "\n");
                    tvMessages.append("Word Count: " + wordCountResult + ", Time: " + computationTime + " ms\n");
                });

                // Send results back to server
                sendResultsToServer(wordCountResult, computationTime);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CLIENT", "Error receiving file: " + e.getMessage());
        }
    }

    private void sendResultsToServer(int wordCountResult, long computationTime) {
        try {
            String results = "Word Count: " + wordCountResult + ", Time: " + computationTime + " ms";
            dos.write(results.getBytes());  // Send the results after processing the file
            dos.flush();
            Log.d("CLIENT", "Results sent to server: " + results);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CLIENT", "Error sending results: " + e.getMessage());
        }
    }

}