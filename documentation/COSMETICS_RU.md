# Скины и брелки TACZ

Скины и брелки теперь являются обычными TACZ-аттачами. У них нет отдельной папки `cosmetics`, отдельного `pack.json`, отдельных команд и отдельной синхронизации. Они читаются, синхронизируются и перезагружаются вместе с gun pack через стандартные файлы TACZ.

## Структура

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
└─ lang/ru_ru.json
```

Главная идея такая же, как у прицелов, магазинов и остальных обвесов: `index` описывает аттач, `data` хранит характеристики и косметические поля, `display` отвечает за клиентское отображение, tags добавляют аттач в категорию.

## Index

Файл `data/<namespace>/index/attachments/gold_deagle.json`:

```json
{
  "type": "skin",
  "name": "attachment.my_pack.gold_deagle",
  "data": "my_pack:gold_deagle_data",
  "display": "my_pack:gold_deagle_display"
}
```

Для брелка используйте `"type": "keychain"`.

## Data: скин

Файл `data/<namespace>/data/attachments/gold_deagle_data.json`:

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

Для универсального overlay-скина:

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

`texture` и `overlay_texture` указывают на обычные Minecraft/TACZ texture resources без `.png`: пример выше ищет `assets/my_pack/textures/gun/uv/deagle_gold.png`.

## Data: брелок

Файл `data/<namespace>/data/attachments/star_charm_data.json`:

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

Модель ищется как `assets/my_pack/geo_models/attachment/star_charm_geo.json`, текстура как `assets/my_pack/textures/attachment/uv/star_charm.png`.

Точка крепления брелка задается не в самом брелке, а в data-файле оружия. У оружия должна быть отдельная bone в geo-модели и поле:

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

Если у оружия нет `keychain_attachment` или указанной bone, брелок на него не ставится.

## Display

Файл `assets/<namespace>/display/attachments/star_charm_display.json` делается так же, как у других TACZ-аттачей. Обычно достаточно указать модель/текстуру для отображения в интерфейсе, используя существующий формат display-файлов вашего gun pack.

## Tags

Добавьте аттачи в категории:

```json
{
  "replace": false,
  "values": [
    "my_pack:gold_deagle"
  ]
}
```

Пути:

```text
data/<namespace>/tacz_tags/attachments/skin.json
data/<namespace>/tacz_tags/attachments/keychain.json
```

Чтобы ограничить скин или брелок конкретным оружием, используйте обычные TACZ tags/правила совместимости для аттачей, как у других категорий.

## Recipes

Файл `data/<namespace>/recipes/attachments/star_charm.json`:

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

Если предмет не должен крафтиться, рецепт можно не добавлять.

## Локализация

```json
{
  "attachment.my_pack.gold_deagle": "Золотой Deagle",
  "attachment.my_pack.gold_deagle.desc": "Золотая отделка для Deagle.",
  "attachment.my_pack.star_charm": "Звёздный брелок",
  "attachment.my_pack.star_charm.desc": "Брелок для оружия."
}
```

Путь: `assets/<namespace>/lang/ru_ru.json`.

## Проверка

1. Положите файлы в обычный TACZ gun pack.
2. Перезагрузите TACZ-паки обычным способом.
3. Откройте refit-интерфейс TACZ.
4. Проверьте категории Skin и Keychain.

Частые ошибки: аттач не добавлен в tag, неверный `"type"` в index, путь к текстуре указан с `.png`, отсутствует display-файл, или `target_gun` не совпадает с ID оружия.
