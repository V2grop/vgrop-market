# VGrop Market 0.15.1 — watchlist drag and optional announcement

- Long-press a watchlist row to drag it; native edge autoscroll, saved order and TalkBack move actions. Removed visible up/down controls.
- Optional violet announcement card below the app header. Hidden by default with zero reserved space.
- Owner edits `config/announcement.json` on GitHub to set plain text, HTTPS link, campaign ID, optional start/end time or disable it without reinstalling the app.
- Dismissal by campaign ID, 15-minute foreground refresh, bounded 24-hour offline cache; invalid/disabled content fails closed.
- No ad SDK, tracking SDK, private API key or remote executable code.
- Existing analysis engines and scenarios unchanged; this remains an uncalibrated research prerelease with the compatibility debug certificate.
- Added offline notice validation and Android 12/15 instrumented drag/persistence/visibility tests. CI gates release on all builds and tests.

See docs/ANNOUNCEMENTS.md for Persian editing instructions. GitHub availability/cache can delay updates; offline devices cannot receive immediate campaign removal.
