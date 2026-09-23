<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Dockerfile Layer Size Companion Changelog

## [Unreleased]

## [0.2.0]

### Fixed

- A file named `Dockerfile.md`/`.markdown`/`.mdx`/`.rst`/`.adoc` is no
  longer treated as a real Dockerfile. Unlike a plain warning, this
  plugin does real filesystem I/O for `COPY`/`ADD` sizing, so
  misdetecting a doc file meant resolving and sizing paths against a
  directory that had nothing to do with a real build context. Real
  variants (`Dockerfile.prod`, `Dockerfile.arm64`) are still
  recognized.
- `ADD <url> <dest>` (a real Docker feature -- downloads over the
  network at build time) is now labeled "downloads from a URL -- not
  calculable without fetching it" instead of the misleading "source
  not found in build context", which implied a broken local file
  rather than a network source.
- The known-expensive `curl`/`wget` check no longer fires on
  `apt-get install curl`/`apk add wget` -- installing the tool never
  downloads anything during the build. It still fires on a real
  invocation, including one in the same `RUN` that also installs the
  package.

### Added

- Review/star CTA: after 10 distinct known-expensive-pattern warnings,
  a one-time notification asks whether to rate the plugin on
  Marketplace, with a permanent "Don't ask again" option. Standard
  mechanism used catalog-wide; this plugin had been missed.

## [0.1.0]

### Added

- **`COPY`/`ADD` real layer size, inline**: sums the actual on-disk size
  of every file/directory a `COPY`/`ADD` instruction copies, resolved
  against the Dockerfile's own directory as the build context -- no
  Docker daemon, no build, no network call.
- **`.dockerignore` support** (documented common subset -- exact paths,
  bare directory names anywhere in the tree, `dir/**`, `dir/*`,
  `*.ext`, and `!`-negation; see README "Known limitations" for what's
  NOT covered): excluded paths never count toward the shown size.
- **Multi-stage `COPY --from=<stage>`** is recognized and labeled "not
  calculable without running the build" -- never a crash, never a
  fabricated number.
- **Known-expensive `RUN` pattern detection**, never a fabricated size
  for `RUN` (there is no way to know a command's real layer impact
  without executing it): flags `apt-get install` without
  `--no-install-recommends`, `apt-get update` without cleaning the
  package cache in the same instruction, `pip install` without
  `--no-cache-dir`, `npm install` without `--production`/`npm ci`, a
  `curl`/`wget` download with no visible cleanup in the same `RUN`,
  and runs of 2+ consecutive `RUN` instructions that could be combined
  into one.
- Recognizes `Dockerfile`, `Dockerfile.<env>` (`Dockerfile.dev`,
  `Dockerfile.prod`, ...), and `*.dockerfile` by filename -- no custom
  file type registered, so it never conflicts with another installed
  Docker-support plugin's own file type.

[Unreleased]: https://github.com/GapHunterLabs/dockerfile-layer-size-companion/compare/0.2.0...HEAD
[0.2.0]: https://github.com/GapHunterLabs/dockerfile-layer-size-companion/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/GapHunterLabs/dockerfile-layer-size-companion/commits/0.1.0
