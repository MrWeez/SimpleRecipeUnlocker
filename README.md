# MyPlugin

Minimal Paper (Bukkit) plugin skeleton.

## Requirements

* Java 17+
* Gradle 8+

## Build

Run:

```bash
./gradlew shadowJar
```

Jar will be at `build/libs/MyPlugin-1.0.0.jar`.

## Install

Copy the jar into your Paper server `plugins/` folder and start the server. Restart or use a plugin manager to load.

## Usage

In game run:

```text
/mycommand
```

You should see a hello message. Players joining get a welcome message.

## Customize

* Change group/name/version in `build.gradle.kts` & `settings.gradle.kts`.
* Update `plugin.yml` (name, description, commands, permissions).
* Add event listeners & commands in `onEnable()`.
* Remove `minimize()` from `shadowJar` if shading strips needed classes.

## Testing

JUnit + MockBukkit are configured. See `MyPluginMockTest` for an example to spin up a mock server and assert behavior.
