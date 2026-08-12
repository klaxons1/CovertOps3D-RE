# Doom E1M1/E1M2 → C3D2

`docs/DOOM.WAD` — исходный материал конвертера, а не runtime-ресурс. Java ME
JAR не включает `docs/`, поэтому игра не читает WAD и не несёт его размер.

```bash
# Генерация компактного package по умолчанию
python3 scripts/convert_doom_e1m1.py

# Явные пути для первого и второго уровня эпизода
python3 scripts/convert_doom_e1m1.py docs/DOOM.WAD \
    res/gamedata/custom/doom-e1m1 --map E1M1
python3 scripts/convert_doom_e1m1.py docs/DOOM.WAD \
    res/gamedata/custom/doom-e1m2 --map E1M2

# Необязательная выгрузка Doom patch sprites для дальнейшей работы
python3 scripts/convert_doom_e1m1.py --sprites used
# --sprites all экспортирует весь S_START..S_END namespace, но не нужен для ходьбы.
```

Результаты находятся в `res/gamedata/custom/doom-e1m1/` и
`res/gamedata/custom/doom-e1m2/`. `New Game` запускает E1M1, второй пункт
главы — E1M2, а normal exit switch E1M1 переводит игрока на E1M2:

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
- все видимые things E1M1/E1M2: barrels, medkits, ammo, armor, weapon pickups,
  bonus/decorative props как C3D billboard entities;
- свет секторов `0..255` → C3D `0..15`;
- необязательные Doom sprite patch lumps (`--sprites used|all`).

Каждая полученная world texture масштабируется под ограничения C3D и проходит
детерминированный K-means до 16 цветов, после чего сохраняется BMP4. В runtime
попадает только package конкретной карты; исходный WAD размером около 10 MB
никогда не читается Java ME. `level.c3b` E1M1 занимает около 25 KB, E1M2 —
около 60 KB.

## Осознанные упрощения для первого playable import

Classic Doom в этом WAD не имеет slope-поверхностей: сектор хранит только
горизонтальные `floorHeight` и `ceilingHeight`. Поэтому slope support для
E1M1 не нужен и не добавляется в Java ME renderer hot path. По умолчанию
конвертер переносит Doom `REJECT` в симметричный C3D PVS: пара отсекается
только если Doom отвергает её в обе стороны, а прямые portal neighbors всегда
остаются видимыми, в том числе для двери. Это убирает лишние outdoor sky
leaves, которые могли выглядеть как PVS-артефакты. Для диагностики доступен
полностью консервативный режим `--pvs all-visible`.

Обычные door lines (`special 1` и совместимые classic door specials)
переносятся в существующий `GameEngine` type-1 door controller: закрытый Doom
sector остаётся закрытым, а клавиша `1` поднимает его ceiling. Двусторонние
линии без door special остаются статическими порталами. Высоты Doom уменьшаются
в 2 раза, а не-дверные сектора получают минимум 64 единицы clearance — это
делает Doom stairs проходимыми в CovertOps collision model.

Imp, zombieman и shotgun guy переносятся как существующие AI-типы с
собственными Doom billboard BMP4 (`sprite.<slot>`). Их state patches
масштабируются до 160px высоты при конвертации; статические pickups, barrels
и decorations — до 128px, поэтому не исчезают на расстоянии и не требуют
per-frame scale в Java ME. Для каждого actor выгружаются семь state frames и
четыре промежуточные death frames: runtime проигрывает полный I→J→K→L→M→N
strip перед трупом. `special 11/52` exit switch сохраняется как C3D wall type
11 и запускает normal E1M1→E1M2 transition с сохранением HP/ammo/weapon.
Doom elevators, switches, keys и остальные specials остаются следующим этапом
точного Doom gameplay.

Все восемь Doom weapon slots доступны игроку сразу в E1M1 sandbox:
fist, pistol, shotgun, chaingun, rocket launcher, plasma rifle, BFG9000 и
chainsaw. Их HUD BMP4 patch frames выгружаются в `hud/`. Rocket, plasma и BFG
сразу создают centered world projectile с отдельным Doom BMP4: BFG использует
правильный `BFS1A0` ball (а `BFUGA0` остаётся pickup), imp использует `BAL1A0`
fireball вместо legacy Covert projectile. Ammo, shells, rockets,
cells, medkits, stimpacks, armor, bonus items и weapon pickups уже
конвертируются как collectable Doom items; barrels и decorations остаются
world props. `#` (или `*` на альтернативной раскладке) переключает Doom-only
God Mode; индикатор `GOD` показывается рядом с FPS. Точные spread, splash
falloff и BFG tracer states останутся следующим gameplay-проходом.

`--sprites used|all` по-прежнему экспортирует дополнительные quantized Doom
patch BMP4 для редактора и будущей покадровой анимации, но не нужен для уже
включённых E1M1 врагов.
