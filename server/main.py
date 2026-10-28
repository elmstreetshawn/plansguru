from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel
from typing import Dict, Set, List, Optional
import time
import uuid
import os

app = FastAPI(title="PlansGuru Relay Server", version="1.0.0")

# In-memory storage with ephemeral expiration (No persistent DB)
# session_id -> set of active WebSocket connections
active_sessions: Dict[str, Set[WebSocket]] = {}
# invite_code -> invite metadata (expires after 15 mins)
active_invitations: Dict[str, dict] = {}

class InvitationCreate(BaseModel):
    inviter_name: str
    inviter_pub_key: str

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "plansguru-relay"}

@app.post("/api/invitations")
def create_invitation(payload: InvitationCreate):
    code = str(uuid.uuid4())[:8].upper()
    expires_at = time.time() + 900 # 15 minutes
    active_invitations[code] = {
        "inviter_name": payload.inviter_name,
        "inviter_pub_key": payload.inviter_pub_key,
        "expires_at": expires_at
    }
    return {"invite_code": code, "expires_at": expires_at}

@app.get("/api/invitations/{code}")
def get_invitation(code: str):
    invite = active_invitations.get(code)
    if not invite or time.time() > invite["expires_at"]:
        if code in active_invitations:
            del active_invitations[code]
        raise HTTPException(status_code=404, detail="Invitation expired or not found")
    return invite

@app.websocket("/ws/session/{session_id}")
async def websocket_relay(websocket: WebSocket, session_id: str):
    await websocket.accept()
    if session_id not in active_sessions:
        active_sessions[session_id] = set()
    active_sessions[session_id].add(websocket)

    try:
        while True:
            data = await websocket.receive_text()
            # Relay raw encrypted message payload to all other peers in the session
            # Server never decrypts or persists message history.
            if session_id in active_sessions:
                for client in active_sessions[session_id]:
                    if client != websocket:
                        await client.send_text(data)
    except WebSocketDisconnect:
        if session_id in active_sessions:
            active_sessions[session_id].discard(websocket)
            if not active_sessions[session_id]:
                del active_sessions[session_id]

MODEL_FILE_NAME = "gemma3-1b-it-int4.litertlm"

@app.get("/api/model/gemma")
def download_gemma_model():
    search_paths = [
        MODEL_FILE_NAME,
        os.path.join(os.path.dirname(__file__), MODEL_FILE_NAME),
        "gemma-4-E2B-it-web.task",
        os.path.join(os.path.dirname(__file__), "gemma-4-E2B-it-web.task")
    ]

    found_path = None
    for p in search_paths:
        if os.path.exists(p):
            found_path = p
            break

    if not found_path:
        server_dir = os.path.dirname(__file__) or "."
        for f in os.listdir(server_dir):
            if f.endswith(".litertlm") or f.endswith(".task"):
                found_path = os.path.join(server_dir, f)
                break

    if not found_path:
        raise HTTPException(status_code=404, detail="Gemma model file (.litertlm / .task) not found in server directory.")

    filename = os.path.basename(found_path)
    return FileResponse(found_path, media_type="application/octet-stream", filename=filename)
