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

class InvitationCreate(BaseModel):
    inviter_name: str
    inviter_pub_key: str

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

            <a href="/api/apk/download" class="btn">📲 Download PlansGuru APK (v1.1.0)</a>
            <a href="/api/model/gemma" class="btn" style="background: #E8DEF8; color: #1D192B;">🤖 Download Gemma 3 Model (584 MB)</a>

            <div class="badge">
                ⚡ Version 1.1.0 (Build 11) • Server Online
            </div>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=html_content)

@app.get("/join", response_class=HTMLResponse)
def join_redirect(inviter: str = "Friend", group: str = "Dinner Plan"):
    app_deep_link = f"plansguru://plan/join?inviter={inviter}&group={group}"
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
            <p>Joining <b>{group}</b> invited by <b>{inviter}</b>.</p>
            <p><a href="{app_deep_link}">Tap here to open PlansGuru App</a></p>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=html_content)

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "plansguru-relay"}

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
