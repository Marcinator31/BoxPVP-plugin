# BoxPvP

A PaperMC **1.21.11** plugin for BoxPvP arenas. Define zones with a wand or
commands; configured blocks get removed on a timer, players are warned 10s
before (configurable), and dropped items inside the zone are cleared. Everything
is adjustable in `config.yml`.

## Build

You need JDK 21+.

```
mvn package
```

The jar is produced at `target/BoxPvP.jar`. Drop it into your server's
`plugins/` folder.

### Build via GitHub Actions

Push this repo to GitHub. The workflow in `.github/workflows/build.yml` runs on
every push/PR and uploads `BoxPvP.jar` as a build **artifact** (Actions tab →
the run → Artifacts).

To also publish a **Release** with the jar attached, push a tag:

```
git tag v1.0.0
git push origin v1.0.0
```

## Commands

`/boxpvp` (aliases: `/box`, `/bpvp`) — permission `boxpvp.admin` (default: op).

| Command | Description |
|---|---|
| `/boxpvp wand` | Get the selection wand |
| `/boxpvp pos1` / `pos2` | Set a point at your current position (no wand needed) |
| `/boxpvp create <name> [seconds]` | Create a zone from your selection; optional interval |
| `/boxpvp delete <name>` | Delete a zone |
| `/boxpvp reset <name>` | Reset blocks **and** items now (restarts countdown) |
| `/boxpvp resetblocks <name>` | Reset only the configured blocks |
| `/boxpvp resetitems <name>` | Clear only dropped items in the zone |
| `/boxpvp list` | List zones |
| `/boxpvp info <name>` | Show zone details |
| `/boxpvp reload` | Reload config + zones |

### Wand

Hold the wand item (default `BLAZE_ROD`), **left-click** a block for Pos1,
**right-click** for Pos2, then `/boxpvp create <name>`.

## Config

See `config.yml`. Key options: `wand-item`, `default-reset-interval`,
`warning-seconds`, `clear-items-on-reset`, `blocks-to-remove`, and all
`messages`.
