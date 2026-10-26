# PlansGuru Architecture Specification

PlansGuru is an Android-first, privacy-preserving, multi-agent social coordination application designed for decentralized group decision making.

## Core Architecture Components

1. **Android Client (`android/`)**:
   - Built with Kotlin, Jetpack Compose, and Material 3.
   - MVVM architecture with ViewModels and StateFlow.
   - SQLCipher-backed SQLite for encrypted local persistence at rest.
   - Android Keystore & EncryptedSharedPreferences for hardware-backed master key protection.
   - OkHttp WebSockets for direct peer-to-peer or relay-assisted agent communication.

2. **Backend Relay (`server/`)**:
   - Python FastAPI WebSocket relay server.
   - In-memory invitation and ephemeral session management.
   - **Zero persistent database storage**, zero application request logs, zero message history persistence, and zero third-party analytics.

3. **Multi-Agent Negotiation Protocol (`protocols/`)**:
   - Versioned JSON protocol message types (`HELLO`, `GRAPH_UPDATE`, `PLAN_REQUEST`, `PROPOSAL`, `CHALLENGE`, `REVISE`, `CONSENSUS`, `ACK`).
   - Two-round negotiation protocol evaluating hard constraints and maximizing minimum participant satisfaction against local social graphs.
