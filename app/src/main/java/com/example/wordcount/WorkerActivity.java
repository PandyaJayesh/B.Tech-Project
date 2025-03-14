package com.example.wordcount;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WorkerActivity extends AppCompatActivity {

    Thread Thread1 = null;
    EditText etIP, etPort;
    TextView tvMessages;
    Boolean connected = false;
    EditText etMessage;
    Button btnSend;
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
//        etMessage = findViewById(R.id.etMessage);
//        btnSend = findViewById(R.id.btnSend);


        Button btnConnect = findViewById(R.id.btnConnect);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                tvMessages.setText("");
                SERVER_IP = etIP.getText().toString().trim();
                SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());

                connectToServer();

                btnConnect.setEnabled(false);



            }
        });

//        btnSend.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String message = etMessage.getText().toString().trim();
//                if (!message.isEmpty()) {
//                    sendMessageToServer(message);
//                }
//            }
//        });
    }


    private int getBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }


    private void connectToServer() {
        Thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Ensure Wi-Fi is connected
                    if (!isWiFiConnected()) {
                        runOnUiThread(() -> tvMessages.append("Please connect to Wi-Fi before starting.\n"));
                        return;
                    }

                    // Try connecting to the server
                    Socket socket = new Socket(SERVER_IP, SERVER_PORT);
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



//    private void sendMessageToServer(final String message) {
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
//                    writer.write(message + "\n");
//                    writer.flush();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            tvMessages.append("client: " + message + "\n");
//                            etMessage.setText("");
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        thread.start();
//    }

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
        long totalStartCpuTime = getProcessCpuTime();
        int batteryStart = getBatteryLevel();

        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            // **Receiving File**
            long receiveStartTime = System.currentTimeMillis();
            long receiveStartCpuTime = getProcessCpuTime();

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

                fileToUpdate = new File(directory, "testing.txt");
                FileOutputStream fos = new FileOutputStream(fileToUpdate);

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                fos.close();
            }

            long receiveEndTime = System.currentTimeMillis();
            long receiveEndCpuTime = getProcessCpuTime();
            long receiveTime = receiveEndTime - receiveStartTime;
            long receiveCpuTime = receiveEndCpuTime - receiveStartCpuTime;

            // **Processing Word Count**
            long processStartTime = System.currentTimeMillis();
            long processStartCpuTime = getProcessCpuTime();

            int ans = 0;
            if (fileToUpdate != null) {
                WordCount wordCount = new WordCount();
                ans = wordCount.countWords(fileToUpdate.getAbsolutePath());
            }

            long processEndTime = System.currentTimeMillis();
            long processEndCpuTime = getProcessCpuTime();
            long processTime = processEndTime - processStartTime;
            long processCpuTime = processEndCpuTime - processStartCpuTime;

            // **Sending Result**
            long sendStartTime = System.currentTimeMillis();
            long sendStartCpuTime = getProcessCpuTime();

            sendMessageToServer("Word Count is: " + ans + " \n");

            long sendEndTime = System.currentTimeMillis();
            long sendEndCpuTime = getProcessCpuTime();
            long sendTime = sendEndTime - sendStartTime;
            long sendCpuTime = sendEndCpuTime - sendStartCpuTime;

            // **Final Stats**
            long totalEndTime = System.currentTimeMillis();
            long totalEndCpuTime = getProcessCpuTime();
            int batteryEnd = getBatteryLevel();

            long totalTime = totalEndTime - totalStartTime;
            long totalCpuTime = totalEndCpuTime - totalStartCpuTime;
            int batteryUsed = batteryStart - batteryEnd;

            runOnUiThread(() -> {
                tvMessages.append("\n--- Worker Performance Metrics ---\n");
                tvMessages.append("Receive Time: " + receiveTime + " ms, CPU: " + receiveCpuTime + " ms\n");
                tvMessages.append("Processing Time: " + processTime + " ms, CPU: " + processCpuTime + " ms\n");
                tvMessages.append("Send Time: " + sendTime + " ms, CPU: " + sendCpuTime + " ms\n");
                tvMessages.append("Total Time: " + totalTime + " ms, CPU: " + totalCpuTime + " ms\n");
                tvMessages.append("Battery Used: " + batteryUsed + "%\n");
            });

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CLIENT", "Error receiving file: " + e.getMessage());
        }
    }

    private long getProcessCpuTime() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/self/stat"));
            String[] stats = reader.readLine().split(" ");
            reader.close();
            long utime = Long.parseLong(stats[13]);  // User mode time
            long stime = Long.parseLong(stats[14]);  // Kernel mode time
            return utime + stime;  // Total CPU time used
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }


}
