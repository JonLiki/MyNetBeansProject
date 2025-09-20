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
