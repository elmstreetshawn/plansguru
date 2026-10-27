# PlansGuru — Backend Tunnel & Transitive Trust Architecture

---

## Part 1: Python Backend Tunnel & E2E Encrypted Sync Protocol

To connect the PlansGuru Android app to a Python backend server for end-to-end encrypted (E2E) message routing and social graph synchronization without compromising user privacy:

### 1. Cryptographic Envelope (Zero-Knowledge Server)
- **Zero-Knowledge Relay:** The Python server acts strictly as a blind packet relay (`/api/v1/relay`). It never holds decryption keys.
- **Payload Structure (`ProtocolMessage`):**
  ```json
  {
    "type": "PLAN_REQUEST",
    "senderId": "local_user_pubkey",
    "recipientId": "peer_pubkey",
    "encryptedPayload": "AES-GCM-256 ciphertext encrypted with recipient's public key",
    "signature": "ECDSA signature over ciphertext",
    "timestamp": 1698345600000
  }
  ```

### 2. Tunnel Implementation (OkHttp & WebSockets)
- **WebSocket Transport:** Use OkHttp WebSocket in Android connected to the Python FastAPI/WebSockets server (`wss://api.plansguru.net/ws`).
- **Local SQLite Sync Queue:** When offline, outbound encrypted graph updates and plan requests are queued in SQLCipher (`synchronizationStatus = 'PENDING'`). Upon tunnel reconnection, the queue flushes automatically.

---

## Part 2: The Transitive Trust Problem ("4 People Who Don't Know Each Other")

### The Challenge
You initiate a dinner plan with 4 friends (Alice, Bob, Charlie, Diana). You have verified trust (`VERIFIED`) with each of them individually, but Alice has never met Bob, Charlie, or Diana. How does the group negotiate consensus securely?

### The Architectural Solution: Hub-and-Spoke Federated Coordination (Trusted Coordinator)

1. **You Are the Secure Bridge (Coordinator Node):**
   - Because you are connected to all 4 participants and hold verified pairwise trust with each, your device acts as the **Secure Federated Coordinator**.
2. **Blind Preference Aggregation:**
   - Instead of broadcasting raw preferences across strangers, each participant sends their encrypted preference assertions (`Assertion`) *only* to you (the coordinator).
3. **Decentralized Multi-Party Computation (MPC) / Local Negotiation:**
   - Your local `NegotiationEngine` runs the multi-variable scoring algorithm across all participants' assertions in a secure enclave/sandbox.
4. **Consensus Proposal Broadcast:**
   - Once consensus is reached (e.g., *Trattoria Bella* with 95% group match), your device generates a cryptographically signed `AgentProposal` and broadcasts it to Alice, Bob, Charlie, and Diana.
5. **Progressive Key Exchange:**
   - Upon accepting the dinner plan, participants receive each other's ephemeral group session keys, establishing a secure multi-party channel for the event without requiring prior direct connections.
