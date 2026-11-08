# RedOps Mobile Test Plan

## Environment
- Windows 10/11
- Android Studio and SDK Platform 34
- JDK 17
- Emulator API 26+ or physical Android device

## Functional cases
| ID | Test | Expected result |
|---|---|---|
| TC-01 | Launch app | Dashboard appears without crash |
| TC-02 | Save engagement with blank required field | Validation message appears |
| TC-03 | Save engagement without authorization confirmation | Save is blocked |
| TC-04 | Save authorized engagement | Engagement appears in list and dashboard count increments |
| TC-05 | Force-close/reopen app | Engagement remains saved |
| TC-06 | Edit engagement scope/status | Changes remain after save |
| TC-07 | Save finding with required fields | Finding appears in Findings |
| TC-08 | Mark finding resolved | Status changes to Resolved |
| TC-09 | Add notebook note | Note appears in Notebook |
| TC-10 | Delete a note/finding/engagement | Record disappears and counts update |
| TC-11 | Export report | Android share chooser opens with report text |
| TC-12 | Use airplane mode | Core record-keeping features continue working |

Record actual outcomes during testing; do not mark tests passed before running them.
