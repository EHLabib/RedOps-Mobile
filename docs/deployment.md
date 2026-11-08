# RedOps Mobile — Deployment & Release Architecture

## 1. Android Release Build Pipeline

### 1.1 Gradle Release Configuration
The Android application builds two distinct variants:
1. **Debug APK**: Includes demo datasets, mock network interceptors, logging interceptors, and non-minified symbols.
2. **Release APK**: Signed with release keystore, minified via ProGuard/R8, with log obfuscation.

### 1.2 Keystore Security
- Keystore credentials and signing keys must never be committed to source control.
- In CI/CD (GitHub Actions), signing secrets are injected via GitHub Actions Secrets:
  - `ANDROID_KEYSTORE_BASE64`
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`

---

## 2. Backend Containerized Deployment

The backend is packaged into an ultra-lean Alpine Node.js Docker container:
- Production `NODE_ENV=production`
- Runs under a non-root user (`node`)
- Managed with healthchecks and restart policies via `docker-compose.yml` or Kubernetes manifests.
