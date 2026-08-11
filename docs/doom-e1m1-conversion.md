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
hud/*.bmp             # fist/pistol/shotgun/... HUD frames из WAD
doom_conversion.json # размеры, hash исходника и BSP report
```

## Что конвертируется

- classic Doom `VERTEXES`, `LINEDEFS`, `SIDEDEFS`, `SECTORS`;
- front/back стороны, upper/lower/middle wall texture slots;
- Doom `x/y` сохраняются как C3D `x/z` без зеркалирования; player angle
  компенсируется под систему углов движка;
- использованные composite `TEXTURE1` wall textures через `PNAMES` и patch
  lumps;
- использованные 64×64 flats и `SKY1` → C3D sky;
- игроки Doom things `1..4` → `entities.ini`;
- 16 HUD weapon frames: fist, pistol, shotgun, chaingun, rocket, plasma,
  BFG и chainsaw;
- свет секторов `0..255` → C3D `0..15`;
- необязательные Doom sprite patch lumps (`--sprites used|all`).

Каждая полученная world texture масштабируется под ограничения C3D и проходит
детерминированный K-means до 16 цветов, после чего сохраняется BMP4. У E1M1
это даёт примерно **0.6 MB** package с ресурсами вместо 10 MB исходного WAD;
сам `level.c3b` занимает около 25 KB.

## Осознанные упрощения для первого playable import

Classic Doom в этом WAD не имеет slope-поверхностей: сектор хранит только
горизонтальные `floorHeight` и `ceilingHeight`. Поэтому slope support для
E1M1 не нужен и не добавляется в Java ME renderer hot path. По умолчанию
конвертер переносит Doom `REJECT` в симметричный C3D PVS: пара отсекается
только если Doom отвергает её в обе стороны, а прямые portal neighbors всегда
остаются видимыми, в том числе для двери. Это убирает лишние outdoor sky
leaves, которые могли выглядеть как PVS-артефакты. Для диагностики доступен
полностью консервативный режим `--pvs all-visible`.

Обычные E1M1 door lines (`special 1` и совместимые classic door specials)
переносятся в существующий `GameEngine` type-1 door controller: закрытый Doom
sector остаётся закрытым, а клавиша `1` поднимает его ceiling. Двусторонние
линии без door special остаются статическими порталами. Высоты Doom уменьшаются
в 2 раза, а не-дверные сектора получают минимум 64 единицы clearance — это
делает Doom stairs проходимыми в CovertOps collision model.

E1M1 imp, zombieman и shotgun guy переносятся как существующие CovertOps AI
типы с собственными Doom billboard BMP4 (`sprite.<slot>`). Их исходные Doom
patches масштабируются до 96px высоты при конвертации: это соответствует
физическому масштабу inherited renderer и не требует per-frame масштабирования
на Java ME. Для стабильного первого импорта один Doom frame повторяется во
всех AI frame indexes; обычный combat/movement уже работает через `GameEngine`.
Doom elevators, switches, keys, exit scripting и остальные thing остаются
metadata в `doom_things.ini` до отдельного этапа точного Doom gameplay.

Все восемь Doom weapon slots доступны игроку сразу в E1M1 sandbox:
fist, pistol, shotgun, chaingun, rocket launcher, plasma rifle, BFG9000 и
chainsaw. Их HUD BMP4 patch frames выгружаются в `hud/`. Rocket, plasma и BFG
сразу создают видимый world projectile с отдельным Doom BMP4, а imp использует
`BAL1A0` fireball вместо legacy Covert projectile. Точные spread, splash
falloff и BFG tracer states останутся следующим gameplay-проходом.

`--sprites used|all` по-прежнему экспортирует дополнительные quantized Doom
patch BMP4 для редактора и будущей покадровой анимации, но не нужен для уже
включённых E1M1 врагов.
