from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from fastapi.responses import FileResponse, HTMLResponse
from pydantic import BaseModel
from typing import Dict, Set, List, Optional
import time
import uuid
import os

app = FastAPI(title="PlansGuru Relay & Distribution Server", version="1.0.0")

# In-memory storage with ephemeral expiration (No persistent DB)
active_sessions: Dict[str, Set[WebSocket]] = {}
active_invitations: Dict[str, dict] = {}
registered_users: Dict[str, dict] = {}  # userId -> {publicKey, displayName, createdAt}

class InvitationCreate(BaseModel):
    inviter_name: str
    inviter_pub_key: str

class RegisterUserRequest(BaseModel):
    userId: str
    publicKey: str
    displayName: str

class RegisterUserResponse(BaseModel):
    status: str
    userId: str
    message: Optional[str] = None

@app.get("/", response_class=HTMLResponse)
def landing_page():
    html_content = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>PlansGuru • On-Device AI Distribution Server</title>
        <style>
            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #121016; color: #E6E1E5; margin: 0; padding: 24px; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 90vh; }
            .card { background: #1D1B20; border-radius: 24px; padding: 32px; max-width: 480px; width: 100%; box-shadow: 0 8px 24px rgba(0,0,0,0.4); border: 1px solid #49454F; text-align: center; }
            h1 { color: #D0BCFF; margin-top: 0; font-size: 28px; }
            p { color: #CAC4D0; font-size: 15px; line-height: 1.5; margin-bottom: 24px; }
            .btn { display: inline-block; background: #D0BCFF; color: #381E72; font-weight: bold; padding: 16px 28px; border-radius: 100px; text-decoration: none; font-size: 16px; margin: 8px 0; transition: transform 0.2s; width: 80%; }
            .btn:active { transform: scale(0.98); }
            .badge { display: inline-block; background: #2B2930; color: #E8DEF8; padding: 8px 16px; border-radius: 100px; font-size: 13px; margin-top: 16px; border: 1px solid #49454F; }
        </style>
    </head>
    <body>
        <div class="card">
            <h1>🌮 PlansGuru</h1>
            <p>Privacy-First On-Device AI Dinner Coordination & Encrypted Social Graph for Greater Austin, TX.</p>

            <a href="/api/apk/download" class="btn">📲 Download PlansGuru APK (v1.1.12)</a>
            <a href="/api/model/gemma" class="btn" style="background: #E8DEF8; color: #1D192B;">🤖 Download Gemma 3 Model (584 MB)</a>

            <div class="badge">
                ⚡ Version 1.1.12 (Build 12) • Server Online
            </div>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=html_content)

@app.get("/join", response_class=HTMLResponse)
def join_redirect(inviter: str = "Friend", pubkey: Optional[str] = None, name: Optional[str] = None, group: str = "Dinner Plan"):
    """Handle plan join deep link redirects with support for UUID-based identity."""
    # Use name parameter if provided (display name), fallback to inviter
    display_name = name or inviter
    app_deep_link = f"plansguru://plan/join?inviter={inviter}&pubkey={pubkey or ''}&name={display_name}&group={group}"

    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>Joining PlansGuru...</title>
        <script>
            window.location.href = "{app_deep_link}";
            setTimeout(function() {{
                window.location.href = "/api/apk/download";
            }}, 2500);
        </script>
        <style>
            body {{ font-family: sans-serif; background: #121016; color: #E6E1E5; text-align: center; padding: 40px; }}
            .card {{ background: #1D1B20; padding: 24px; border-radius: 16px; display: inline-block; border: 1px solid #49454F; }}
            a {{ color: #D0BCFF; font-weight: bold; }}
        </style>
    </head>
    <body>
        <div class="card">
            <h2>Opening PlansGuru App...</h2>
            <p>Joining <b>{group}</b> invited by <b>{display_name}</b>.</p>
            <p><a href="{app_deep_link}">Tap here to open PlansGuru App</a></p>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=html_content)

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "plansguru-relay"}

@app.post("/api/register", response_model=RegisterUserResponse)
def register_user(request: RegisterUserRequest):
    """Register a new user with their public key for identity verification."""
    userId = request.userId.strip()
    publicKey = request.publicKey.strip()
    displayName = request.displayName.strip()

    if not userId or not publicKey or not displayName:
        raise HTTPException(status_code=400, detail="userId, publicKey, and displayName are required")

    if len(userId) < 8:
        raise HTTPException(status_code=400, detail="userId must be at least 8 characters")

    if len(publicKey) < 16:
        raise HTTPException(status_code=400, detail="publicKey must be at least 16 characters")

    # Check if user already registered
    if userId in registered_users:
        existing = registered_users[userId]
        if existing["publicKey"] == publicKey:
            # Same key, idempotent registration
            return RegisterUserResponse(
                status="success",
                userId=userId,
                message="User already registered with this public key"
            )
        else:
            # Different key, potential security issue
            raise HTTPException(
                status_code=409,
                detail="User ID already registered with a different public key"
            )

    # Register new user
    registered_users[userId] = {
        "publicKey": publicKey,
        "displayName": displayName,
        "createdAt": time.time()
    }

    return RegisterUserResponse(
        status="success",
        userId=userId,
        message="User registered successfully"
    )

@app.get("/api/users/{user_id}/publickey")
def get_user_public_key(user_id: str):
    """Look up a user's public key by their user ID."""
    user_id = user_id.strip()

    if not user_id:
        raise HTTPException(status_code=400, detail="user_id is required")

    user = registered_users.get(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    return {"publicKey": user["publicKey"]}

@app.get("/api/users/{user_id}")
def get_user_info(user_id: str):
    """Get user information (public key and display name)."""
    user_id = user_id.strip()

    if not user_id:
        raise HTTPException(status_code=400, detail="user_id is required")

    user = registered_users.get(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    return {
        "userId": user_id,
        "publicKey": user["publicKey"],
        "displayName": user["displayName"],
        "createdAt": user["createdAt"]
    }

@app.get("/api/health/users")
def get_user_stats():
    """Get server statistics about registered users."""
    return {
        "status": "healthy",
        "service": "plansguru-user-registry",
        "registeredUsers": len(registered_users),
        "activeSessions": len(active_sessions),
        "timestamp": time.time()
    }

@app.get("/api/apk/download")
@app.get("/download")
def download_apk():
    apk_paths = [
        os.path.join(os.path.dirname(__file__), "android-debug.apk"),
        os.path.join(os.path.dirname(__file__), "..", "android", "build", "outputs", "apk", "debug", "android-debug.apk")
    ]

    found_apk = None
    for p in apk_paths:
        if os.path.exists(p):
            found_apk = p
            break

    if not found_apk:
        raise HTTPException(status_code=404, detail="APK file not found on server. Please build 'android:assembleDebug' first.")

    return FileResponse(found_apk, media_type="application/vnd.android.package-archive", filename="PlansGuru.apk")

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
