package com.example.wordcount;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterBalancedActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // ExecutorService for background tasks
    ServerSocket serverSocket;
    ServerSocket resultServerSocket;
    Thread Thread1 = null;
    long sendingStart = 0;
    long sendingEnd = 0;
    Helpers.LogThread logThread = null;
    long sending = 0;
    long sendingStartCPU = 0;
    long sendingEndCPU = 0;
    long sendingCPU = 0;

    private final Map<Socket, Thread2> clientThreads = new HashMap<>();

    Thread Thread2 = null;
    TextView tvIP, tvPort;
    TextView tvMessages;
    TextView tvConnectionStatusmb;
    EditText etMessage;
    Button btnSend;
    Button btnReset;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 8080;
    String message;
    int clientCount = 0; // Variable to track connected clients
    Set<Socket> clientSockets = new HashSet<>(); // Set to store active client sockets
    Map<Socket, Integer> client_Sockets = new HashMap<>();
    Map<String, Double> computingCapacity = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_balanced);
        tvIP = findViewById(R.id.tvIPmb);
        tvPort = findViewById(R.id.tvPortmb);
        tvMessages = findViewById(R.id.tvMessagesmb);
        tvConnectionStatusmb = findViewById(R.id.tvConnectionStatusmb);
        btnSend = findViewById(R.id.btnSendmb);
        btnReset = findViewById(R.id.btnResermb);
        SERVER_IP = getLocalIpAddress();
        Thread1 = new Thread(new Thread1());
        Thread1.start();

        btnReset.setOnClickListener(v -> restartApp());

        logThread = new Helpers.LogThread(this, 200,tvMessages);

        btnSend.setOnClickListener(v -> {
//            File file = new File(this.getExternalFilesDir(null), "testing.txt");
//            String fileName = file.getAbsolutePath();
            String fileName = Environment.getExternalStorageDirectory().getPath() + "/testing.txt";
            executorService.execute(() -> sendFileToClients(fileName));




//                if (checkAndRequestPermissions()) {
//                   String fileName = Environment.getExternalStorageDirectory().getPath() + "/testing.txt";
//                  executorService.execute(() -> sendFileToClients(fileName));
//                }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
        try {
            for (Socket socket : client_Sockets.keySet()) {
                System.out.println("Socket: " + socket + ", Value: " + client_Sockets.get(socket));
            }
            client_Sockets.clear(); // Clear the list
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
            logThread.stopLogging();
            if (resultServerSocket != null && !resultServerSocket.isClosed()) {
                try {
                    resultServerSocket.close();
                    Log.d("MASTER", " Closed server socket.");
                } catch (IOException e) {
                    Log.e("MASTER", "⚠️ Error closing server socket: " + e.getMessage());
                }
                resultServerSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }





    //  Small delay to ensure proper cleanup before restart
    try {
        Thread.sleep(500); // 500ms delay
    } catch (InterruptedException e) {
        Log.e("MASTER", "⚠️ Sleep interrupted: " + e.getMessage());
    }

        // Restart the activity
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }



    private void sendFileToClients(String fileName) {
        long totalStartTime = System.currentTimeMillis();
        long totalStartCpuTime = Helpers.getProcessCpuTime();
        logThread.start();
        // **Partitioning**
        long partitionStartTime = System.currentTimeMillis();
        long partitionStartCpuTime = Helpers.getProcessCpuTime();
        File file = new File(fileName);
        if (!file.exists()) {
            runOnUiThread(() -> tvMessages.append("File not found: " + fileName + "\n"));
            return;
        }
        List<String> subfileNames;
        try {
            subfileNames = FileSplitter.splitTextFileBySize(fileName, computingCapacity);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        long partitionEndTime = System.currentTimeMillis();
        long partitionEndCpuTime = Helpers.getProcessCpuTime();
        long partitionTime = partitionEndTime - partitionStartTime;
        long partitionCpuTime = partitionEndCpuTime - partitionStartCpuTime;

        // **Sending Files**
        long sendStartTime = System.currentTimeMillis();
        long sendStartCpuTime = Helpers.getProcessCpuTime();
        int clientIndex = 0;
        int client_Sockets_size = client_Sockets.size();

        for (Socket socket : client_Sockets.keySet()) {
            try {
                Log.d("MASTER", "index of client: " + clientIndex );
                sendSubfile(socket, subfileNames.get(clientIndex));
                if(clientIndex == 0) startResultListener();
                clientIndex++;
                int percent = (clientIndex*100)/client_Sockets_size;
                String temp = "connected\nFile sending..... " + percent + " % ";
                runOnUiThread(() -> {
                    tvConnectionStatusmb.setText(temp);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        runOnUiThread(() -> {
            tvConnectionStatusmb.setText("connected\nFile sent..... ");
        });

        long receiveTime = System.currentTimeMillis();
        long receiveCpuTime = Helpers.getProcessCpuTime();
        long taskTime = receiveTime - sendStartTime - sending;
        long taskCpuTime = receiveCpuTime - sendStartCpuTime - sendingCPU;



        // **Final Stats**
        long totalEndTime = System.currentTimeMillis();
        long totalEndCpuTime = Helpers.getProcessCpuTime();

        long totalTime = totalEndTime - totalStartTime;
        long totalCpuTime = totalEndCpuTime - totalStartCpuTime;


        double partitionCpuUtilizaztion = Helpers.calculateCPUUtilization(partitionTime,partitionCpuTime );
        double sendingCPUUtilizaztion = Helpers.calculateCPUUtilization(sending,sendingCPU );
        double taskCpuTimeUtilizaztion = Helpers.calculateCPUUtilization(taskTime,taskCpuTime );
        double  totalCpuTimeUtilizaztion = Helpers.calculateCPUUtilization(totalTime,totalCpuTime );
        logThread.stopLogging();

        runOnUiThread(() -> {
            tvMessages.append("\n--- Master Performance Metrics ---\n");
            tvMessages.append("Partition Time: " + partitionTime + " ms, CPU: " + partitionCpuUtilizaztion + " %\n");
            tvMessages.append("Sending Time: " + sending + " ms, CPU: " + sendingCPUUtilizaztion + " %\n");
            tvMessages.append("Task Time: " + taskTime + " ms, CPU: " + taskCpuTimeUtilizaztion + " %\n");
            tvMessages.append("Total Time: " + totalTime + " ms, CPU: " + totalCpuTimeUtilizaztion + " %\n");
            tvMessages.append("Battery Used: " + logThread.powerConsumption + " mWh\n");
        });

        subfileNames.parallelStream().forEach(subfileName -> {
            try {
                boolean deleted = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    deleted = Files.deleteIfExists(Paths.get(subfileName));
                }
                if(deleted){
                    Log.d("MASTER", "File deleted: " + subfileName);
                }else{
                    Log.d("MASTER", "File not found: " + subfileName);
                }
            } catch (Exception e) {
                Log.d("MASTER", "Failed to delete: " + fileName);

                e.printStackTrace();
            }
        });

    }






    private void sendSubfile(Socket clientSocket, String subfileName) throws IOException {
        File file = new File(subfileName);
        if (!file.exists()) {
            runOnUiThread(() -> tvMessages.append("File not found: " + subfileName + "\n"));
            return;
        }


//        if (clientThreads.containsKey(clientSocket)) {
//            Objects.requireNonNull(clientThreads.get(clientSocket)).stopThread();
//            //clientThreads.remove(clientSocket);
//        }

        byte[] buffer = new byte[10000];
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);

        long fileSize = file.length();
        DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

        sendingStart = System.currentTimeMillis();
        sendingStartCPU = Helpers.getProcessCpuTime();

        dos.writeInt(1);  // Send number of files
        dos.writeLong(fileSize); // Send file size
        dos.writeUTF(file.getName()); // Send file name

        int bytesRead;
        while ((bytesRead = bis.read(buffer)) > 0) {
            dos.write(buffer, 0, bytesRead);
        }
        dos.flush();
        bis.close();
        sendingEnd = System.currentTimeMillis();
        sendingEndCPU = Helpers.getProcessCpuTime();;

        sending = sendingEnd - sendingStart;
        sendingCPU = sendingEndCPU - sendingStartCPU;

        runOnUiThread(() -> tvMessages.append("File sent to client: "
                + clientSocket.getInetAddress().getHostAddress() + "\n"));
        // ** NEW: Read the response from the Worker**

//        Log.d("MASTER", "Waiting for response from: " + clientSocket.getInetAddress().getHostAddress());
//        Log.d("MASTER", "time: " + Helpers.getCurrentTimeUTC());
//        DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
//        String workerResponse = dis.readUTF();  // Read message from worker





    }
    private void startResultListener() {
        new Thread(() -> {
            try {

                int portnumber = 5001;
                resultServerSocket = new ServerSocket(portnumber);  // Listen on new port
                Log.d("MASTER", "🟢 Waiting for results on port 5001");

                while (true) {
                    Socket resultSocket = resultServerSocket.accept();  // Accept incoming connection
                    new Thread(() -> handleWorkerResults(resultSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("MASTER", "🚨 Error in result listener: " + e.getMessage());
            }
        }).start();
    }

    private void handleWorkerResults(Socket resultSocket) {
        try {
            DataInputStream dis = new DataInputStream(resultSocket.getInputStream());
            Log.d("MASTER", "🔸 Waiting to receive from Worker...");

            String workerResults = dis.readUTF();  // Read result from Worker
            Log.d("MASTER", "✅ Received from Worker: " + workerResults);
            runOnUiThread(() -> tvMessages.append("Worker response: "
                    + workerResults + "\n"));
            // 🔹 Send acknowledgment back
            DataOutputStream dos = new DataOutputStream(resultSocket.getOutputStream());
            dos.writeUTF("ACK");
            dos.flush();
            Log.d("MASTER", "✅ Sent acknowledgment to Worker");

            resultSocket.close();  // Close the result socket
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MASTER", "🚨 Error receiving results: " + e.getMessage());
        }
    }









    private String getLocalIpAddress() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            tvConnectionStatusmb.append("No IP Address.\n");
            return "No Network";
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            tvConnectionStatusmb.append("No IP Address.\n");
            return "No Network";
        }

        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        if (linkProperties == null) {
            tvConnectionStatusmb.append("No IP Address.\n");
            return "No IP Address";

        }

        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                return address.getHostAddress();  // IPv4 address
            }
        }
        tvConnectionStatusmb.append("No IP Address.\n");
        return "No IPv4 Address";
    }

    class Thread1 implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                runOnUiThread(() -> {
                    tvConnectionStatusmb.setText("Not connected");
                    tvIP.setText("IP: " + SERVER_IP);
                    tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
                });
                Integer clientIndex = 0;
                while (true) {
                    socket = serverSocket.accept();
                    client_Sockets.put(socket,clientIndex);
                    // New client connected

                    //updateClientCountUI(clientSockets.size()); // Update UI with client count
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    runOnUiThread(() -> {
                        tvConnectionStatusmb.setText("Connected clients: " + client_Sockets.size() + "\n"); // Update UI message
                    });

                    Thread2 clientThread = new Thread2(socket);
                    clientThreads.put(socket, clientThread);
                    new Thread(clientThread).start();

//                        new Thread(new Thread2()).start(); // Pass socket to Thread2

                }

            } catch (IOException e) {

                e.printStackTrace();
                runOnUiThread(() -> tvConnectionStatusmb.append("Server error: " + e.getMessage() + "\n"));
            }
        }



    }

//    private void updateClientCountUI(int count) {
//
//    }

    private class Thread2 implements Runnable {
        private final Socket clientSocket;
        private volatile boolean running = true;

        public void stopThread() {
            running = false;
//            try {
//                clientSocket.close(); // Close socket to unblock readLine()
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        public Thread2(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String message;
                while ((message = input.readLine()) != null) {
                    // Extract Speed and IP
                    if (message.contains("Speed:") && message.contains("IP:")) {
                        String speed = extractSpeed(message);
                        String ipAddress = extractIp(message);
                        computingCapacity.put(ipAddress, Double.parseDouble(speed));

                        Log.d("SERVER", "Extracted Speed: " + speed + ", IP: " + ipAddress);
                        runOnUiThread(() -> tvMessages.append("Speed: " + speed + ", IP: " + ipAddress + "\n"));
                    }
                }
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("SERVER", "Thread2 end \n" );
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
}