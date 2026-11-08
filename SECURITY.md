# Security Policy & Ethical Boundaries

## Authorized Use Mandate

**REDOPS MOBILE** is designed exclusively as an **Authorized Security Assessment Platform** for security students, accredited penetration testers, cybersecurity educators, and defensive audit teams.

```
+-------------------------------------------------------------------------------+
|                                IMPORTANT NOTICE                               |
|                                                                               |
|  Use only on systems and networks for which you have explicit authorization.  |
|  Unauthorized access to computer systems or networks is strictly illegal.     |
+-------------------------------------------------------------------------------+
```

The application strictly adheres to defensive and auditing workflows. Under no circumstances does RedOps Mobile include, facilitate, or tolerate:
- Unauthorized network exploitation or vulnerability weaponization.
- Wi-Fi deauthentication, packet injection, or WPA/WPA2/WPA3 handshake cracking.
- Automated brute-forcing, password spraying, or credential harvesting.
- Covert surveillance, persistent backdoor installation, or malware deployment.
- Destructive denial-of-service (DoS/DDoS) scanning or fuzzing.

---

## Defensive & Passive Architecture Standards

1. **Passive Wireless Enumeration**:
   - RedOps Mobile utilizes standard Android `WifiManager` scan results broadcasted openly in public airwaves.
   - It performs zero packet tampering, monitor mode injection, or radio frequency disruption.
2. **Explicit Authorization Checkpoints**:
   - Every assessment requires an authenticated operator to provide an explicit declaration of authorization prior to logging in-scope targets, observations, or findings.
   - Out-of-scope targets are visually segregated and functionally restricted.
3. **Data Protection at Rest and in Transit**:
   - Mobile tokens and sensitive assessment records are stored locally using Android Jetpack Security (`EncryptedSharedPreferences` / Jetpack DataStore / Room with database encryption hooks).
   - Network communication between the mobile client and backend REST API is strictly conducted over HTTPS using industry-standard TLS 1.3/1.2 with JSON Web Token (JWT) Bearer authentication.
   - Backend passwords are encrypted using `bcrypt` (12 salt rounds minimum). No plain-text passwords or secrets are ever recorded or transmitted.

---

## Reporting a Vulnerability

If you discover a vulnerability in RedOps Mobile, please report it responsibly:

1. **Do not open a public issue.**
2. Email your findings with a detailed Proof of Concept (PoC) to `security@redops-platform.local` (or file a private GitHub Advisory).
3. Include:
   - Component affected (Android app, REST API, Database migrations, Auth flow).
   - Reproduction steps.
   - Impact assessment (CVSS 3.1 score).
   - Potential remediation strategy.

The maintainers will acknowledge receipt within 48 hours and work on a coordinated fix.
