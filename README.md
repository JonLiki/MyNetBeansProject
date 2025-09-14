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
