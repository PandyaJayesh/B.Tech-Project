# Distributed Word Count System 📜⚡

This project implements a **distributed word count system** using **smartphones as computing nodes**. It supports **two partitioning approaches** for distributing text files among worker devices:

1. **Equal Partitioning** – Splits the file equally among all available workers.
2. **Balanced Partitioning** – Splits the file based on each worker’s computing capacity.

The system measures **execution time, CPU usage, and battery consumption** for performance evaluation.

---

## 📌 Features
- **Master-Worker Architecture** 🖥️📱
- **Two Partitioning Methods**
  - 🔹 **Equal Partition** (Fixed-size chunks)
  - 🔹 **Balanced Partition** (Capacity-based distribution)
- **Performance Metrics**
  - ⏱ **Time Taken**
  - ⚡ **CPU Usage**
  - 🔋 **Battery Consumption**
- **Multi-Device Computing** 📶

---

## 📂 Project Structure
    /app/src/main/java/com/example/wordcount/
        │-- MainActivity.java # App entry point (Select Equal  or Balanced partition)     
        │-- EqualActivity.java # UI for Equal Partition mode 
        │-- BalancedActivity.java # UI for Balanced Partition mode 
        │-- master_worker/ 
        │ 
        │-- MasterActivity.java # Equal Partition Master Node 
        │ 
        │-- WorkerActivity.java # Equal Partition Worker Node 
        │-- balanced_master_worker/ 
        │ 
        │-- MasterBalancedActivity.java # Balanced Partition Master Node 
        │ 
        │-- WorkerBalancedActivity.java # Balanced Partition Worker Node 
        │-- utils/ 
        │ 
        │-- FileSplitter.java # Handles file splitting logic 
        │ 
        │-- WordCount.java # Performs word count operation




---

## 🚀 How It Works
### **1️⃣ Select Partitioning Method**
- On app start, choose **Equal Partition** or **Balanced Partition**.

### **2️⃣ Master Node Execution**
- The **master node** reads a text file and:
  - **Splits** it into smaller chunks.
  - **Distributes** these chunks to multiple worker devices.
  - **Receives** processed word count results from workers.

### **3️⃣ Worker Node Execution**
- The **worker node**:
  - **Receives** the file chunk.
  - **Counts** words in the chunk.
  - **Sends** the result back to the master.

### **4️⃣ Performance Evaluation**
- After processing, the master displays:
  - 🔹 **Partitioning Time**
  - 🔹 **Sending Time**
  - 🔹 **Receiving Time**
  - 🔹 **Total Execution Time**
  - 🔹 **CPU Usage**
  - 🔹 **Battery Consumption**

---

# 🛠️ Technologies Used
#### Java (Android SDK)
#### Sockets (TCP/IP) for device communication
#### Multithreading for background processing
#### Battery & CPU Monitoring via Android APIs
# 👨‍🏫 Project & Research Context
This project is developed as part of B.Tech research work under the guidance of Prof. Kalyan Sasidhar.

It is inspired by: 

🔗[Distributed-Smartphone-Computing-Server](https://github.com/harshmistry3172/Distributed-Smartphone-Computing-Server)

 
🔗 [Distributed-Smartphone-Computing-Client](https://github.com/harshmistry3172/Distributed-Smartphone-Computing-Client)


🎯 Contributors
[Jayesh Pandya](https://github.com/PandyaJayesh)

[Prof. Kalyan Sasidhar] (Project Guide)
🚀 Feel free to fork, contribute, or suggest improvements! 🤝

