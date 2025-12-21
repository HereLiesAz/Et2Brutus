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
