---
name: specutils-gradle-wrapper-broken
description: specutils' committed gradle-wrapper.jar has no Main-Class, so ./gradlew fails; invoke GradleWrapperMain via -classpath instead.
metadata:
  type: project
---

`./gradlew` and `gradlew.bat` both fail in the specutils repo with:
`no main manifest attribute, in .../gradle/wrapper/gradle-wrapper.jar`

The jar is intact (passes `unzip -t`, all 44 classes present, matches the git
blob) but its MANIFEST.MF has no `Main-Class`, while both launcher scripts use
`java -jar`. It also contains Gradle 9-era `gradle-cli-*.properties`, so the
committed jar does not match the `gradlew` script or the 8.14 `distributionUrl`.

**Why:** upstream committed a mismatched wrapper jar. Not a local corruption.

**How to apply:** build with

    java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build --console=plain

This still downloads and uses Gradle 8.14 as intended. First build takes ~22 min
(Loom remaps Minecraft); later builds are 10-30s. Regenerating the wrapper is a
decision for the user, not a mechanical fix.

Related: [[specutils-source-repo]]
