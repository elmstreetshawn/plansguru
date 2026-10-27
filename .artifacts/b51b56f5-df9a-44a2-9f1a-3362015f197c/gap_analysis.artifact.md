# PlansGuru — Comprehensive Gap Analysis & Roadmap

This gap analysis evaluates the current implementation of **PlansGuru** against a production-ready, decentralized, privacy-first social graph and group dining coordination platform.

---

## 1. Decentralized P2P & Cryptographic Sync
- **Current Implementation:** Local SQLCipher encrypted database (`plansguru_secure.db`), cryptographic identity keys per person, and signed `GraphEvent` structures.
- **Identified Gap:** While cryptographic data models (`ProtocolMessage`, `GraphEvent`) exist, actual over-the-air peer-to-peer (P2P) synchronization (e.g., BLE, Wi-Fi Direct, or local relay) between friends' devices is currently mocked.
- **Recommended Next Step:** Implement a local transport layer (e.g., Nearby Connections API or local WebSocket relay) to exchange signed assertion graph events offline.

## 2. Live Multi-Agent Negotiation Protocol
- **Current Implementation:** `NegotiationEngine` evaluates participant assertions (cuisines, dietary restrictions, max distance/drive time, noise preferences) and computes top recommendations.
- **Identified Gap:** The round-trip negotiation protocol (`PLAN_REQUEST` ⇄ `PROPOSAL` ⇄ `CHALLENGE` ⇄ `CONSENSUS`) is computed instantaneously in a ViewModel rather than visualized as a multi-step agentic negotiation negotiation log.
- **Recommended Next Step:** Build a live negotiation protocol sequence view in `GuruPlanningScreen` showing each friend's AI agent proposing, challenging, and agreeing on candidates.

## 3. Cryptographic Trust Handshakes (QR / Key Verification)
- **Current Implementation:** Friends can be added manually, via demo contacts, or imported from phone contacts with default `VERIFIED` trust state.
- **Identified Gap:** Real cryptographic trust verification (scanning a friend's public identity key QR code to elevate trust from `PENDING` to `VERIFIED`) is not yet wired to a camera scanner.
- **Recommended Next Step:** Integrate CameraX QR code scanning to verify peer identity keys directly in the People tab.

## 4. Calendar & Schedule Availability Integration
- **Current Implementation:** Planning checklist simulates checking everyone's free times with a 2-second delay.
- **Identified Gap:** Not yet connected to Android Calendar provider to evaluate real mutual free time slots for group dinners.
- **Recommended Next Step:** Query `CalendarContract` to calculate actual overlapping free intervals among selected group participants.

## 5. End-to-End Persistence for Custom CRUD Entities
- **Current Implementation:** Places, Squads, and Preferences support full CRUD, with People and Preferences Hub persisting assertions to SQLCipher.
- **Identified Gap:** Custom added places and modified squads currently rely on local ViewModel/state lists; persisting custom places and squads to SQLCipher tables will ensure permanent offline state retention.
- **Recommended Next Step:** Add SQLite tables for `restaurants` and `squads` in `EncryptedGraphDatabase` to ensure 100% database persistence across app restarts.
