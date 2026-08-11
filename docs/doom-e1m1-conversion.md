# Doom E1M1 → C3D2

`docs/DOOM.WAD` — исходный материал конвертера, а не runtime-ресурс. Java ME
JAR не включает `docs/`, поэтому игра не читает WAD и не несёт его размер.

```bash
# Генерация компактного package по умолчанию
python3 scripts/convert_doom_e1m1.py

# Явные пути и другая classic Doom карта
python3 scripts/convert_doom_e1m1.py docs/DOOM.WAD \
    res/gamedata/custom/doom-e1m1 --map E1M1

# Необязательная выгрузка Doom patch sprites для дальнейшей работы
python3 scripts/convert_doom_e1m1.py --sprites used
# --sprites all экспортирует весь S_START..S_END namespace, но не нужен для ходьбы.
```

Результат находится в `res/gamedata/custom/doom-e1m1/` и назначен как
первый custom level (`New Game`) в `MainGameCanvas`:

```text
level.c3d.json       # 2D геометрия/сектора/стены
level.c3b            # BSP-компиляция для Java ME
entities.ini         # Doom player starts 1..4 → C3D spawn 1..4
materials.c3m        # только использованные E1M1 wall/flat/sky BMP4
textures/*.bmp       # 16-цветные indexed BMP4

doom_materials.ini   # Doom texture/flat name → C3D material slot
doom_things.ini      # остальные Doom things как source metadata
doom_conversion.json # размеры, hash исходника и BSP report
```

## Что конвертируется

- classic Doom `VERTEXES`, `LINEDEFS`, `SIDEDEFS`, `SECTORS`;
- front/back стороны, upper/lower/middle wall texture slots;
- использованные composite `TEXTURE1` wall textures через `PNAMES` и patch
  lumps;
- использованные 64×64 flats и `SKY1` → C3D sky;
- игроки Doom things `1..4` → `entities.ini`;
- свет секторов `0..255` → C3D `0..15`;
- необязательные Doom sprite patch lumps (`--sprites used|all`).

Каждая полученная world texture масштабируется под ограничения C3D и проходит
детерминированный K-means до 16 цветов, после чего сохраняется BMP4. У E1M1
это даёт примерно **0.6 MB** package с ресурсами вместо 10 MB исходного WAD;
сам `level.c3b` занимает около 25 KB.

## Осознанные упрощения для первого playable import

Classic Doom в этом WAD не имеет slope-поверхностей: сектор хранит только
горизонтальные `floorHeight` и `ceilingHeight`. Поэтому slope support для
E1M1 не нужен и не добавляется в Java ME renderer hot path.

Doom line specials, двери, лифты, ключи, монстры и combat-логика не
переносятся в CovertOps gameplay. Конвертер делает двусторонние линии
статическими порталами, уменьшает Doom высоты в 2 раза и гарантирует минимум
64 единицы clearance. Это сохраняет планировку, лестницы и возможность ходить
по E1M1 без реализации Doom scripting/doors. Исходные thing и special данные
сохраняются в metadata-файлах, чтобы их можно было добавить отдельным этапом.

Custom object sprite runtime пока не включён: `--sprites` экспортирует
квантованные Doom patch BMP4 для редактора/будущего материала, но не добавляет
их как игровых врагов. Для первой цели — загрузка уровня, текстур и spawn,
чтобы походить — это намеренно не требуется.
