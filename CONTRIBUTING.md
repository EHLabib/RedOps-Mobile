# Contributing to RedOps Mobile

Thank you for your interest in contributing to RedOps Mobile ("Authorized Security Assessment Platform").

As a dual-purpose university capstone portfolio and production-ready security tool, RedOps Mobile enforces high software engineering standards, clean code architecture, and strict ethical constraints.

---

## 1. Ethical Code of Conduct

Every contributor must review and uphold the **Authorized Use Mandate**:
- All feature contributions must support authorized security auditing, documentation, risk assessment, or passive enumeration.
- Submissions introducing exploit payloads, brute-force engines, deauthentication routines, or any automated destructive behaviors will be rejected immediately.

---

## 2. Architecture & Design Principles

### Android (Mobile Client)
- **Architecture**: MVVM + Clean Architecture + Repository Pattern.
- **UI Framework**: Modern Jetpack Compose with Material Design 3 (M3).
- **Design Guidelines**: Dark mode by default, cybernetic green/cyan palette (`#00E676`, `#00E5FF`, `#0F172A`), high contrast, professional typography.
- **Concurrency**: Kotlin Coroutines & Flow (StateFlow / SharedFlow) for reactive UI state.
- **Persistence**: Room Database with typed DAOs and Android Jetpack DataStore for preferences.
- **Networking**: Retrofit 2 + OkHttp 3 with JWT Bearer Interceptor and JSON serialization.
- **Dependency Injection**: Modular and testable structure (Manual DI / Hilt compatible).

### Backend (REST API Server)
- **Runtime**: Node.js + Express.js.
- **Database**: PostgreSQL (with SQLite fallback for local developer testing).
- **Authentication**: Stateless JWT authentication with bcrypt password hashing.
- **Validation**: Strict schema validation for all endpoints.
- **Error Handling**: Centralized error middleware returning RFC 7807 compliant Problem Details JSON.

---

## 3. Git Branching & Commit Conventions

### Branch Strategy
- `main`: Production-ready, fully tested code.
- `develop`: Integration branch for upcoming releases.
- `feature/<feature-name>`: Scoped feature development.
- `bugfix/<issue-number>`: Bug fixes and patches.

### Conventional Commits
Please structure commit messages according to the Conventional Commits specification:
```
<type>(<scope>): <short description>

[optional body]

[optional footer]
```
Examples:
- `feat(auth): implement JWT refresh token rotation in AuthRepository`
- `fix(wireless): handle Android 13 NEARBY_WIFI_DEVICES runtime permission`
- `docs(api): update swagger spec for findings evidence upload`

---

## 4. Code Quality & Pre-PR Checklist

Before submitting a Pull Request, verify:
- [ ] Code compiles without warnings (`./gradlew assembleDebug` in `android/`).
- [ ] Backend tests pass (`npm test` in `backend/`).
- [ ] Architecture boundaries are respected (e.g., UI does not talk directly to DAOs).
- [ ] No hardcoded passwords, tokens, or IP addresses.
- [ ] Documentation and inline docstrings are updated.
- [ ] Authorization confirmation gate is verified on all assessment-related actions.
