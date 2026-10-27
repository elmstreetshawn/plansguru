# PlansGuru — E2E Encrypted Group Messaging & Topic Routing Architecture

---

## The Challenge: "How is there a recipientId if we don't know who is in the group?"

In a decentralized group chat (like a dinner planning session with friends who might not all know each other), you don't send individual direct messages to every person. Instead, you route messages through a **Group Topic / Session ID** using **Symmetric Group Encryption** (the standard Signal/Matrix/WhatsApp protocol).

---

## The Solution: Pub/Sub Topic Routing & Group Session Keys

### 1. Group Session Key Generation (Hub-and-Spoke / Coordinator)
- When you create a dinner plan (e.g., `session_taco_crew_987`), your device generates a random, cryptographically secure **Group Session Key** (`AES-256-GCM`).
- Your device distributes this Group Session Key to each participant (Maya, Jo, Priya) via pairwise E2E encryption (`recipientId: participant_pubkey`).

### 2. Message Encryption & Fan-Out
- **Client Encryption:** When you send a message in the group chat, your app encrypts the message *once* using the **Group Session Key**.
- **Topic Addressing:** The packet is wrapped in a `ProtocolMessage` addressed to a group topic:
  ```json
  {
    "type": "GROUP_MESSAGE",
    "senderId": "local_user_pubkey",
    "recipientId": "group:session_taco_crew_987",
    "encryptedPayload": "AES-256-GCM ciphertext encrypted with Group Session Key",
    "signature": "ECDSA signature",
    "timestamp": 1698345600000
  }
  ```

### 3. Python Server Fan-Out (Blind Pub/Sub Relay)
- The Python server manages a pub/sub room mapping for `group:session_taco_crew_987`.
- When the server receives a message addressed to `group:session_taco_crew_987`, it **fans out (broadcasts)** that exact ciphertext to all WebSocket connections subscribed to that session.
- **Zero Knowledge:** Because the payload is encrypted with the Group Session Key, the Python server cannot read the messages—it only acts as a blind postal carrier forwarding ciphertexts to subscribers.
- **Decryption:** Each participant's device receives the ciphertext, unlocks it with their local copy of the Group Session Key, and displays the chat message.
