The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

This is a copy of the changelog for the most recent version. For the full version history, go [here](https://github.com/TheIllusiveC4/Curios/blob/1.20.4/CHANGELOG.md).

## [7.4.0-beta+1.20.4] - 2024.04.08
### Added
- Added a new opt-in user interface for the Curios screen, enable by setting "enableExperimentalMenu" to true in the
  curios-server.toml configuration file
- Added a configuration setting for configuring slots to the curios-common.toml configuration file
- Added "validators" as a field to the slot data files
- [API] Added the following methods to `ICuriosItemHandler`:
  - `isEquipped(Item)`
  - `isEquipped(Predicate<ItemStack>)`
- [API] Added the following methods to `CuriosApi`:
  - `getSlotUuid(SlotContext)`
  - `registerCurioPredicates(ResourceLocation, Predicate<SlotResult>)`
  - `getCurioPredicate(ResourceLocation)`
  - `testCurioPredicates(Set<ResourceLocation>, SlotResult)`
### Changed
- Slot types now exist client-side and are synced from the server
- Slot validations for item stacks are no longer tied solely to item tags and now follow the "validators" field added to
  the slot data files
### Deprecated
- Deprecated the following methods in `CuriosApi`, replaced by client and server-aware methods as listed in the
  javadocs:
  - `getSlot(String)`
  - `getSlotIcon(String)`
  - `getSlots()`
  - `getPlayerSlots()`
  - `getEntitySlots(EntityType<?>)`
  - `getItemStackSlots(ItemStack)`
