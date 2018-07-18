# HiddenOre

Brought to you by the developers from https://www.reddit.com/r/Devoted and https://www.github.com/DevotedMC -- check out our server at play.devotedmc.com and our website at www.devotedmc.com .

Massively configurable, low-impact plugin to allow post world generation ore balance, via either drops, or ad-hoc generation of ores into the blocks of the world on-demand. It works as a wonderful anti-xray plugin, and as a powerful incentive tool for mining.

## Overview

Every time you break a block, naturally placed, there will be a lottery run. This will occasionally drop cool stuff instead of the block you broke, or generate new blocks around the miner.

Typical use case is for stone -- instead of dropping stone, occasionally drop ore instead, or generate awesome blocks around the miner.

For example:

1:100,000 chance to find prismarine.

1:3000 chance to find a diamond.

1:10000 chance to find a vein of 10 diamond ore.

Etc.

This plugin is fully configurable with biome-specific settings, tool restrictions, and limited player state restrictions, allowing a significant degree of options and specificity. 

## Details

The raw technical details:

On block break, checks the config to see if a lottery has been defined for this block. Optionally, a quick check for presence of Silk Touch is done.

You can specify multiple drops, where each drop is computed as an independent probability against the break, and the sum of drops is spawned into the world.

Alternatively, you can indicate only a single drop-type is allowed. In this case a single random number is generated and tested against a probability distribution of possible drops. This allows replication of Minecraft behavior (in terms of how ores are generated).

The chance to drop for each type of drop against a type of broken block is configurable.

A block can be configured to match all subtypes or just a specific set of subtypes (e.g. not regular stone but both andesite and diorite would be possible).

You can apply biome-level chance, level, and amount modifiers.

Drops can be restricted to specific Y levels.

Drops can be restricted to specific tools, with a high degree of configure-ability. Check `config.yml` and `config-advanced.yml`. 

Drops chances can be modifed based on player potion / effect state and level of that state. Currently haste, mining fatigue, nausea, blindness, and luck / bad luck states are supported. See `config.yml`.

Included is a default config that effectively mirrors Minecraft Vanilla orespawn; it should be possible to generate a normal MC world with no ores or caves, but with this plugin allow effectively normal vanilla riches. Consider it the ultimate XRay defense; you cannot see what literally doesn't exist.

Supports tracking of breaks to prevent "gaming" the system for more drops. Extra event handlers watch for game attempts and directly penalize that "chunk" (technically, the chunk slice). An extra "highly localized" round robin list keeps track of recent breaks and places to _completely_ prevent break-place based attempts at exploits. Finally, a new tracker keeps track of _each block_ that is broken or interacted, and prevents it from being converted into ore or producing drops.

You can specify more then one config per block type, to deal with subtypes even better. Note that in terms of drops, the first matching config to be encountered will be used; so keep that in mind. 

You can specify custom messages for specific types of drops, allowing "uber"-finds to have a unique message.

You can turn on or off the debug using `/hiddenore debug true` or `false`. Be warned, if people are digging, this will spam the console _very_ badly.

Supports saving and loading of the tracking database, and fully adheres to /reload with no difficulties.

As of 1.4.2, full multiple world support, via name or UUID. 

I'm probably missing some other details but that's it for now.

### TODO / In progress features:

* Configure which tool to "use" for cave dusting. Default right now is Diamond Pickaxe.

* Better documentation

* Minecraft 1.13 support (will be 1.5.0)

### Feature Augment List:

**v1.4.2** Added full multiple world support. Standard config is used as default for any world that does not have a specific config. A new section, `worlds`
can be specified, each subsection is either the UUID or name of the world with a specific config. A single `blocks` subsection under the world identifier contains all the block configurations for that world. It is configured like the default, within that subsection. Check the configs for examples.

**v1.4.1** Live use of new tracker shows its disk use is much increased. Added a configuration option to explicitly disable it. Added config example of Command "drops" and some fixes.

**v1.4.0** New exploit tracker that tracks the actual blocks broken or exposed. This will fully prevent the "but I already checked that block" problem. Heuristic tracking is, for now, still active.

**v1.3.2** You can now run a command on block break. If you use reward crates, could gift, or custom /give, etc -- runs as SERVER so be careful. Use %player% to inject the player name into the command, or %uuid% to inject the player's Minecraft UUID.

**v1.3.1** Added a command for OPs only that generates all the drops configured. It has some quirks, but type /hiddenore generate to spawn the items.

**v1.2.9** Support for dusting the caves in your map with ore based on your active config; kind of an addendum to v1.2.7's feature. Can be used to prevent the "boring caves" problem when a world is otherwise devoid of ore from the 1.2.7 feature. Don't use your final config, do use a Generate only config, and do turn off drops if generate fails.

**v1.2.8** Support for altering drop chance based on 6 basic player effect states, generated by potion or beacon. Configurable by drop and biome (biome acts as override)

**v1.2.7** Experimental feature to allow stripping a world of ores during the generation phase. Fully configurable per-world by name; you can
  set it to replace any set of materials with a single material. Also includes a new anti-bypass method to directly target that "initial return" that can still occur from generators and place-break-place-break cycles
