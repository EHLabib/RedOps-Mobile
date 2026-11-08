# RedOps Mobile — Mobile Application Course Project Report

## 1. Project overview
RedOps Mobile is an Android application for organizing authorized security assessment engagements. It provides a structured way to record authorization, scope, assessment findings, manual observations, and a report summary from a phone.

## 2. Problem statement
Security assessment work produces multiple pieces of information: engagement scope, client details, observations, findings, severity, remediation recommendations, and reporting notes. Keeping these in separate documents can make a student project difficult to organize. RedOps Mobile brings these records into one local-first mobile workspace.

## 3. Objectives
- Provide an Android interface for managing assessment engagements.
- Make authorization confirmation a prerequisite for creating a new engagement record.
- Track findings with severity, affected asset, description, remediation, and status.
- Keep manual assessment notes organized by category.
- Generate a shareable text report from locally stored data.
- Work without requiring a server or internet connection at runtime.

## 4. Scope and ethical boundary
The application is a documentation and reporting tool. It does not scan networks, exploit targets, capture wireless traffic, or perform remote tests. All assessment activity documented by a user must be explicitly authorized and within the approved scope.

## 5. Technology stack
- Kotlin for application logic.
- Android SDK and Jetpack Compose for UI.
- Material 3 components for visual structure.
- SharedPreferences for local app-private storage.
- Gson for serializing the local data model.
- Android share intent for exporting report text.

## 6. Main modules
### Dashboard
Displays current counts for engagements, open findings, and notebook entries, with shortcuts into the workflow.

### Engagement manager
Stores engagement name, client/course, authorized scope, start/end dates, authorization confirmation, and status. Users can view and edit scope/status or delete records.

### Findings manager
Stores a finding title, severity, affected asset, description/evidence summary, remediation recommendation, status, and optional associated engagement. Users can resolve/reopen or delete a finding.

### Assessment notebook
Stores manual observations, evidence references, test notes, and meeting notes with a category and timestamp.

### Reports
Builds a plain-text summary of engagements, findings, notebook entries, and authorization limitations, then opens Android's share sheet.

## 7. Data model
- `Engagement`: id, name, client, scope, start date, end date, status, authorization flag.
- `Finding`: id, title, severity, asset, description, remediation, status, optional engagement id.
- `FieldNote`: id, category, title, detail, creation timestamp.
- `AppData`: lists of engagements, findings, and notes.

Records are serialized into app-private SharedPreferences. This keeps the demo self-contained, but does not provide encrypted record-level storage, cloud sync, or multi-user collaboration.

## 8. Validation and error handling
- Required engagement fields must be completed.
- A new engagement cannot be saved unless the user checks the authorization confirmation.
- Required finding fields must be completed.
- Notebook entries require a title and detail.
- Local loading falls back to an empty data model if stored data cannot be parsed.

## 9. Test plan
Run the cases in `testing.md` on an emulator or physical device. Record actual results and attach screenshots. Do not report tests as passed until they have been executed.

## 10. Limitations
- No login, roles, or team collaboration.
- No backend synchronization.
- Reports are plain text rather than PDF.
- No active scanning, wireless enumeration, or automated vulnerability detection.
- Data is protected by Android's app sandbox, but is not separately encrypted by the app.
- Deleting app data or uninstalling the app removes locally stored records.

## 11. Future improvements
- Add Room database and repository/ViewModel architecture.
- Add encrypted local storage and optional biometric app lock.
- Add PDF report generation and attachment management.
- Add test evidence files and CVSS calculator with manual review.
- Add authenticated team synchronization and audit trail.
- Add automated UI tests and CI build verification.

## 12. Conclusion
RedOps Mobile demonstrates a practical Android workflow for authorized security assessment documentation. It combines mobile UI, local persistence, input validation, record management, and report sharing in a self-contained course project. Its explicit boundary between documentation and active security testing makes its intended purpose clear.
