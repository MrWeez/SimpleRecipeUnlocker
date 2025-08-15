
# SimpleRecipeUnlocker

Unlock all Minecraft crafting recipes for yourself or other players with a single command! This lightweight Paper (Bukkit) plugin is designed for servers that want to remove recipe discovery restrictions and make all crafting recipes instantly available.

## Features

- Instantly unlocks all available crafting recipes for a player
- Optionally unlocks recipes automatically when a player joins
- Supports unlocking recipes for other players
- Reloads configuration and locale files without server restart
- Customizable messages and permissions
- Broadcasts unlock events (optional)

## Requirements

- Java 17 or higher
- Gradle 8+ (for building)
- PaperMC (API version 1.20+) or compatible Bukkit server

## Build

Run:

```bash
./gradlew shadowJar
```

The jar will be located at `build/libs/SimpleRecipeUnlocker-1.0.0.jar`.

## Installation

1. Copy the jar into your server's `plugins/` folder.
2. Start or reload your server.

## Usage

### Command

```
/unlockrecipes [player|reload]
```

- `/unlockrecipes` — Unlocks all recipes for yourself
- `/unlockrecipes <player>` — Unlocks all recipes for another player (requires permission)
- `/unlockrecipes reload` — Reloads config and locale files (requires permission)

### Permissions

| Permission                        | Description                                              | Default |
|-----------------------------------|----------------------------------------------------------|---------|
| simplerecipeunlocker.use          | Use the /unlockrecipes command                           | true    |
| simplerecipeunlocker.reload       | Reload config/locales with /unlockrecipes reload         | op      |
| simplerecipeunlocker.other        | Unlock recipes for other players                         | op      |
| simplerecipeunlocker.autounlock   | Receive automatic unlock on join                         | true    |
| simplerecipeunlocker.broadcast    | Broadcast unlock event to all players                    | op      |

## Configuration

Edit `config.yml` to adjust plugin behavior:

```
settings:
	unlock_on_join: true      # Unlock recipes automatically on join
	cache_on_enable: true     # Cache all recipes at startup
	broadcast_unlock_on_join: false # Broadcast unlocks to all players
	log_undiscoverable: false # Log special (non-discoverable) recipes
```

Edit `locales.yml` to customize messages.

## Localization

All messages are configurable in `locales.yml` and support color codes (&).

## License

MIT License

You should see a hello message. Players joining get a welcome message.

## Customize

* Change group/name/version in `build.gradle.kts` & `settings.gradle.kts`.
* Update `plugin.yml` (name, description, commands, permissions).
* Add event listeners & commands in `onEnable()`.
* Remove `minimize()` from `shadowJar` if shading strips needed classes.

## Testing

JUnit + MockBukkit are configured. See `MyPluginMockTest` for an example to spin up a mock server and assert behavior.
