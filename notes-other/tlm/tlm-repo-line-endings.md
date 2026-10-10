---
name: tlm-repo-line-endings
description: TLM repo stores CRLF blobs but the machine has core.autocrlf=true, so plain git add reflows whole files
metadata:
  type: reference
---

The TouhouLittleMaid repo stores **CRLF inside its committed blobs**, while this machine has `core.autocrlf=true` globally. A plain `git add` on an existing text file therefore normalises it to LF and rewrites the entire file in the diff - a one-line change showed up as 2349 changed lines in `lang/en_us.json`.

Workaround used: `git -c core.autocrlf=false add -- <paths>`. Note `git add` also skips files whose stat info is unchanged, so after fixing line endings you may need `git rm --cached` first to force a re-hash.

New files (the settings `.yml`, and everything in the addon repo) are LF, matching the existing `winefox.yml`. Only pre-existing TLM files need the workaround.

Related: [[tlm-trumpet-addon]].
