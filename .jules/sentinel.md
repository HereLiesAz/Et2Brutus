## 2025-12-19 - Accessibility Service Data Leak
**Vulnerability:** The Accessibility Service was logging `nodeInfo.text` when a View ID was missing, potentially exposing passwords or PII from other applications in Logcat.
**Learning:** Debugging logs in Accessibility Services must be strictly sanitized. Even fallback identifiers can be dangerous if they use content.
**Prevention:** Use structural identifiers (Class Name + Screen Bounds) instead of content (Text/Description) for logging unidentified nodes.

## 2025-07-04 - Sensitive Data Exposure in Logs
**Vulnerability:** `BruteforceViewModel` and `BruteforceEngine` were logging generated password candidates and success candidates to Logcat.
**Learning:** Even verbose logs (`Log.v`) or debug logs (`Log.d`) are risky in security-sensitive loops (like brute-forcing) as they can leak the entire dictionary or success credentials.
**Prevention:** Audit logging statements in sensitive data processing loops and redact or remove variable content.

## 2025-12-21 - Recurring Sensitive Data Leak in BruteforceEngine
**Vulnerability:** `BruteforceEngine` was logging the `resumeFrom` value (last attempted password) in cleartext upon starting generation or resuming.
**Learning:** Sensitive data variables can easily slip into informational logs (`Log.i`) intended for debugging state flow, especially when "resuming" state.
**Prevention:** Mark sensitive variables (like passwords, keys, PII) explicitly in the code or use a wrapper type (e.g. `Sensitive<T>`) that overrides `toString()` to return "[REDACTED]", preventing accidental logging.

## 2025-12-22 - Unintended Cloud Backup of Sensitive Data
**Vulnerability:** The application was configured with `android:allowBackup="true"` by default, allowing sensitive configuration (attack profiles, dictionary paths, last attempted passwords) to be backed up to the cloud and potentially restored to other devices.
**Learning:** Android templates often default to allowing backup, which is risky for security tools or apps handling sensitive local state that shouldn't leave the device.
**Prevention:** Explicitly set `android:allowBackup="false"` in `AndroidManifest.xml` for security-critical applications to prevent data exfiltration via ADB or Cloud Backup.

## 2025-12-22 - Password Exposure in Accessibility Node Cache
**Vulnerability:** The Accessibility Service was caching the `text` content of all identified nodes, including password fields, in the `NodeInfo` data class. This sensitive data would persist in memory (in `InteractionManager` and `BruteforceState`) and could potentially be exposed if state was serialized or logged.
**Learning:** Data classes used for state transfer (like `NodeInfo`) often outlive the immediate scope of an operation. If they indiscriminately capture sensitive fields from sources (like `AccessibilityNodeInfo`), they become a persistent security risk.
**Prevention:** Sanitize or omit sensitive fields (like `text` from `isPassword=true` nodes) at the point of ingestion/creation of the data object, rather than relying on downstream consumers to handle it safely.

## 2025-12-22 - PII Leaks in Accessibility Node Persistence
**Vulnerability:** The `NodeInfo` data class, used for persisting identified UI elements in Profiles, was capturing the `text` content of all nodes, including editable fields (inputs).
**Learning:** Even if `isPassword` is handled, standard input fields (username, email, search) often contain PII or transient user data at the moment of identification. Persisting this snapshot in a configuration profile is a privacy leak.
**Prevention:** Explicitly clear the `text` field for any node where `isEditable` is true during the creation of the persistence object (`NodeInfo`), relying instead on View IDs or structural properties for re-identification.
