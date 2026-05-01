# Обновление оружия к TACZ 1.1.8

В 1.1.8 косметика подключается через обычную систему аттачей TACZ. Скины и брелки лежат в тех же `data/<namespace>/data/attachments`, `index/attachments`, `recipes/attachments` и `tacz_tags/attachments`, что и остальные аттачи.

## Refit views

В geo-модель оружия добавьте две view-кости рядом с остальными `refit_*_view`:

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

`parent`, `pivot` и `rotation` подбираются под конкретную модель. Если у оружия контейнер view-костей называется `view`, используйте его вместо `views`.

## Точка крепления брелка

Чтобы оружие поддерживало брелки, в geo-модели должна быть отдельная bone без кубов:

```json
{
  "name": "keychain_adapter",
  "parent": "gun_body",
  "pivot": [1.0, 6.0, 12.0]
}
```

Поставьте `keychain_adapter` там, где брелок должен крепиться на оружии. Если этой bone нет, брелки для оружия считаются неподдерживаемыми.

## Gun data

В data-файле оружия добавьте `skin`, если оружие должно поддерживать скины. Для брелков также добавьте `keychain` и привязку к bone:

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

## Allow tag

В `data/<namespace>/tacz_tags/attachments/allow_attachments/<gun_id>.json` добавьте теги косметики, которую поддерживает оружие:

```json
{
  "replace": false,
  "values": [
    "#tacz:skin",
    "#tacz:keychain"
  ]
}
```

Скины работают как обычные аттачи: чтобы слот был доступен, оружие должно иметь `skin` в `allow_attachment_types`, а allow tag должен пропускать конкретный скин или тег скинов.

Если оружие не должно поддерживать скины, не добавляйте `skin` и `#tacz:skin`. Если оружие не должно поддерживать брелки, не добавляйте `keychain` в `allow_attachment_types`, не добавляйте `keychain_attachment` и не указывайте `#tacz:keychain` в allow tag.
