---
name: main-instance-renamed-slim
description: "The pack's Prism instance is now \"1.21.1 slim\", not \"1.21.1\"; old scripts that hardcode the old path break"
metadata:
  node_type: memory
  type: project
  originSessionId: 2150472a-9973-4689-b85c-840ccc0bc1f7
  modified: 2026-09-25T02:15:15.869Z
---

As of 25 Sep 2026 the live instance is `C:\Users\eteil.000\AppData\Roaming\PrismLauncher\instances\1.21.1 slim\minecraft`. The old `instances\1.21.1` folder no longer exists.

**Why:** many scripts under modpack-audit/test-client and earlier notes still hardcode the old path. run-animtest.ps1 was updated on 25 Sep; the others were not.

**How to apply:** use the slim path, and fix any script's `$inst` before running it. See [[mca-anim-compat-mod]].
