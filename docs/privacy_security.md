# PlansGuru Privacy & Security Requirements

PlansGuru enforces strict privacy-by-design guarantees:

1. **Cryptographic Identity**: Each device generates its own cryptographic identity; private signing keys never leave the secure hardware storage on the device.
2. **Encrypted Storage at Rest**: Persistent social graph data (Persons, Assertions, Relationships) is encrypted using SQLCipher with keys protected by Android Keystore.
3. **End-to-End Encryption & Ephemeral Routing**: Agent messages are signed and authenticated peer-to-peer. The relay server acts solely as a blind pipe and cannot decrypt agent payloads.
4. **No Persistent Central History**: The central relay server stores no social graph information, invitation metadata expires automatically after 15 minutes, and session state is strictly in-memory.
5. **No Fingerprinting or Analytics**: The application uses no advertising IDs, IMEIs, hardware fingerprints, contact uploads, or permanent server-side user identifiers.
6. **Limitations & Network Metadata**: While message payloads are encrypted end-to-end, network metadata (IP addresses and timing during WebSocket connection) is visible to relay intermediaries. Pseudonymity is maintained via cryptographic keys rather than absolute anonymity.
