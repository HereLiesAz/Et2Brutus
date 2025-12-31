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

## 2025-12-23 - Active Error Feedback over Disabled States
**Learning:** Disabling a form submission button prevents users from learning why their input is invalid.
**Action:** Keep submission buttons enabled and use the click event to validate inputs and display specific error messages.

## 2025-01-08 - Keyboard Actions in Forms
**Learning:** On mobile, manually dismissing the keyboard between form fields breaks flow and causes frustration.
**Action:** Always configure `ImeAction` (Next/Done) for text inputs to allow smooth navigation between fields or easy completion.

## 2025-01-08 - Visual Selection in Filter Chips
**Learning:** `FilterChip` components are harder to scan when they rely solely on color for selection state.
**Action:** Always include a leading check icon (`Icons.Filled.Check`) in selected `FilterChip`s to provide clear, accessible visual confirmation.
