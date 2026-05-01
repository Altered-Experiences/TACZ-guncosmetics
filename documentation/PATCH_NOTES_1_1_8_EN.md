# TACZ 1.1.8 - Cosmetic Attachments

Version 1.1.8 moves skins and keychains into the regular TACZ attachment system. The separate GunCosmetics pipeline has been removed: cosmetics now load, craft, appear in the refit menu, and apply stat modifiers the same way as every other attachment.

## What Changed

- Skins and keychains now use the standard TACZ paths: `data/tacz/data/attachments`, `data/tacz/index/attachments`, `data/tacz/tacz_tags/attachments`, and `data/tacz/recipes/attachments`.
- Separate GunCosmetics items and commands were removed. Distribution, recipes, and tags use the normal `tacz:attachment` item.
- Skin and keychain slots are integrated into the standard attachment manager/refit UI.
- If a gun does not support a skin or keychain, the slot is shown as unsupported.
- Skins can use the same stat modifiers as regular attachments, including `damage`, `ads`, `weight`, `movement_speed`, `aim_inaccuracy`, and other registered TACZ properties.
- Keychains attach only to an explicit gun bone named `keychain_adapter`. The separate position editor and keychain sway animation were removed.
- Keychain geo models were updated to `format_version: "1.21.110"`.
- Geckolib for 1.20.1 was updated to the Forge release `4.8.3`.

## Pack Update Notes

- Add `refit_skin_view` and `refit_keychain_view` to guns that should expose cosmetic slots.
- Add a `keychain_adapter` bone to the gun model when the gun supports keychains.
- Allow skins and keychains through normal attachment allow-tags, the same way scopes, magazines, and other categories are handled.
- Define skins and keychains as normal attachment data/index/recipe files. Do not use separate `guncosmetics` directories.

## Compatibility

Old GunCosmetics files and commands are no longer part of the active pipeline. Packs should be migrated to the standard TACZ attachment pack structure.

## Notes

1. We are doing Codex with this one! (it really works better than Antigravity's Claude Opus and Gemini)
2. Keychains are to be updated with proper settings for positioning.
3. Requirements: Geckolib 4.8.3+, PacketFixer (any)
4. It builds already as a 1.1.8-trinkets and INCOMPATIBLE with current addons (obviously). Use "all" version as it comes with mixins.