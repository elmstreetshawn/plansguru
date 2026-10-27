# PlansGuru — Anonymous Group Membership & Anti-Fingerprinting Architecture

---

## The Privacy Dilemma
*If you create a group plan, how do participants identify each other and verify membership without using persistent tracking IDs or device fingerprinting?*

---

## The Cryptographic Solution: Ephemeral Session Keys & Blind Invitation Tokens

To achieve zero-fingerprint group identification and metadata privacy, PlansGuru uses **Epoch-Based Ephemeral Keypairs** and **Cryptographic Blind Invitation Tokens**.

### 1. Ephemeral Session Keypairs (Zero Persistent Fingerprinting)
- **No Master Key Exposure:** Participants never use their permanent master `publicIdentityKey` to chat or communicate in a group session.
- **Session-Specific Key:** When joining a plan, each participant generates a fresh, random **ephemeral public key** (`ed25519` / `secp256k1`) valid *only* for the duration of that specific dinner session.
- **Automatic Destruction:** Once consensus is reached and the dinner plan is completed, the ephemeral private key is wiped from memory, leaving zero persistent linkability, tracking history, or device fingerprint.

### 2. Cryptographic Blind Invitation Tokens
- When you create a plan and invite Maya, Jo, and Priya, your device generates a signed **Blind Invitation Token**:
  $$\text{Token} = \text{Sign}_{\text{YourPrivKey}}(\text{SessionID} \parallel \text{ExpirationEpoch})$$
- When Maya connects to the group WebSocket room (`wss://.../ws/session_123`), she presents this Token.
- **Membership Proof without Tracking:** The server (and other participants) verify the signature against your known coordinator public key. They confirm Maya is authorized to join *without* knowing her device hardware, IP address metadata, or master identity unless she chooses to reveal her display name.

### 3. Blind Relay Enforcement
- The Python server enforces strict metadata stripping (removing IP headers, User-Agent strings, and device identifiers) before broadcasting packets to the session room, ensuring complete anonymity from network-level fingerprinting.
