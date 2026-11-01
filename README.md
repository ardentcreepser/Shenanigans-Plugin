# Shenanigans 🎭

A big Paper plugin for **Minecraft 26.3** with **99 trolls** and fun extras, inspired by the classic troll plugins (TrollGUI, Troll+, "fake crash", "demo screen", Herobrine...) plus new stuff that only works on 26.x (bouncy players, ice-skate feet, tiny/giant players).

Everything is built to be **harmless to your server**: no real bans, no client-crashing exploits, no deleted items, no destroyed builds. Cages only fill empty space, fake TNT never breaks blocks, inventories are saved to disk before "Inventory Panic" hides them, and changed player stats are stored on the player so they get fixed even after a crash.

## Features

- **Chest menu** (`/troll`): pick a player's head, then click trolls. Category tabs, pages, glowing = currently on.
- **Troll Wand** (`/troll wand`): a blaze rod you point at someone (up to 60 blocks away) and right-click. Sneak-click to switch trolls.
- **Commands with tab-complete** for everything, including trolling everyone at once with `*`.
- **One-shot trolls** (happen once) and **toggle trolls** (stay on until they time out or you click them again).
- **Staff notifications + console log** so you know who trolled who.
- **Bypass permission** to protect people (like the owner).
- **Everything configurable** in `config.yml`: messages, durations, fake ban reason, silly phrases, damage, and a list to disable any troll.

## Installing

1. Your server must run **Paper 26.3** (Java 25).
2. Put `Shenanigans-1.0.0.jar` into your server's `plugins` folder.
3. Restart the server. A `plugins/Shenanigans/config.yml` file gets created.

## Building the .jar

You need the source code turned into a `.jar` file first. Pick **one** way:

### Option A — Let GitHub build it (no installs needed)
1. Make a free GitHub account and create a new repository.
2. Upload everything in this folder (including the hidden `.github` folder).
3. Open the **Actions** tab, click the latest **Build** run, and download **Shenanigans** under *Artifacts*. Unzip it — the `.jar` is inside.

### Option B — Build on your own computer
1. Install **Java 25** (for example Temurin from https://adoptium.net).
2. Open a terminal in this folder and run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
3. The plugin is at `build/libs/Shenanigans-1.0.0.jar`.

The first build downloads Gradle and the Paper API, so it takes a minute or two.

### Option C — IntelliJ IDEA
Open this folder in IntelliJ (Community Edition is free), wait for it to load, then open the Gradle panel on the right and run **Tasks → build → build**.

## Commands

| Command | What it does |
|---|---|
| `/troll` | Open the troll menu |
| `/troll <player> <troll> [extra]` | Use a troll (also works as `/troll <troll> <player>`) |
| `/troll <player> random` | Random one-shot troll |
| `/troll * <troll>` | Troll everyone online except you |
| `/troll stop <player\|*> [troll]` | Turn off toggle trolls |
| `/troll list [category]` | Every troll in chat. Hover for info, click to use |
| `/troll active` | Who has trolls running right now (click to stop) |
| `/troll gui [player]` | Open the menu (optionally straight to a player) |
| `/troll wand [troll]` | Get the Troll Wand |
| `/troll reload` | Reload `config.yml` |

Aliases: `/shenanigans`, `/trolls`

Some trolls take extra words, for example:
`/troll Steve fakeban Hacking with a potato` · `/troll Steve sudo I love dirt` · `/troll Steve title Behind you` · `/troll Steve cage obsidian` · `/troll Steve shuffle all`

## Permissions

| Permission | Default | What it does |
|---|---|---|
| `shenanigans.*` | op | Everything below (except bypass) |
| `shenanigans.use` | op | Use `/troll` and the menu |
| `shenanigans.troll.*` | op | Use every troll |
| `shenanigans.troll.<id>` | op | Use one troll, e.g. `shenanigans.troll.fakeban` |
| `shenanigans.all` | op | Troll everyone at once with `*` |
| `shenanigans.wand` | op | Get and use the Troll Wand |
| `shenanigans.reload` | op | `/troll reload` |
| `shenanigans.notify` | op | See when other staff troll people |
| `shenanigans.bypass` | nobody | Can't be trolled by others |
| `shenanigans.bypass.override` | nobody | Troll people even if they have bypass |

## All 99 trolls

Toggle durations are in `config.yml` under `durations` (0 = until you turn it off).

### Fake-Outs (15)

| id | Name | Type | What it does |
|---|---|---|---|
| `fakeop` | Fake OP | one-shot | Shows them the real-looking "Made you a server operator" message. They are not op. |
| `fakedeop` | Fake De-OP | one-shot | Tells them they are no longer a server operator. Great on actual admins. |
| `fakegamemode` | Fake Creative | one-shot | "Your game mode has been updated to Creative Mode" ...but it hasn't. |
| `fakeban` | Fake Ban | one-shot | Kicks them with a real-looking "You are banned from this server" screen. They can rejoin. |
| `fakecrash` | Fake Crash | one-shot | Disconnects them with a scary-looking Internal Exception error. They can rejoin. |
| `fakejoin` | Fake Join | one-shot | Broadcasts "&lt;name&gt; joined the game" even though they're already here. |
| `fakeleave` | Fake Leave | one-shot | Broadcasts "&lt;name&gt; left the game" while they're still online. |
| `fakedeath` | Fake Death | one-shot | Broadcasts a silly death message ("was poked to death by a sweet berry bush") while they're alive. |
| `fakeadvancement` | Fake Advancement | one-shot | "&lt;name&gt; has made the advancement [Certified Noob]" for everyone to see. |
| `fakehacker` | Fake Anticheat Alert | one-shot | Broadcasts fake anticheat alerts saying they're using hacks like KillAura or Fly. |
| `fakechat` | Fake Chat | one-shot | Makes it look like they said something in chat (only a message, they don't really type it). |
| `sudo` | Sudo (Force Chat) | one-shot | Makes them REALLY send a chat message. (Commands are blocked for safety.) |
| `fakerestart` | Fake Restart | one-shot | Counts down "Server restarting in 10 seconds" just for them, then kicks them with "Server closed". |
| `fakexp` | Fake XP Wipe | toggle | Their XP bar suddenly shows level 0. It's only visual and fixes itself. |
| `fakelowhp` | Fake Half Heart | toggle | Their screen shows half a heart and an empty food bar. It's fake and fixes itself. |

### Scares & Sounds (16)

| id | Name | Type | What it does |
|---|---|---|---|
| `demo` | Demo Screen | one-shot | Pops up the "You're playing the Demo version" screen. Classic. |
| `credits` | End Credits | one-shot | Shows the End Poem and credits like they just beat the game. |
| `guardian` | Elder Guardian | one-shot | The Elder Guardian ghost face flies into their screen with that sound. |
| `jumpscare` | Jumpscare | one-shot | Darkness... silence... then a screaming face. Turn your volume down. Or don't. |
| `herobrine` | Herobrine Visit | one-shot | Herobrine "joins", whispers creepy stuff, lightning strikes behind them, then he leaves. Only they see it. |
| `title` | Big Title | one-shot | Puts a giant title on their screen. Type your own or use the default. |
| `totem` | Fake Totem Pop | one-shot | Plays the Totem of Undying animation in their face like they just almost died. |
| `virus` | Fake Virus Download | one-shot | Shows a fake "Downloading free_robux.exe" progress bar, then "VIRUS INSTALLED". |
| `hiss` | Creeper Hiss | one-shot | Plays a creeper hiss right behind them. Watch them run. |
| `cave` | Cave Noise | one-shot | One of those creepy cave ambience sounds. Nobody knows what they are. |
| `knock` | Knock Knock | one-shot | Someone knocks on a door near them. Three times. |
| `footsteps` | Footsteps | one-shot | Footsteps creep up behind them, getting closer and closer... |
| `explosion` | Explosion Sound | one-shot | A huge explosion goes off next to them. Nothing actually explodes. |
| `ghast` | Ghast Scream | one-shot | A ghast screams in their ears. Works best in the Overworld. |
| `enderman` | Enderman Stare | one-shot | The "you looked at an enderman" noise, then a teleport sound right behind them. |
| `noise` | Noise Spam | toggle | A wall of random annoying sounds (goat screams, villager no's...). |

### Movement & Physics (24)

| id | Name | Type | What it does |
|---|---|---|---|
| `launch` | Launch | one-shot | Yeets them straight up into the sky. Fall damage is cancelled (see config). |
| `rocket` | Rocket | one-shot | Turns them into a rocket with a smoke trail and a firework explosion at the top. |
| `skydive` | Skydive | one-shot | Teleports them way up into the sky. They fall back down safely (fall damage cancelled). |
| `freeze` | Freeze | toggle | They can look around but can't move at all. They'll think they're lagging. |
| `spin` | Spin | toggle | Spins their camera around and around and around... |
| `flip` | 180 Flip | one-shot | Instantly turns them around to face the other way. |
| `lag` | Fake Lag | toggle | Every few seconds they get rubber-banded back to where they were. Very believable. |
| `drunk` | Drunk | toggle | Wobbly camera, nausea and random stumbling. |
| `hiccups` | Hiccups | toggle | *hic!* They randomly hop into the air with a burp. |
| `sneeze` | Sneeze | one-shot | ACHOO! They sneeze so hard they fly backwards. |
| `push` | Ghost Push | one-shot | An invisible force shoves them in a random direction. |
| `levitate` | Levitate | one-shot | They float up helplessly for a few seconds (and don't take fall damage after). |
| `randomtp` | Random Teleport | one-shot | Teleports them to a random safe spot somewhere in the world. |
| `swap` | Swap Places | one-shot | Swaps their position with another random player (or one you choose). |
| `tiny` | Tiny | toggle | Shrinks them to ant size. Everything is huge now. |
| `giant` | Giant | toggle | Makes them HUGE. Watch them get stuck in doorways. |
| `moon` | Moon Gravity | toggle | Super low gravity. Every jump goes to the moon. |
| `nojump` | No Jumping | toggle | Their jump button stops working. |
| `shortarms` | T-Rex Arms | toggle | Their reach is tiny. They can't break, place, open or hit anything that isn't right in their face. |
| `snail` | Snail Speed | toggle | They walk sooooo slooooowly. |
| `turbo` | Turbo Speed | toggle | They run way too fast to control. |
| `bouncy` | Bouncy | toggle | They bounce off the ground and walls like a rubber ball. (Minecraft 26.2+ attribute) |
| `iceskates` | Ice Skates | toggle | No grip on the ground at all, everything feels like ice. (Minecraft 26.2+ attribute) |
| `stairmaster` | Step Master | toggle | They walk up full blocks (and walls!) like stairs, by accident. |

### Inventory (6)

| id | Name | Type | What it does |
|---|---|---|---|
| `butterfingers` | Butterfingers | one-shot | They drop whatever they're holding. |
| `greasy` | Greasy Hands | toggle | Every now and then their held item just slips out of their hand. |
| `shuffle` | Hotbar Shuffle | one-shot | Shuffles their hotbar so nothing is where they left it. Use "all" to shuffle the whole inventory. |
| `panic` | Inventory Panic | toggle | Their whole inventory vanishes! ...and comes back when it ends. Saved to disk first, so it's 100% safe. |
| `hotpotato` | Hot Potato | toggle | Gives them a Hot Potato. They must DROP it before the timer ends or it blows up! (Harmless boom.) |
| `strip` | Wardrobe Malfunction | one-shot | Their armor falls off into their inventory (or onto the floor if it's full). |

### Chat (8)

| id | Name | Type | What it does |
|---|---|---|---|
| `uwu` | UwU Chat | toggle | Evewything they type gets uwu-ified. Hewwo! &gt;w&lt; |
| `backwards` | Backwards Chat | toggle | .sdrawkcab tuo semoc yllacigam yas yeht gnihtyrevE |
| `caps` | CAPS LOCK | toggle | EVERYTHING THEY SAY IS SHOUTED!!! |
| `scramble` | Word Scramble | toggle | Their words come out in a random order. Yoda would be proud. |
| `silly` | Embarrassing Chat | toggle | Whatever they type gets replaced with something embarrassing from silly-phrases in the config. |
| `shadowmute` | Shadow Mute | toggle | They can still chat and see their own messages... but nobody else can. They'll wonder why everyone's ignoring them. |
| `chatwipe` | Chat Wipe | one-shot | Pushes their whole chat off the screen with blank lines. |
| `spam` | Rainbow Spam | one-shot | Floods their chat with rainbow "TROLLED" messages. |

### World & Blocks (14)

| id | Name | Type | What it does |
|---|---|---|---|
| `cage` | Cage | one-shot | Traps them in a glass cage for a while. Only fills empty space, nothing gets destroyed. |
| `webtrap` | Web Trap | one-shot | Surrounds them with cobwebs that disappear after a bit. |
| `anvilrain` | Anvil Rain | one-shot | Anvils fall from the sky on them! They don't leave blocks behind. Damage is capped in the config. |
| `fakediamonds` | Fool's Diamonds | one-shot | Stone around them turns into diamond ore... but only on THEIR screen. Mining it gives nothing. |
| `border` | Tiny World Border | toggle | A world border appears just around them (only for them). They can't walk out of it. |
| `haunt` | Haunted | toggle | Doors and trapdoors near them open and close by themselves, with spooky noises. |
| `tnt` | Fake TNT | one-shot | Lit TNT appears at their feet... and goes boom. Only it's fake: no damage, no broken blocks. |
| `lightning` | Lightning | one-shot | Strikes lightning on them. Harmless by default ("real: true" in the config to make it hurt). |
| `fire` | Set on Fire | one-shot | Sets them on fire for a few seconds. |
| `dirtdrops` | Dirt Drops | toggle | Every block they mine drops... dirt. Diamond ore? Dirt. |
| `nomine` | Butter Pickaxe | toggle | They can't break any blocks. The blocks just pop back like lag. |
| `noplace` | Slippery Blocks | toggle | They can't place blocks. Every block just vanishes back into their hand. |
| `regen` | Blocks Grow Back | toggle | Blocks they break come right back a second later (and drop nothing). |
| `boompick` | Explosive Mining | toggle | Every block they break goes off like TNT. Only visually, nothing else gets destroyed. |

### Mobs & Fun (9)

| id | Name | Type | What it does |
|---|---|---|---|
| `bees` | Angry Bees | one-shot | A swarm of very angry bees appears around them. The bees leave after a while. |
| `silverfish` | Silverfish Infestation | one-shot | Silverfish pour out around them. They go away after a while. |
| `animalrain` | Animal Rain | one-shot | Cows, pigs, sheep and chickens rain from the sky around them. They can't die (no free food) and vanish later. |
| `creeper` | Creeper Surprise | one-shot | A creeper spawns right behind them and ignites. The explosion is fake. |
| `fireworks` | Firework Show | one-shot | A little firework show just for them. Fireworks don't hurt anyone. |
| `diamondrain` | Diamond Rain | one-shot | Diamonds rain down around them... but they can't pick any of them up. Pure torture. |
| `disco` | Disco Armor | toggle | Everyone else sees them in flashing rainbow leather armor. Their real armor isn't touched. |
| `glow` | Spotlight | toggle | Makes them glow so everyone can see them through walls. No more hiding. |
| `lonely` | Lonely | toggle | Every other player disappears from their screen. Where did everyone go? |

### Effects (7)

| id | Name | Type | What it does |
|---|---|---|---|
| `blind` | Blackout | one-shot | Blindness + darkness. They can barely see their own hands. |
| `starve` | Starve | one-shot | Their hunger bar drops to zero instantly. |
| `heartattack` | Heart Attack | one-shot | Drops them to half a heart (never kills) with a heartbeat sound. Run, Forrest, run! |
| `ouch` | Phantom Pain | one-shot | They flinch and hear the hurt sound like they got hit, but take no damage. Do it a few times. |
| `lazy` | Lazy Day | one-shot | Heavy slowness and mining fatigue. Everything takes forever. |
| `dizzy` | Dizzy | one-shot | Strong nausea that makes their screen warp and wobble. |
| `hunger` | Hungry Hungry | one-shot | The Hunger effect so their food bar drains super fast. |

## Adding your own troll

Every troll is a few lines in `src/main/java/com/shenanigans/troll/list/`. Copy one and change it. For example, add this inside `register(...)` in `EffectTrolls.java`:

```java
r.add(c, Troll.instant("pumpkinspice", "Pumpkin Spice", Material.PUMPKIN_PIE,
                "Gives them speed and plays a happy sound.")
        .does(ctx -> {
            Fx.effect(ctx.target(), PotionEffectType.SPEED, 200, 1);   // 200 ticks = 10 seconds
            Fx.sound(ctx.target(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }));
```

Rebuild, and it automatically shows up in the menu, `/troll list`, tab-complete and permissions.

For something that stays on, use `Troll.toggle(...)` with `.onStart(...)`, `.every(ticks, ...)` and `.onStop(...)` — always undo your changes in `onStop`. `MovementTrolls.java` has lots of examples.

## Notes

- **Fake Ban / Fake Crash / Fake Restart** really disconnect the player (that's the joke) — they can rejoin straight away.
- **Sudo** makes the player really send a chat message, but commands are blocked.
- **Anvil Rain** can hurt (2 hearts max per anvil by default). Set `trolls.anvilrain.hurt: false` to make it harmless.
- **Bouncy** and **Ice Skates** use attributes added in Minecraft 26.2. Tweak their `value` in the config if they feel too strong or too weak.
- Be nice. Trolling is funnier when everyone's laughing. 🙂
