# C3D2: новый формат пользовательских уровней

`level_XX`, `tx*` и `sp*` остаются **legacy**-форматами: их можно
импортировать и проверять старым редактором, но новые карты не обязаны
наследовать их ограничения.

Новый pipeline разделяет редактируемые данные и runtime-данные:

```text
custom/<name>/
  level.c3d.json       # исходник редактора
  level.c3b            # скомпилированная карта/BSP для Java ME
  materials.c3m        # manifest внешних материалов
  textures/*.bmp       # indexed BMP4/BMP8
```

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

В репозитории есть минимальный проверочный пакет:

```text
res/gamedata/custom/demo/materials.c3m
res/gamedata/custom/demo/textures/
```

## План C3B

`level.c3b` будет компактным бинарным runtime-пакетом с прямыми node/leaf
ссылками и explicit root index. BSP построитель останется integer,
Doom-style и детерминированным. `shapely` допустим как опциональный
валидатор геометрии в редакторе, но не как обязательная зависимость и не как
основа BSP: float-геометрия даёт sliver-сегменты и нестабильные texture offset.
