## 2025-12-19 - Recursive List Performance
**Learning:** Recursive functions that return lists and use `addAll` to merge them create a massive amount of temporary objects and copy operations ($O(N^2)$ behavior).
**Action:** When traversing a tree to collect data, pass a single mutable collection (List/Set) down the recursion stack instead of returning and merging lists.
