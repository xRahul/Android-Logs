## 2024-05-24 - Empty States for Lists
**Learning:** New users were greeted with a confusing blank screen because log files didn't exist yet. Empty states are critical for onboarding and system status feedback, not just "nice to have".
**Action:** Always check `getItemCount()` or list size before rendering a RecyclerView and provide a helpful empty state with a call to action.
