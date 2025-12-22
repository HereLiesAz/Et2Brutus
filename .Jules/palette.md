## 2024-05-22 - Permission Status Clarity
**Learning:** Using a disabled button with "Granted" text to indicate success is confusing. It looks actionable but isn't.
**Action:** Use a clear, semantic status indicator (Check Icon + "Granted" label) instead of repurposing interactive elements.

## 2024-05-24 - Accessibility of Compound Controls
**Learning:** `Switch` and `Slider` components often lack inherent context for screen readers when placed next to `Text` labels without semantic grouping.
**Action:** Use `Modifier.toggleable` with `Role.Switch` on the parent `Row` for switches, and add `contentDescription` to sliders to ensure they are announced correctly.

## 2024-05-25 - Label Clarity over Jargon
**Learning:** Technical terms like "Pace (ms)" in form labels increase cognitive load. Users shouldn't have to parse units or technical concepts in titles.
**Action:** Use descriptive, natural language labels ("Attempt Delay") and move technical details (units like "ms") to supporting text.

## 2024-05-26 - Prevention of Data Loss
**Learning:** Instant deletion of user data (like profiles) without confirmation causes anxiety and errors.
**Action:** Always wrap destructive actions in a confirmation dialog that clearly states what is being deleted.
