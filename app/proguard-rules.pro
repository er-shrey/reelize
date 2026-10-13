# Reelize has no network layer and no reflective serialization, so the default
# Android and AndroidX consumer rules cover almost everything.

-keep class com.shrey.reelize.ui.MainActivity { *; }
-keep class com.shrey.reelize.detect.ReelWatchService { *; }
-keep class com.shrey.reelize.ui.DashboardViewModel { <init>(...); }
