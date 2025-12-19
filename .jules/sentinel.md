## 2025-12-19 - Accessibility Service Data Leak
**Vulnerability:** The Accessibility Service was logging `nodeInfo.text` when a View ID was missing, potentially exposing passwords or PII from other applications in Logcat.
**Learning:** Debugging logs in Accessibility Services must be strictly sanitized. Even fallback identifiers can be dangerous if they use content.
**Prevention:** Use structural identifiers (Class Name + Screen Bounds) instead of content (Text/Description) for logging unidentified nodes.
