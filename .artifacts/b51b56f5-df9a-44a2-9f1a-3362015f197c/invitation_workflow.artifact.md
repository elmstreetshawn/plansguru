# PlansGuru — Out-of-Band Cryptographic Invitation Workflow

---

## The Question: "How am I actually inviting people?"

In a privacy-first, decentralized ecosystem with no central user directory or phone book lookup, invitations are sent **out-of-band** using cryptographic bootstrap tickets (QR codes or secure deep links).

---

## The 3-Step Invitation Workflow

### Step 1: Generating the Secure Invite Ticket (Coordinator)
When you create a dinner plan with Maya, Jo, and Priya in the PlansGuru app:
1. Your device generates a unique `SessionID` (e.g., `session_taco_crew_987`).
2. Your device generates the symmetric **Group Session Key**.
3. Your app compiles an **Encrypted Invitation URI / QR Code**:
   `plansguru://plan/join?coordinator=pubkey_you&session=session_taco_crew_987&sig=...`

### Step 2: Sharing Out-of-Band (Any Messenger or In-Person)
- You share this secure QR code or deep link with Maya via **Signal, iMessage, WhatsApp, AirDrop, or scanning in-person**.
- Because the invitation is already encrypted and cryptographically bound to your coordinator key and session ID, **it doesn't matter what messaging app you use to send it**—even if intercepted, nobody can forge membership without your private key.

### Step 3: Accepting & Joining the Session
1. Maya taps the link or scans the QR code with her PlansGuru app.
2. Maya's app verifies your coordinator signature.
3. Maya's app generates an **ephemeral session key**, connects to the Python WebSocket relay (`wss://.../ws/session_taco_crew_987`), and securely joins the dinner planning session without exposing her persistent fingerprint or master identity to strangers!
