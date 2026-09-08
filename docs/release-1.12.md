# tjHBM-NTM 1.12

ABMs now retain evidence when their explosion destroys the tracked target, including target entity ID, UUID and dimension. The launch-pad getInterceptorStatus callback returns INTERCEPTED for that exact shot and target even after the target entity is removed. A pad retains a bounded history of 64 shots so subsequent launches do not overwrite earlier in-flight results. History is transient; after reload or eviction, outcomes remain unknown.

Install on server and clients and restart Minecraft. Update CENTRAL to STRATCOM 3.6.4; existing defense runtime 2.4.0 forwards the result. Radar loss alone is not treated as proof of interception.

Java 8 Gradle build and 85 tests passed locally, including exact shot/target correlation. Live explosion behavior and client/dedicated-server smoke checks remain unverified.
