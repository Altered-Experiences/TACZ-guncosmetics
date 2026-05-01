# Updating Guns For TACZ 1.1.8

In 1.1.8 cosmetics use the normal TACZ attachment pipeline. Skins and keychains live in the same `data/<namespace>/data/attachments`, `index/attachments`, `recipes/attachments`, and `tacz_tags/attachments` folders as other attachments.

## Refit Views

Add two view bones to the gun geo model near the existing `refit_*_view` bones:

```json
{
  "name": "refit_skin_view",
  "parent": "views",
  "pivot": [20, 9, -2],
  "rotation": [0, 90, 0]
},
{
  "name": "refit_keychain_view",
  "parent": "views",
  "pivot": [12, 8, 12],
  "rotation": [0, 90, 0]
}
```

Tune `parent`, `pivot`, and `rotation` per weapon. If the model uses `view` as the view-bone container, use `view` instead of `views`.

## Keychain Attach Point

A gun supports keychains only when its geo model has a dedicated empty bone:

```json
{
  "name": "keychain_adapter",
  "parent": "gun_body",
  "pivot": [1.0, 6.0, 12.0]
}
```

Place `keychain_adapter` exactly where the keychain should attach. If this bone is missing, keychains are treated as unsupported for that gun.

## Gun Data

Add `skin` when the gun should support skins. For keychains, also add `keychain` and bind it to the model bone:

```json
"allow_attachment_types": [
  "scope",
  "muzzle",
  "extended_mag",
  "skin",
  "keychain"
],
"keychain_attachment": {
  "bone": "keychain_adapter",
  "offset": [0.0, 0.0, 0.0],
  "rotation": [0.0, 0.0, 0.0],
  "scale": [1.0, 1.0, 1.0]
}
```

## Allow Tag

In `data/<namespace>/tacz_tags/attachments/allow_attachments/<gun_id>.json`, allow the cosmetic tags supported by the gun:

```json
{
  "replace": false,
  "values": [
    "#tacz:skin",
    "#tacz:keychain"
  ]
}
```

Skins behave like regular attachments: the slot is available only when the gun has `skin` in `allow_attachment_types` and the allow tag accepts the skin item or a skin tag.

For guns that should not support skins, do not add `skin` or `#tacz:skin`. For guns that should not support keychains, do not add `keychain` to `allow_attachment_types`, do not add `keychain_attachment`, and do not list `#tacz:keychain` in the allow tag.
