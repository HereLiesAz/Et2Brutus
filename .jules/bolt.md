## 2025-12-19 - Recursive List Performance
**Learning:** Recursive functions that return lists and use `addAll` to merge them create a massive amount of temporary objects and copy operations ($O(N^2)$ behavior).
**Action:** When traversing a tree to collect data, pass a single mutable collection (List/Set) down the recursion stack instead of returning and merging lists.

## 2024-05-23 - Accessibility Node Search Safety
**Learning:** Optimizing tree traversal by pruning branches based on parent bounds (`!parent.contains(point)`) is unsafe in Android View hierarchies. Views can render children outside their bounds (e.g., `clipChildren="false"`, negative margins), so pruning based on parent bounds causes false negatives for these elements.
**Action:** When optimizing hit-testing in View/Accessibility trees, always recurse into children even if the parent doesn't contain the target point, unless you can guarantee strict containment.
