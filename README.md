# Infinite One Blocks

Fabric mod for **Minecraft 26.3**. The overworld is an empty void with a single line of **every block in the game**. Every block is an infinite "one block": break it, it drops itself, and it instantly grows back. Beat the game from there.

*Magyarul lent.*

## How it works

- New worlds default to the **Infinite One Blocks** world type (the other types are still selectable).
- The line holds every block that has an item form (about 1100), two blocks apart, in a random order per seed. You start on a grass block at the start of the line.
- Breaking a line block drops it as an item from the side you hit it from. In the void, it can fall away.
- Vanilla tool rules apply: you only get stone with a pickaxe, and so on. Normally unbreakable blocks (bedrock, end portal frame, command blocks…) mine like obsidian, so they need a diamond pickaxe.
- Line blocks never fall, decay, pop off or react to redstone. If an explosion, piston or fire removes one, it comes back within half a second.
- After the last block of the line waits an **End portal with 12 empty frames**. Fill it with 12 eyes of ender to go fight the dragon.
- The Nether and the End are vanilla.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.5+ for Minecraft 26.3.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) (0.164.0+26.3) and `infiniteoneblocks-1.0.0.jar` into your `mods` folder.
3. Create a new world. The world type is already set to Infinite One Blocks.

### Dedicated server

In `server.properties`:

```properties
level-type=infiniteoneblocks:infinite_one_blocks
spawn-protection=0
```

`spawn-protection=0` is needed, otherwise non-op players cannot break the line blocks near spawn.

## Building

Requires Java 25.

```
./gradlew build
```

The jar ends up in `build/libs/`. `./gradlew runClientGameTest` runs the automated in-game test.

---

## Magyarul

Fabric mod a **Minecraft 26.3**-hoz. A felszín üres void, benne egy sorban **a játék összes blokkja**. Minden blokk végtelen: ha kiütöd, kiesik tárgyként, és azonnal visszanő. Ebből kell végigjátszani a játékot.

- Új világnál alapból az „Végtelen Egyblokkok” világtípus van kiválasztva.
- A kiütött blokk arról az oldalról esik ki, amelyik felől ütöd. Vigyázz, a voidba leeshet!
- Vanilla szerszámszabályok vannak. A törhetetlen blokkok (bedrock, end portal frame, command block…) úgy bányászhatók, mint az obsidian.
- A sor végén egy **End portál vár 12 üres kerettel**. Tegyél bele 12 Ender-szemet, és mehetsz a sárkányhoz.
- Telepítés: Fabric Loader 0.19.5+ és Fabric API 0.164.0+26.3, majd a jar a `mods` mappába.
