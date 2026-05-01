# TACZ Skins and Keychains

Skins and keychains are regular TACZ attachments now. They do not use a separate `cosmetics` folder, a separate `pack.json`, separate commands, or separate sync. They are loaded, synced, and reloaded through the normal TACZ gun pack pipeline.

## Layout

```text
data/<namespace>/
├─ index/attachments/<id>.json
├─ data/attachments/<id>_data.json
├─ tacz_tags/attachments/skin.json
├─ tacz_tags/attachments/keychain.json
└─ recipes/attachments/<id>.json

assets/<namespace>/
├─ display/attachments/<id>_display.json
├─ geo_models/attachment/<model>.json
├─ textures/attachment/uv/<texture>.png
└─ lang/en_us.json
```

This is the same structure used by scopes, magazines, barrels, and other TACZ attachments: `index` declares the attachment, `data` stores stats and cosmetic metadata, `display` handles client display, and tags place the attachment into a category.

## Index

`data/<namespace>/index/attachments/gold_deagle.json`:

```json
{
  "type": "skin",
  "name": "attachment.my_pack.gold_deagle",
  "data": "my_pack:gold_deagle_data",
  "display": "my_pack:gold_deagle_display"
}
```

For keychains, use `"type": "keychain"`.

## Data: Skin

`data/<namespace>/data/attachments/gold_deagle_data.json`:

```json
{
  "weight": 0.0,
  "cosmetic": {
    "rarity": "epic",
    "description": [
      "attachment.my_pack.gold_deagle.desc"
    ],
    "skin": {
      "type": "specific",
      "target_gun": "tacz:deagle",
      "texture": "my_pack:gun/uv/deagle_gold"
    }
  }
}
```

For a universal overlay skin:

```json
{
  "weight": 0.0,
  "cosmetic": {
    "rarity": "rare",
    "skin": {
      "type": "universal",
      "overlay_texture": "my_pack:attachment/uv/blood_overlay",
      "blend_mode": "multiply"
    }
  }
}
```

`texture` and `overlay_texture` are normal Minecraft/TACZ texture resources without `.png`. The example above resolves to `assets/my_pack/textures/gun/uv/deagle_gold.png`.

## Data: Keychain

`data/<namespace>/data/attachments/star_charm_data.json`:

```json
{
  "weight": 0.0,
  "cosmetic": {
    "rarity": "epic",
    "description": [
      "attachment.my_pack.star_charm.desc"
    ],
    "keychain": {
      "model": "my_pack:attachment/star_charm_geo",
      "texture": "my_pack:attachment/uv/star_charm"
    }
  }
}
```

The model resolves to `assets/my_pack/geo_models/attachment/star_charm_geo.json`; the texture resolves to `assets/my_pack/textures/attachment/uv/star_charm.png`.

The keychain attach point is defined on the weapon, not on the keychain. The weapon geo model must contain a dedicated bone and the gun data must declare:

```json
"allow_attachment_types": [
  "keychain"
],
"keychain_attachment": {
  "bone": "keychain",
  "offset": [0.0, 0.0, 0.0],
  "rotation": [0.0, 0.0, 0.0],
  "scale": [1.0, 1.0, 1.0]
}
```

If a weapon has no `keychain_attachment` or the named bone is missing, keychains cannot be attached to it.

## Display

Create `assets/<namespace>/display/attachments/star_charm_display.json` like any other TACZ attachment display file. In most packs, you can follow the existing display files for small attachment icons/models.

## Tags

Add the attachments to their categories:

```json
{
  "replace": false,
  "values": [
    "my_pack:gold_deagle"
  ]
}
```

Paths:

```text
data/<namespace>/tacz_tags/attachments/skin.json
data/<namespace>/tacz_tags/attachments/keychain.json
```

To limit a skin or keychain to specific weapons, use the normal TACZ attachment compatibility tags/rules, just like other attachment categories.

## Recipes

`data/<namespace>/recipes/attachments/star_charm.json`:

```json
{
  "type": "tacz:gun_smith_table_crafting",
  "materials": [
    {
      "item": {
        "item": "minecraft:amethyst_shard"
      },
      "count": 1
    }
  ],
  "result": {
    "type": "attachment",
    "id": "my_pack:star_charm",
    "count": 1
  }
}
```

If the attachment should not be crafted, omit the recipe.

## Localization

```json
{
  "attachment.my_pack.gold_deagle": "Golden Deagle",
  "attachment.my_pack.gold_deagle.desc": "A golden finish for the Deagle.",
  "attachment.my_pack.star_charm": "Star Charm",
  "attachment.my_pack.star_charm.desc": "A weapon keychain."
}
```

Path: `assets/<namespace>/lang/en_us.json`.

## Testing

1. Put the files into a normal TACZ gun pack.
2. Reload TACZ packs normally.
3. Open the TACZ refit screen.
4. Check the Skin and Keychain categories.

Common mistakes: the attachment is missing from a tag, the index has the wrong `"type"`, a texture path includes `.png`, the display file is missing, or `target_gun` does not match the weapon ID.
