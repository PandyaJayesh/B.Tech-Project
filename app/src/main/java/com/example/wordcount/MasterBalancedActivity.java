package com.example.wordcount;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterBalancedActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // ExecutorService for background tasks
    ServerSocket serverSocket;
    Thread Thread1 = null;
    TextView tvIP, tvPort;
    TextView tvMessages;
    EditText etMessage;
    Button btnSend;
    Button btnReset;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 8080;
    String message;
    int clientCount = 0; // Variable to track connected clients
    Set<Socket> clientSockets = new HashSet<>(); // Set to store active client sockets
    Map<String, Double> computingCapacity = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_balanced);
        tvIP = findViewById(R.id.tvIPmb);
        tvPort = findViewById(R.id.tvPortmb);
        tvMessages = findViewById(R.id.tvMessagesmb);
//        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSendmb);
        btnReset = findViewById(R.id.btnResermb);
        SERVER_IP = getLocalIpAddress();
        Thread1 = new Thread(new Thread1());
        Thread1.start();

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartApp();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = Environment.getExternalStorageDirectory().getPath() + "/testing.txt";
                executorService.execute(() -> sendFileToClients(fileName));

//                if (checkAndRequestPermissions()) {
////                    String fileName = Environment.getExternalStorageDirectory().getPath() + "/testing.txt"; // Replace with your actual file
////                    new SendFileTask().execute(fileName);
//
////                    File file = new File(getExternalFilesDir(null), "testing.txt");
//////                    new SendFileTask().execute(file.getAbsolutePath());
////                    executorService.execute(() -> sendFileToClients(file.getAbsolutePath()));
//
//                    String fileName = Environment.getExternalStorageDirectory().getPath() + "/testing.txt";
//                    executorService.execute(() -> sendFileToClients(fileName));
//                }
            }
        });

    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CODE_STORAGE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btnSend.performClick();
            } else {
                tvMessages.append("Storage permission denied.\n");
            }
        }
    }

    public void restartApp() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        int pendingIntentId = 123456;
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), pendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
        System.exit(0);
    }

//    private class SendFileTask extends AsyncTask<String, Void, Void> {
//        @Override
//        protected Void doInBackground(String... params) {
//            sendFileToClients(params[0]);
//            return null;
//        }
//    }

//    private void sendFileToClients(String fileName) {
//        startTime = System.currentTimeMillis();
//        File file = new File(fileName);
//        if (!file.exists()) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    tvMessages.append("File not found: " + fileName + "\n");
//                }
//            });
//            return;
//        }
//
//        try {
//            // Calculate number of clients and split file accordingly
//            int numberOfClients = clientSockets.size();
//            List<String> subfileNames = FileSplitter.splitTextFile(fileName, numberOfClients);
//
//            // Iterate over each client socket and send corresponding subfile
//            int clientIndex = 0;
//            for (Socket clientSocket : clientSockets) {
//                String subfileName = subfileNames.get(clientIndex);
//                sendSubfile(clientSocket, subfileName);
//                clientIndex++;
//            }
//            long endTime = System.currentTimeMillis();
//            timeTaken = endTime - startTime;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


    private void sendFileToClients(String fileName) {
        long totalStartTime = System.currentTimeMillis();
        long totalStartCpuTime = getProcessCpuTime();
        float batteryStart = getBatteryLevel();
//        tvMessages.append("Debug: 1\n");
        // **Partitioning**
        long partitionStartTime = System.currentTimeMillis();
        long partitionStartCpuTime = getProcessCpuTime();
//        tvMessages.append("Debug: 2\n");
        File file = new File(fileName);
        if (!file.exists()) {
            runOnUiThread(() -> tvMessages.append("File not found: " + fileName + "\n"));
            return;
        }
//        tvMessages.append("Debug: 3\n");
        List<String> subfileNames;
        try {
            subfileNames = FileSplitter.splitTextFileBySize(fileName, computingCapacity);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
//        tvMessages.append("Debug: 4\n");
        long partitionEndTime = System.currentTimeMillis();
        long partitionEndCpuTime = getProcessCpuTime();
        long partitionTime = partitionEndTime - partitionStartTime;
        long partitionCpuTime = partitionEndCpuTime - partitionStartCpuTime;
//        tvMessages.append("Debug: 5\n");
        // **Sending Files**
        long sendStartTime = System.currentTimeMillis();
        long sendStartCpuTime = getProcessCpuTime();
//        tvMessages.append("Debug: 6\n");
        long totalWordCount = 0;
        int clientIndex = 0;
        for (Socket clientSocket : clientSockets) {
            try {
                totalWordCount += sendSubfile(clientSocket, subfileNames.get(clientIndex));
                clientIndex++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        tvMessages.append("Debug: 7\n");

        long sendEndTime = System.currentTimeMillis();
        long sendEndCpuTime = getProcessCpuTime();
        long sendTime = sendEndTime - sendStartTime;
        long sendCpuTime = sendEndCpuTime - sendStartCpuTime;
//        tvMessages.append("Debug: 8\n");
        // **Receiving Word Counts**
        long receiveStartTime = System.currentTimeMillis();
        long receiveStartCpuTime = getProcessCpuTime();


//        tvMessages.append("Debug: 9\n");

        long receiveEndTime = System.currentTimeMillis();
        long receiveEndCpuTime = getProcessCpuTime();
        long receiveTime = receiveEndTime - receiveStartTime;
        long receiveCpuTime = receiveEndCpuTime - receiveStartCpuTime;

        // **Final Stats**
        long totalEndTime = System.currentTimeMillis();
        long totalEndCpuTime = getProcessCpuTime();
        float batteryEnd = getBatteryLevel();

        long totalTime = totalEndTime - totalStartTime;
        long totalCpuTime = totalEndCpuTime - totalStartCpuTime;
        float batteryUsed = batteryStart - batteryEnd;
//        tvMessages.append("Debug: 10\n");
        runOnUiThread(() -> {
            tvMessages.append("\n--- Master Performance Metrics ---\n");
            tvMessages.append("Partition Time: " + partitionTime + " ms, CPU: " + partitionCpuTime + " ms\n");
            tvMessages.append("Send Time: " + sendTime + " ms, CPU: " + sendCpuTime + " ms\n");
            tvMessages.append("Receive Time: " + receiveTime + " ms, CPU: " + receiveCpuTime + " ms\n");
            tvMessages.append("Total Time: " + totalTime + " ms, CPU: " + totalCpuTime + " ms\n");
            tvMessages.append("Battery Used: " + batteryUsed + "%\n");
        });
//        tvMessages.append("Debug: 11\n");
    }

    private float getBatteryLevel() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level == -1 || scale == -1) {
            return -1.0f; // Error case
        }

        return ((float) level / (float) scale) * 100.0f; // Returns detailed battery level
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


    private long sendSubfile(Socket clientSocket, String subfileName) throws IOException {
        File file = new File(subfileName);
        if (!file.exists()) {
            runOnUiThread(() -> tvMessages.append("File not found: " + subfileName + "\n"));
            return -1;
        }

        byte[] buffer = new byte[10000];
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);

        long fileSize = file.length();
        DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

        dos.writeInt(1);  // Send number of files
        dos.writeLong(fileSize); // Send file size
        dos.writeUTF(file.getName()); // Send file name

        int bytesRead;
        while ((bytesRead = bis.read(buffer)) > 0) {
            dos.write(buffer, 0, bytesRead);
        }
        dos.flush();
        bis.close();

        // **🔹 NEW: Read the response from the Worker**
        DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
        String workerResponse = dis.readUTF();  // Read message from worker

        runOnUiThread(() -> tvMessages.append("File sent to client: "
                + clientSocket.getInetAddress().getHostAddress() + "\n"
                + "Worker Response: " + workerResponse + "\n"));
        String[] parts = workerResponse.split(" ");
        return Long.parseLong(parts[3]); // Extract the 4th word (index 3)
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

    private PrintWriter output;
    private BufferedReader input;

    class Thread1 implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.setText("Not connected");
                        tvIP.setText("IP: " + SERVER_IP);
                        tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
                    }
                });

                while (true) {
                    socket = serverSocket.accept();
                    if (clientSockets.add(socket)) {
                        // New client connected
                        updateClientCountUI(clientSockets.size()); // Update UI with client count
                        output = new PrintWriter(socket.getOutputStream(), true);
                        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.setText("Connected clients: " + clientSockets.size() + "\n"); // Update UI message
                            }
                        });
                        new Thread(new Thread2()).start(); // Pass socket to Thread2
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateClientCountUI(int count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMessages.setText("Connected clients: " + count + "\n");
            }
        });
    }

    private class Thread2 implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = input.readLine()) != null) {
                    // Extract speed and IP from the message
                    if (message.contains("Speed:") && message.contains("IP:")) {
                        // Use regex or substring to extract the values
                        String speed = extractSpeed(message);
                        String ipAddress = extractIp(message);

                        // Store the extracted values in a HashMap with IP as key and Speed as value
                        computingCapacity.put(ipAddress, Double.parseDouble(speed));

                        // Optionally, print or log the extracted data
                        Log.d("SERVER", "Extracted Speed: " + speed + ", IP: " + ipAddress);

                        // Update the UI with the received message
                        final String receivedMessage = "Speed: " + speed + ", IP: " + ipAddress;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.append(receivedMessage + "\n");
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Method to extract the speed from the message
        private String extractSpeed(String message) {
            String speed = null;
            try {
                int speedStart = message.indexOf("Speed:") + 7;  // Skip "Speed: "
                int speedEnd = message.indexOf(",", speedStart);  // Find the comma after the speed
                if (speedStart != -1 && speedEnd != -1) {
                    speed = message.substring(speedStart, speedEnd).trim();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return speed;
        }

        // Method to extract the IP address from the message
        private String extractIp(String message) {
            String ip = null;
            try {
                int ipStart = message.indexOf("IP:") + 4;  // Skip "IP: "
                if (ipStart != -1) {
                    ip = message.substring(ipStart).trim();  // Get everything after "IP: "
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ip;
        }
    }


//    class Thread3 implements Runnable {
//        private String message;
//
//        Thread3(String message) {
//            this.message = message;
//        }
//
//        @Override
//        public void run() {
//            output.println(message);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    tvMessages.append("server: " + message + " ");
////                    etMessage.setText("");
//                }
//            });
//        }
//
//
//    }
}