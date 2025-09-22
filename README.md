<<<<<<< HEAD
# Leader Election Protocol using Java RMI

## Overview
This project implements a **Leader Election Protocol** in a distributed system using **Java RMI (Remote Method Invocation)**.  
The goal is to simulate how nodes in a distributed network can communicate with each other and elect a leader using message passing.

## Project Structure
The main files in this project are:

- **Node.java**  
  Defines the `Node` interface for RMI communication.

- **NodeImpl.java**  
  Implements the `Node` interface and handles election logic.

- **PeerRegister.java**  
  Defines the interface for registering peers in the network.

- **PeerRegisterImpl.java**  
  Implements the peer registry for managing nodes.

## Features
- Ring topology communication between nodes.
- Implementation of **Leader Election Algorithm** (LCR - Le Lann, Chang and Roberts algorithm).
- Fault-tolerant design, where nodes can fail and elections are retriggered.
- RMI-based communication between distributed peers.

## Requirements
- Java Development Kit (JDK) 8 or later
- NetBeans IDE (or any Java IDE)
- Git (for version control)

## How to Run
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/LeaderElectionRMI.git
   cd LeaderElectionRMI
   ```

2. Open the project in **NetBeans IDE** (or another IDE).

3. Start the RMI registry:
   ```bash
   rmiregistry 1099
   ```

4. Compile and run the server (PeerRegisterImpl).

5. Start multiple node instances (NodeImpl) to form the ring.

6. Trigger the election from one of the nodes. The elected leader will be broadcast to all nodes.

## Example Workflow
- Start 4 nodes (Node A, Node B, Node C, Node D).
- One node triggers an election.
- Messages circulate around the ring.
- The node with the highest ID becomes the leader.
- Leader announcement circulates to all nodes.

## .gitignore (Recommended)
```
/build/
/dist/
/nbproject/private/
/*.class
```

## Author
Developed by **Sione Likiliki**  
Bachelor of Network and Security (Year 2)

---
This project is for educational purposes as part of learning **Distributed Systems** and **RMI-based communication**.
=======
# 📘 LCR Leader Election with Centralized Registration

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8+-blue.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-orange.svg)](https://maven.apache.org/)

A distributed systems project implementing the **Le Lann–Chang–Roberts (LCR) Leader Election Algorithm** in a unidirectional ring topology. This version adds a centralized **Peer Register Service** to coordinate node registration and fault handling via **Java RMI**.

---

## ✨ Features

- **Leader Election (LCR)**: Correct Chang–Roberts variant for unidirectional rings  
- **Centralized Registration**: Avoids race conditions with a `PeerRegister`  
- **Fault Tolerance**: Failure simulation, recovery, and re-election  
- **Interactive CLI**: Commands for election, monitoring, failures, recovery  
- **Dynamic Topology**: Ring rebuilds on join/leave  
- **Heartbeat Monitoring**: Detects leader crashes and retriggers election  
- **Colorized Logging**: Easy-to-follow, timestamped console output  

---

## 🏗️ Architecture

### Components
| Component       | Role                                          | Port         |
|-----------------|-----------------------------------------------|--------------|
| **PeerRegister** | Central registry for ring management          | `1099` (RMI) |
| **Node**        | Distributed process implementing LCR          | Dynamic RMI  |
| **Ring**        | Unidirectional logical ring managed centrally | —            |

### Workflow
```
Register → Ring Setup → Election → Leader Propagation → Acceptance
```
<img width="272" height="928" alt="image" src="https://github.com/user-attachments/assets/8332a880-eae8-4b6e-9388-aded0c0c6c97" />



---

## 🚀 Quick Start

### Prerequisites
- **Java 8+** (tested on OpenJDK 11/17)  
- **Maven 3.6+**  
- A terminal supporting **ANSI colors**

### Installation
```bash
git clone https://github.com/yourusername/election-with-register.git
cd election-with-register
mvn clean compile
```

### Running
1. Start the Peer Register:
   ```bash
   java -cp target/classes cs324.election.with.register.PeerRegisterImpl
   ```
2. Start nodes in separate terminals:
   ```bash
   java -cp target/classes cs324.election.with.register.NodeImpl 5
   java -cp target/classes cs324.election.with.register.NodeImpl 11
   java -cp target/classes cs324.election.with.register.NodeImpl 2
   java -cp target/classes cs324.election.with.register.NodeImpl 7
   ```

---

## 🎮 Commands

At a node prompt (`Node 005 >`):

| Command   | Action                        |
|-----------|-------------------------------|
| `start`   | Start election                |
| `leader`  | Show current leader           |
| `kill`    | Simulate node failure         |
| `recover` | Recover node                  |
| `status`  | Display node status           |
| `debug`   | Verbose report                |
| `reset`   | Reset election state          |
| `exit`    | Shut down node                |

---

## 📸 Demo

### Election
![Election Demo Placeholder](screenshots/election-demo.gif)

1. Node 5 starts an election.  
2. UID 11 wins as leader.  
3. All nodes accept and acknowledge leader.  

### Failure & Recovery
- Kill node mid-election → ring rebuilds  
- Recover node → automatic re-election triggered  

---

## 📚 Algorithm

**Chang–Roberts (LCR) Variant**

- Initiator sends `<candidate=ID, origin=ID>`  
- Highest UID propagates around ring  
- When UID returns, leader is elected  
- Complexity:  
  - Best: **O(N)** messages  
  - Worst: **O(N²)** messages  

---

## 🛠️ Troubleshooting

| Problem              | Fix                                |
|----------------------|------------------------------------|
| `Connection refused` | Start PeerRegister first           |
| Duplicate node IDs   | Use unique UIDs (2,5,7,11, etc.)   |
| Election timeout     | Increase `ELECTION_TIMEOUT` value  |
| No color output      | Switch to VS Code, iTerm, etc.     |

---

## Group Members (CS324)

- Sione Likiliki
- Seiloni Utuone
- Fasi Tangataevaha
- Lanuongo Guttenbeil

Assignment: LCR Leader Election with Centralized Registration.

---

## 🤝 Contributing

1. Fork the repo  
2. Create a feature branch (`git checkout -b feature/foo`)  
3. Commit changes (`git commit -m 'Add feature foo'`)  
4. Push branch (`git push origin feature/foo`)  
5. Open a Pull Request  

Style Guide:
- [Google Java Style](https://google.github.io/styleguide/javaguide.html)  
- 100-char line limit  
- Javadoc for public methods  

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file.  

---

## 📖 References

- Chang, E., & Roberts, R. (1979). “An improved algorithm for decentralized extrema-finding in circular configurations of processes.” *Communications of the ACM*.  
- Tanenbaum, A. S., & Van Steen, M. (2017). *Distributed Systems: Principles and Paradigms*.  

---

🔥 Built with passion for distributed systems education.  
>>>>>>> b212495cb36aa8506ae91e6d0c9887d719d73088
