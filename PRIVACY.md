# Privacy Policy — Dockerfile Layer Size Companion

**Effective date:** 2026-10-06

Dockerfile Layer Size Companion is a Gap Hunter Labs plugin for
IntelliJ Platform IDEs. This policy is short because the plugin's
design makes it short: there is nothing to disclose beyond what's
below.

## What this plugin collects

**Nothing.** Dockerfile Layer Size Companion does not collect, transmit, or sell any data — no source code, no file contents, no file
paths, no usage analytics, no telemetry, no crash reports, no
personally identifiable information. Dockerfile text and file sizes
read from your local project exist only in memory for as long as the
IDE is open.

## What it keeps on your machine

To decide when to show its one-time rating prompt, the plugin keeps two values
in the IDE's own settings on your computer: whether you have answered the
prompt, and a list of up to 500 findings it has already counted. Until the
next release, each entry in that list is the file path and line of a finding,
sometimes with its message. From the next release on, each entry is a one-way
fingerprint that cannot be turned back into a path, and the old list is
deleted. None of this is ever sent anywhere.

## Network access

**None.** Dockerfile Layer Size Companion makes zero network calls
during normal operation. Every size shown is computed by reading real
files already present on your local disk (`java.io.File.length()`) —
no remote lookup, no API call, no Docker daemon connection, ever.

## Third parties

None. Dockerfile Layer Size Companion has no third-party SDKs, no
analytics libraries, no ad networks, no external dependencies that
phone home.

## Changes to this policy

If this ever changes, this file will be updated and the change will be
noted in the plugin's `CHANGELOG.md`.

## Contact

Questions about this policy: **gaphunterlabs@gmail.com**
