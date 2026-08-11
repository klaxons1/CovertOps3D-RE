# C3D2: новый формат пользовательских уровней

`level_XX`, `tx*` и `sp*` остаются **legacy**-форматами: их можно
импортировать и проверять старым редактором, но новые карты не обязаны
наследовать их ограничения.

Новый pipeline разделяет редактируемые данные и runtime-данные:

```text
custom/<name>/
  level.c3d.json       # исходник геометрии редактора
  level.c3b            # скомпилированная геометрия/BSP для Java ME
  entities.ini         # координаты спавнов и объектов
  materials.c3m        # manifest внешних материалов
  textures/*.bmp       # indexed BMP4/BMP8
```

## Ориентация стен

Координаты вершины записываются как `[x, z]`. У `front`-поверхности сектор
должен находиться **справа** от направленного ребра `start -> end`; у `back`
сторона автоматически идёт в обратном направлении. Поэтому внешний контур
обычной комнаты рисуется **по часовой стрелке** в плоскости `x,z`.

Это не косметика: текущий быстрый `PortalRenderer` принимает только
проецируемые слева-направо front-сегменты. Контур против часовой стрелки может
успешно пройти BSP-компиляцию, но все стены будут отсечены как back-to-front и
экран останется чёрным. `c3d2_core.py` проверяет это до записи `.c3b` и выдаёт
понятную ошибку; редактор должен показывать направление стен стрелками.

## Энтити в отдельном INI

Координаты и параметры энтити не участвуют в BSP, поэтому новый C3D2 source
не хранит их в `level.c3d.json`, а указывает только путь:

```json
"entities": "entities.ini"
```

`entities.ini` — UTF-8, комментарии начинаются с `#` или `;`. У каждого
объекта есть отдельная секция; `x`, `z` и `type` обязательны, `angle` и
`param` по умолчанию равны нулю. Все значения — signed `int16`, как и в
runtime.

```ini
# C3D entity placement v1
[entities]
format=C3D-ENTITIES-1

[entity.0]
x=0
z=0
angle=0
type=1
param=0
```

`type=1..4` — точки старта игрока по вариантам сложности. При компиляции C3B
получает header flag `external entities`, нулевой счётчик inline-объектов и
относительный путь к INI; загрузчик Java ME читает INI отдельно. Поэтому
перемещение спавна/предмета не меняет геометрию или BSP. Ранние C3B v1 с
inline-объектами (`flags=0`) остаются читаемы для совместимости.

Сейчас external material pipeline поставляет wall/flat/sky; позиции любых
типов объектов уже читаются из INI, но полноценные custom sprite/material
описания для врагов и предметов будут добавлены отдельным этапом. Spawn-точки
работают полностью уже сейчас.

## Внешние материалы

`materials.c3m` - UTF-8 текстовый manifest. Пустые строки, `#` и `;`
игнорируются. Пути относительны самому manifest.

```ini
wall.1=textures/brick.bmp
wall.2=textures/steel_door.bmp
flat.1=textures/floor.bmp
flat.2=textures/ceiling.bmp
sky=textures/sky.bmp
```

Ограничения runtime (они намеренно простые и детерминированные):

| Вид | Формат | Размер |
| --- | --- | --- |
| `wall.<slot>` | indexed BMP4/BMP8, используются индексы 0..15 | power-of-two ширина, высота 16/64/128 |
| `flat.<slot>` | indexed BMP4/BMP8, используются индексы 0..15 | 64×64 |
| `sky` | indexed BMP4/BMP8, используются индексы 0..15 | 64×128 |

PNG остаётся удобным исходником для художника и редактора, но экспортируется
в BMP4 через `scripts/png_to_bmp4.py`. В Java ME `CustomMaterialSet` грузит
BMP напрямую через существующий `BMPLoader`; атлас не требуется.

Материал сохраняет palette indices, поэтому получает те же 16 световых строк
`Texture.createColorPalettes`, что и стоковые текстуры. `flat` режим качества
также работает с внешними полами/потолками.

Поля C3D `floor_material`/`floor_sky` всегда означают физический пол, а
`ceiling_material`/`ceiling_sky` — физический потолок. Внутренние имена
наследованного `PortalRenderer` вертикально инвертированы; `CustomLevelLoader`
адаптирует их один раз во время загрузки, не меняя legacy-карты. Поэтому C3D
автору не нужно помнить о старом перепутанном представлении.

В репозитории есть минимальный проверочный пакет:

```text
res/gamedata/custom/demo/materials.c3m
res/gamedata/custom/demo/textures/
```

## Runtime entry point

Java ME code can load a package directly through:

```java
levelResourceManager.loadCustomLevelResources(
    "/gamedata/custom/demo/level.c3b"
);
```

This loads `level.c3b`, resolves its relative `materials.c3m` and, for new
packages, `entities.ini`, installs loose wall/flat/sky BMPs, builds
`GameWorld`, and then uses the existing `PortalRenderer`. Stock campaign
loading remains unchanged.

The repository currently routes campaign slot 0 (New Game) to the demo C3B
package, so the complete custom path is exercised by normal game startup.
The legacy `level_01a` is retained as an import/compatibility asset.

## C3B v1 runtime layout

`level.c3b` уже генерируется `scripts/c3d2_core.py`. Он little-endian и не
использует legacy section-size blocks.

```text
C3B1 magic          4 bytes
version             u8 (=1)
flags               u8 (bit 0 = external entities)
rootNode            s16
counts              8 x u16
materialPathLength  u16
materialPath        UTF-8 bytes, relative to C3B
entityPathLength    u16, only if flags & 1
entityPath          UTF-8 bytes, only if flags & 1
vertices            x,z: s16,s16
walls               start,end:u16; front,back:s16; flags,type,special,reserved:u8
objects             x,z,angle,type,param: s16; only if flags & 1 == 0
surfaces            offsetX,offsetY:s16; upper,lower,main,reserved:u8; sector:u16
sectors             floor,ceiling:s16; floorSlot,ceilingSlot,light,flags:u8; tag,type:s16
nodes               x,z,dx,dz,frontChild,backChild: s16
leaves              sector,firstSegment,segmentCount: u16
segments            start,end,definition:u16; frontFacing:u8; textureOffset:s16
pvsByteCount        u32
PVS                 LSB-first, bit from*sectorCount+to, 1=visible
```

При `flags & 1` поле `objects` count равно нулю: координаты находятся только
в указанном INI. Node children не используют `0x8000` на диске: `>=0` означает
node index, `<0` означает leaf index `-child-1`. Корень хранится явно, поэтому
node array можно свободно перестраивать. Для текущего Java renderer loader
преобразует эти ссылки во внутреннее legacy-представление только в памяти.

### Преимущества перед legacy

| Legacy | C3D2/C3B |
| --- | --- |
| размер каждой секции хранится в байтах, поля надо знать вручную | фиксированные records и явные counts |
| root = последний node, leaf sector выводится из первого сегмента | explicit root и explicit `leaf.sectorId` |
| child использует high-bit tag, PVS имеет обратную семантику | signed child refs, PVS `1=visible` |
| surface может ссылаться только на byte sector ID | surface sector ID `u16` |
| текстуры требуют глобальных tx/sp atlas | отдельный manifest и BMP рядом с уровнем |
| координаты объектов смешаны с геометрией | `entities.ini` рядом с C3B, BSP от них не зависит |
| формат данных и формат редактора смешаны | JSON source отдельно от compiled C3B |
| BSP rebuild привязан к байт-в-байт legacy compatibility | чистый deterministic integer builder |

BSP построитель остаётся integer, Doom-style и детерминированным. `shapely`
допустим как **опциональный** валидатор геометрии в редакторе, но не как
обязательная зависимость и не как основа BSP: float-геометрия даёт
sliver-сегменты и нестабильные texture offset.
