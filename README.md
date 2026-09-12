# Auto Texture Packs

A client-only [Fabric](https://fabricmc.net/) mod for Minecraft 26.1.2 that automatically re-applies your resource/texture packs — in the exact order you saved them — a few seconds after the game finishes loading.

No more re-enabling and reordering your packs by hand every time you launch.

## Requirements

- Minecraft 26.1.2
- Fabric Loader `>=0.19.5`
- [Fabric API](https://modrinth.com/mod/fabric-api) `0.155.3+26.1.2` (or newer matching `+26.1.2`)
- Java 25
- [Mod Menu](https://modrinth.com/mod/modmenu) 18.0.1+ (required to reach the settings screen)

## Install

Drop the built jar into your instance's `mods/` folder alongside Fabric API and Mod Menu.

## Usage

1. Open **Mod Menu** → **Auto Texture Packs** → the gear icon.
2. In the vanilla Resource Packs screen, enable and order the packs you want, then confirm.
3. Back in the Auto Texture Packs screen, click **Use current pack order** to save that order.
4. Configure:
   - **Enabled** — turn auto-apply on/off.
   - **Delay** — how many seconds after the game loads to auto-apply (0–30s).
   - **Show chat/toast notification** — confirms what was applied, or warns about missing packs.
   - **Skip if packs already match** — skip the reload entirely when nothing changed.
5. Close and relaunch — your saved pack order re-applies automatically.

The saved order only changes when you explicitly click **Use current pack order**; nothing is captured automatically on close, so a saved preset is never silently overwritten by whatever happens to be selected when you quit.

**Apply now** in the settings screen re-applies the saved order on demand, without waiting for the delay or restarting.

## Building from source

```
./gradlew build
```

Requires Gradle's Foojay toolchain resolver to auto-provision a JDK 25 (Temurin) if your machine only has an older JDK; the wrapper handles this automatically. Output jar lands in `build/libs/`.

## License

CC0-1.0 — see [LICENSE](LICENSE).
