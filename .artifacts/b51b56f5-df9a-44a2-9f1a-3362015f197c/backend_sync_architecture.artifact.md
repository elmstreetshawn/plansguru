# PlansGuru — Python Backend Server & E2E Encrypted Sync Architecture

---

## 1. Passing Messages Through the Server (Blind Relay)
Yes! Every E2E encrypted group message and social graph update flows through the Python backend server, but **the server acts strictly as a blind relay**.

### The Flow:
1. **Client Encryption:** Your Android app encrypts the chat message or social graph assertion using the recipient's public key (AES-GCM-256) and signs it with your ECDSA private key (`ProtocolMessage`).
2. **Transmission:** The app transmits the encrypted envelope to the Python server via WebSocket (`wss://api.plansguru.net/ws`) or HTTP POST (`/api/v1/relay`).
3. **Blind Routing:** The Python server inspects only the envelope metadata (`recipientId`) and routes the ciphertext directly to the recipient without ever decrypting or reading the content.

---

## 2. Pulling & Sharing with Other Users

To sync and share data across users in the ecosystem through the server:

### A. Real-Time Push (WebSockets)
- When a user is online, the Python server maintains an active WebSocket connection.
- When User A sends a message to the group, the server instantly pushes the encrypted packet down the WebSocket tunnel to User B, User C, and User D.

### B. Offline Sync Queue (Pull & Push REST API)
- **Outbound Queue:** If a user is offline, outbound encrypted messages and graph assertions are queued in SQLCipher (`synchronizationStatus = 'PENDING'`). Upon reconnection, the app pushes the batch to `POST /api/v1/sync`.
- **Inbound Pull:** When opening the app or joining a plan, the client requests any pending encrypted messages from `GET /api/v1/sync?since=timestamp`.

---

## 3. Python Server Code Blueprint (FastAPI + WebSockets)

```python
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from pydantic import BaseModel
from typing import Dict, List

app = FastAPI()

class ProtocolMessage(BaseModel.model_config):
    type: str
    senderId: str
    recipientId: str
    encryptedPayload: str
    signature: str
    timestamp: int

# Active WebSocket connections mapped by public userId
active_connections: Dict[str, WebSocket] = {}

@app.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: str):
    await websocket.accept()
    active_connections[user_id] = websocket
    try:
        while True:
            data = await websocket.receive_text()
            # Parse message metadata to find recipient
            # Blindly relay encrypted payload to recipient if online
            # If offline, store in server's encrypted pending queue for later pull
    except WebSocketDisconnect:
        del active_connections[user_id]
```
