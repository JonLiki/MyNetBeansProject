# LCR Leader Election with Centralized Registration using Java RMI

Java Maven

This project implements the Le Lann–Chang–Roberts (LCR) leader election algorithm over a ring topology using Java RMI, enhanced with a centralized `PeerRegister` service for node registration, dynamic ring management, and fault tolerance.

## Overview

This project demonstrates distributed leader election using the LCR algorithm with centralized coordination to handle dynamic node joins, failures, and recoveries. It is designed as a coursework assignment for distributed systems, showing how nodes in a ring topology coordinate through message passing over Java RMI, with added resilience features like heartbeat monitoring and automatic ring rebuilding.

## Requirements

- Java 11 or newer  
- NetBeans or any Java IDE (optional)  
- 5 terminals (or processes): 1 for PeerRegister, 4 for nodes  
- Classes compiled to `target/classes`  

## Setup

0. Install Java 11+ and ensure `java` and `javac` are on your PATH.  
1. Compile the source code:

```bash
javac -d target/classes src/main/java/cs324/election/with/register/*.java
```

## Visual Feedback (Color & Emoji Support)

For best presentation results, enable UTF-8 output in each terminal before starting the components. This ensures that ANSI colors and emojis display correctly.

In PowerShell, run:

```powershell
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

Now when you run the PeerRegister and nodes, log messages will include color-coded text and emojis for clearer visual feedback.

## Running the Simulation

Open 5 terminals: one for the PeerRegister and four for the nodes.

### Terminal 1 (PeerRegister)
```bash
cd <project-root>
java -cp target/classes cs324.election.with.register.PeerRegisterImpl
```
<img width="1349" height="174" alt="image" src="https://github.com/user-attachments/assets/06d01129-a59b-4a3d-827e-ce71f153b27f" />


### Terminal 2 (Node 5)
```bash
cd <project-root>
java -cp target/classes cs324.election.with.register.NodeImpl 5
```
<img width="730" height="224" alt="image" src="https://github.com/user-attachments/assets/714c472a-5a9e-42e1-8a82-3fee2663aac2" />


### Terminal 3 (Node 11)
```bash
cd <project-root>
java -cp target/classes cs324.election.with.register.NodeImpl 11
```
<img width="697" height="251" alt="image" src="https://github.com/user-attachments/assets/bb0b229e-dcde-4dbc-802a-8de10840aa8f" />


### Terminal 4 (Node 2)
```bash
cd <project-root>
java -cp target/classes cs324.election.with.register.NodeImpl 2
```
<img width="731" height="239" alt="image" src="https://github.com/user-attachments/assets/26c53b0a-142b-4290-87be-510082f51e9f" />


### Terminal 5 (Node 7)
```bash
cd <project-root>
java -cp target/classes cs324.election.with.register.NodeImpl 7
```
<img width="705" height="230" alt="image" src="https://github.com/user-attachments/assets/ae61b694-1209-4690-a8c1-a4ce48e0e8da" />


1. The PeerRegister will start and wait for registrations. Nodes will register automatically and form the ring.
2. Each node terminal will present an interactive prompt.
   
  <img width="1919" height="1031" alt="image" src="https://github.com/user-attachments/assets/edb19ca3-b261-4e7e-8991-f25215769625" />

3. On any node terminal, type:

```bash
start
```

to begin the leader election.

4. Messages will circulate around the ring.  
5. At the end, the node with the highest ID will declare itself as leader and broadcast this result.

## Example Output

```text
[Node-5] initiating election with UID=5
[Node-11] received ELECTION(5), replaces with 11
[Node-2] received ELECTION(11), forwards unchanged
[Node-7] received ELECTION(11), forwards unchanged
[Node-11] received its own UID -> declares LEADER
[Node-11] broadcasting LEADER(11)
All nodes: leaderId = 11, electionCompleted = true
```

<img width="1919" height="1031" alt="image" src="https://github.com/user-attachments/assets/2c941fd8-d8d0-4a5d-8aba-08c65917525a" />


## Notes

- The PeerRegister runs on port 1099 and manages the ring topology.  
- Nodes must be started after the PeerRegister.  
- Only unique IDs are valid. Duplicate IDs break the algorithm.  
- The system supports fault tolerance: simulate failure with `kill` and recovery with `recover`.  
- Heartbeat monitoring automatically detects leader failures and triggers re-elections.  

## Commands (interactive prompt)

- `start` – start an election  
- `leader` – show current leader  
- `kill` – simulate node failure  
- `recover` – recover node  
- `status` – display node status  
- `debug` – verbose report  
- `reset` – reset election state  
- `help` – show commands  
- `exit` – shut down node  

## Algorithm Summary

- **Election rule**: Forward the larger of (incoming UID, local ID).  
- **Leader detection**: When a node receives its own UID back, it declares itself leader.  
- **Leader announcement**: The leader sends a `LEADER(id)` message around the ring until it returns.  
- **Enhancements**: Centralized registration pauses during elections; heartbeats trigger ring rebuilds on failure.  

**Complexity**: O(n²) messages for the election plus O(n) for the leader announcement.

## Troubleshooting

- `Connection refused`: Ensure PeerRegister is running on port 1099.  
- `NotBoundException`: Verify node names/IDs and start order.  
- Election timeout: Increase `ELECTION_TIMEOUT` value.  
- No color output: Enable UTF-8 in terminals.  
- Re-election fails: Ensure all nodes are alive; check heartbeat interval.  

## Group Members (CS324)

- Sione Likiliki  
- Seiloni Utuone  
- Fasi Tangataevaha  
- Lanuongo Guttenbeil  

Assignment: LCR Leader Election with Centralized Registration._

## License

For academic use in distributed systems coursework.
