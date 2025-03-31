package com.example.wordcount;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MasterActivity extends AppCompatActivity {
    static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // ExecutorService for background tasks
    ServerSocket serverSocket;
    long sendingStart = 0;
    long sendingEnd = 0;
    long sending = 0;
    long sendingStartCPU = 0;
    long sendingEndCPU = 0;
    long sendingCPU = 0;
    Thread Thread1 = null;
    Helpers.LogThread logThread = null;
    TextView tvIP, tvPort;
    TextView tvMessages;
    TextView tvConnectionStatus;
    Button btnSend;
    Button btnReset;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 8080;
    Set<Socket> clientSockets = new HashSet<>(); // Set to store active client sockets

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        tvIP = findViewById(R.id.tvIP);
        tvPort = findViewById(R.id.tvPort);
        tvMessages = findViewById(R.id.tvMessages);
        btnSend = findViewById(R.id.btnSend);
        btnReset = findViewById(R.id.btnReser);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);

        // Request permissions at startup
        requestStoragePermissions();

        SERVER_IP = getLocalIpAddress();

        Thread1 = new Thread(new Thread1());
        Thread1.start();
        logThread = new Helpers.LogThread(this, 200,tvMessages); // Print value every 0.2 seconds


        btnReset.setOnClickListener(v -> restartApp());

        btnSend.setOnClickListener(v -> {


            String fileName = Environment.getExternalStorageDirectory().getPath() + "/testing.txt";
//            File file = new File(this.getExternalFilesDir(null), "testing.txt");
//            String fileName = file.getAbsolutePath();

            executorService.execute(() -> sendFileToClients(fileName));


        });
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {  // Android 11+
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {  // Android 10 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // User permanently denied permission, open settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(this, "Enable storage permission manually", Toast.LENGTH_LONG).show();
                    return;
                }

                // Request storage permission
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CODE_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    break;
                }
            }
        }
    }



    public void restartApp() {
        try {
            for (Socket socket : clientSockets) {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
            logThread.stopLogging();
            clientSockets.clear(); // Clear the list
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            subfileNames = FileSplitter.splitTextFile(fileName, clientSockets.size());
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
        int client_Sockets_size = clientSockets.size();
        int clientIndex = 0;
        for (Socket clientSocket : clientSockets) {
            try {
                sendSubfile(clientSocket, subfileNames.get(clientIndex));
                clientIndex++;
                int percent = (clientIndex*100)/client_Sockets_size;
                String temp = "connected\nFile sending..... " + percent + " % ";
                runOnUiThread(() -> {
                    tvConnectionStatus.setText(temp);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        runOnUiThread(() -> {
            tvConnectionStatus.setText("connected\nFile sent..... ");
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
        logThread.stopLogging();

        double partitionCpuUtilizaztion = Helpers.calculateCPUUtilization(partitionTime,partitionCpuTime );
        double sendingCPUUtilizaztion = Helpers.calculateCPUUtilization(sending,sendingCPU );
        double taskCpuTimeUtilizaztion = Helpers.calculateCPUUtilization(taskTime,taskCpuTime );
        double  totalCpuTimeUtilizaztion = Helpers.calculateCPUUtilization(totalTime,totalCpuTime );


        runOnUiThread(() -> {
            tvMessages.append("\n--- Master Performance Metrics ---\n");
            tvMessages.append("Partition Time: " + partitionTime + " ms, CPU: " + partitionCpuUtilizaztion + " %\n");
            tvMessages.append("Sending Time: " + sending + " ms, CPU: " + sendingCPUUtilizaztion + " %\n");
            tvMessages.append("Task Time: " + taskTime + " ms, CPU: " + taskCpuTimeUtilizaztion + " %\n");
            tvMessages.append("Total Time: " + totalTime + " ms, CPU: " + totalCpuTimeUtilizaztion + " %\n");
            tvMessages.append("Battery Used: " + logThread.powerConsumption + " mWh\n");
        });

//        for(String filename :subfileNames){
//            if (filename != null && filename.exists()) {
//                boolean deleted = filename.delete();
//                Log.d("CLIENT", "File deleted: " + deleted);
//            }
//        }


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

        sendingStart = System.currentTimeMillis();
        sendingStartCPU = Helpers.getProcessCpuTime();
        dos.writeInt(1);  // Send number of files
        dos.writeLong(fileSize); // Send file size
        dos.writeUTF(file.getName()); // Send file name
        Log.e("MASTER", "Sent file name" + file.getName());
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
        // **🔹 NEW: Read the response from the Worker**
        DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
        String workerResponse = dis.readUTF();  // Read message from worker

        runOnUiThread(() -> tvMessages.append( "Worker Response: " + workerResponse + "\n"));
        String[] parts = workerResponse.split(" ");
        return Long.parseLong(parts[3]); // Extract the 4th word (index 3)
    }


    private String getLocalIpAddress() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            tvConnectionStatus.append("No IP Address.\n");
            return "No Network";
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            tvConnectionStatus.append("No IP Address.\n");
            return "No Network";
        }

        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        if (linkProperties == null) {
            tvConnectionStatus.append("No IP Address.\n");
            return "No IP Address";

        }

        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                return address.getHostAddress();  // IPv4 address
            }
        }
        tvConnectionStatus.append("No IP Address.\n");
        return "No IPv4 Address";
    }

    class Thread1 implements Runnable {
        @Override
        public void run() {
            try {

                serverSocket = new ServerSocket(SERVER_PORT);

                // Move this UI update BEFORE accept() so it gets executed
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Not connected");
                    tvIP.setText("IP: " + SERVER_IP);
                    tvPort.setText("Port: " + SERVER_PORT);
                });

                // Now enter the loop for accepting clients
                while (true) {
                    Socket socket = serverSocket.accept(); // Blocks here until a client connects
                    if (clientSockets.add(socket)) {
                        runOnUiThread(() -> tvConnectionStatus.setText("Connected clients: " + clientSockets.size() + "\n"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> tvConnectionStatus.append("Server error: " + e.getMessage() + "\n"));
            }
        }
    }



}
