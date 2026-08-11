#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CovertOps3D — редактор уровней (GUI на чистом tkinter, без зависимостей).

Запуск из корня репозитория:
    python3 scripts/level_editor.py [путь/к/level_01a] [--resdir res/gamedata]

Возможности:
  * 2D-вид карты: вершины, стены, сектора (цветная заливка по текстуре пола
    и свету), объекты с направлением и именем;
  * редактирование всего: вершины (drag), стены (создание по двум вершинам,
    тип/флаги/special, surface: main/upper/lower текстуры с визуальным
    выбором по атласам, смещения, связи с секторами), сектора (высоты,
    текстуры пола/потолка, свет, tag, type), объекты (тип, угол, param, drag);
  * добавление стен/вершин/объектов, удаление, undo/redo (Ctrl+Z/Ctrl+Y);
  * 3D-предпросмотр (F3): портальный рейкаст с текстурами атласов,
    затенением по свету сектора, небом (id 51), спрайтами объектов;
    WASD — ходьба, QE/стрелки — поворот, F — текстурированные полы,
    требует лишь положить уровень в корректный res/gamedata;
  * сохранение: если геометрия не менялась — файл пишется байт-в-байт как
    был (проверено roundtrip на всех 13 картах); если менялась —
    перестраиваются сегменты/BSP/PVS (Doom-style построитель, листья
    сектор-чистые, PVS консервативно «всё видно»);
  * проверка уровня (ссылочная целостность), статистика.
"""

import copy
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    import tkinter as tk
    from tkinter import filedialog, messagebox, simpledialog, ttk
    TK_OK = True
except Exception:  # нет дисплея/тонкий питон — работаем только headless
    TK_OK = False

import co3d_level_core as C

APP_TITLE = 'CovertOps3D Level Editor'

WALL_TYPES = {0: 'обычная', 1: 'дверь', 11: 'выход (след. уровень)',
              26: 'дверь (зол. ключ)', 28: 'дверь (сер. ключ)',
              51: 'возврат (пред. уровень)', 62: 'лифт'}
SECTOR_TYPES = {0: 'обычный', 10: 'лифт', 555: 'пол наносит урон',
                666: 'триггер выхода'}


# ---------------------------------------------------------------------------
# Вспомогательное: tk.PhotoImage из RGB-байт
# ---------------------------------------------------------------------------

def photo_from_rgb(rgb, w, h, scale=1):
    """PhotoImage из RGB буфера. Пробуем PPM P6 напрямую (Tk 8.6 умеет),
    fallback — base64."""
    if scale > 1:
        out = bytearray()
        for y in range(h):
            row = bytearray()
            r0 = y * w * 3
            for x in range(w):
                c = bytes(rgb[r0 + x * 3: r0 + x * 3 + 3])
                row += c * scale
            for _ in range(scale):
                out += row
        rgb = bytes(out)
        w *= scale
        h *= scale
    ppm = b'P6 %d %d 255\n' % (w, h) + bytes(rgb)
    try:
        return tk.PhotoImage(data=ppm, format='PPM')
    except Exception:
        import base64
        b64 = base64.b64encode(ppm)
        return tk.PhotoImage(data=b64, format='PPM')


def mk_level_new():
    """Новый уровень: квадратная комната 256x256 высотой 96."""
    lv = C.Level()
    lv.vertices = [(0, 0), (256, 0), (256, 256), (0, 256)]
    lv.sectors = [dict(floor=0, ceil=96, ceil_tex=1, floor_tex=1,
                       light_packed=9 << 4, tag=0, type=0)]
    for i in range(4):
        lv.surfaces.append(dict(ox=0, oy=0, upper=27, lower=27, main=27,
                                sector=0))
        lv.walls.append(dict(sv=i, ev=(i + 1) % 4, flags=1, type=0,
                             special=0, front=len(lv.surfaces) - 1, back=-1))
    lv.objects.append(dict(x=128, z=128, angle=0, type=1, param=0))
    n = 1
    lv.pvs = [bytearray(n) for _ in range(n)]
    return lv


# ---------------------------------------------------------------------------
# Диалог выбора текстуры
# ---------------------------------------------------------------------------

class TexturePicker(object):
    """Сетка миниатюр текстур. mode='wall'|'flat'. Возвращает id или None."""

    def __init__(self, parent, assets, mode='wall', title='Текстура'):
        self.result = None
        top = self.top = tk.Toplevel(parent)
        top.title(title)
        top.transient(parent.winfo_toplevel())
        top.grab_set()
        frame = tk.Frame(top)
        frame.pack(fill='both', expand=True)
        canv = tk.Canvas(frame, width=480, height=360, bg='#202020')
        sb = tk.Scrollbar(frame, orient='vertical', command=canv.yview)
        canv.configure(yscrollcommand=sb.set)
        sb.pack(side='right', fill='y')
        canv.pack(side='left', fill='both', expand=True)
        inner = tk.Frame(canv, bg='#202020')
        canv.create_window((0, 0), window=inner, anchor='nw')
        self._photos = []
        if mode == 'wall':
            ids = sorted(assets.wall_tex.keys())
            imgs = assets.wall_tex
        else:
            ids = sorted(assets.flat_sprites.keys())
            imgs = assets.flat_sprites
        col = row = 0
        cell = 76
        for tid in ids:
            tex = imgs[tid]
            sc = 1
            if tex.w <= 32:
                sc = 2
            ph = photo_from_rgb(_tex_rgb(tex, 8), tex.w, tex.h,
                                scale=max(1, 64 // max(tex.w, tex.h)) or sc)
            self._photos.append(ph)
            cellf = tk.Frame(inner, bg='#303030', bd=1, relief='raised')
            cellf.grid(row=row, column=col, padx=2, pady=2)
            lbl = tk.Label(cellf, image=ph, bg='#303030')
            lbl.pack()
            tk.Label(cellf, text='id %d (%dx%d)' % (tid, tex.w, tex.h),
                     bg='#303030', fg='#dddddd',
                     font=('TkDefaultFont', 7)).pack()
            def on_click(ev, t=tid):
                self.result = t
                top.destroy()
            lbl.bind('<Button-1>', on_click)
            for ch in cellf.winfo_children():
                ch.bind('<Button-1>', on_click)
            cellf.bind('<Button-1>', on_click)
            col += 1
            if col >= 5:
                col = 0
                row += 1
        inner.update_idletasks()
        canv.configure(scrollregion=canv.bbox('all'))
        bot = tk.Frame(top)
        bot.pack(pady=4)
        if mode == 'flat':
            def sky():
                self.result = 51
                top.destroy()
            tk.Button(bot, text='Небо (id 51)',
                      command=sky).pack(side='left', padx=6)
        tk.Button(bot, text='Отмена', command=top.destroy).pack(side='left')
        top.wait_window()


def _tex_rgb(tex, light):
    shades = tex.shaded[min(15, max(0, light))]
    w, h = tex.w, tex.h
    out = bytearray(w * h * 3)
    for y in range(h):
        r = y * w
        o = r * 3
        for x in range(w):
            out[o + x * 3: o + x * 3 + 3] = shades[tex.pixels[r + x]]
    return out


# ---------------------------------------------------------------------------
# 3D предпросмотр
# ---------------------------------------------------------------------------

class View3D(object):
    def __init__(self, editor):
        self.ed = editor
        top = self.top = tk.Toplevel(editor.root)
        top.title('3D предпросмотр — WASD движение, Q/E или ←/→ поворот')
        self.W, self.H = 240, 288
        self.canvas = tk.Canvas(top, width=self.W * 2, height=self.H * 2,
                                bg='black', highlightthickness=0)
        self.canvas.pack()
        self.status = tk.Label(top, text='', anchor='w')
        self.status.pack(fill='x')
        self.cam = None
        self.reset_cam()
        self.textured_flats = False
        self.sprites = True
        self.keys = set()
        self._photo = None
        self._img_item = None
        top.bind('<KeyPress>', self._down)
        top.bind('<KeyRelease>', self._up)
        top.protocol('WM_DELETE_WINDOW', self.close)
        self._closed = False
        self._tick()

    def reset_cam(self):
        self.cam = C.spawn_camera(self.ed.level)

    def close(self):
        self._closed = True
        self.top.destroy()

    def _down(self, ev):
        k = ev.keysym.lower()
        self.keys.add(k)
        if k == 'f':
            self.textured_flats = not self.textured_flats
            self.render()
        elif k == 'o':
            self.sprites = not self.sprites
            self.render()
        elif k == 'r':
            self.reset_cam()
            self.render()

    def _up(self, ev):
        self.keys.discard(ev.keysym.lower())

    def _tick(self):
        if self._closed:
            return
        moved = False
        sp = 12.0
        if 'w' in self.keys or 'up' in self.keys:
            self.cam.x += math.cos(self.cam.angle) * sp
            self.cam.z += math.sin(self.cam.angle) * sp
            moved = True
        if 's' in self.keys or 'down' in self.keys:
            self.cam.x -= math.cos(self.cam.angle) * sp
            self.cam.z -= math.sin(self.cam.angle) * sp
            moved = True
        if 'a' in self.keys:
            self.cam.x += math.cos(self.cam.angle + math.pi / 2) * sp
            self.cam.z += math.sin(self.cam.angle + math.pi / 2) * sp
            moved = True
        if 'd' in self.keys:
            self.cam.x -= math.cos(self.cam.angle + math.pi / 2) * sp
            self.cam.z -= math.sin(self.cam.angle + math.pi / 2) * sp
            moved = True
        if 'q' in self.keys or 'left' in self.keys:
            self.cam.angle -= 0.12
            moved = True
        if 'e' in self.keys or 'right' in self.keys:
            self.cam.angle += 0.12
            moved = True
        if moved:
            self.render()
        self.top.after(50, self._tick)

    def render(self):
        ed = self.ed
        if ed.level is None or ed.assets is None:
            return
        # глаз на полу текущего сектора (+32)
        edges = ed.level.sectors_edges()
        sec = None
        for s in range(len(ed.level.sectors)):
            if C.point_in_sector(ed.level, edges, s, self.cam.x, self.cam.z):
                sec = s
                break
        light = 0
        if sec is not None:
            sdat = ed.level.sectors[sec]
            self.cam.eye = sdat['floor'] + 32
            light = (sdat['light_packed'] >> 4) & 15
        buf = C.render_view(ed.level, ed.assets, self.cam, self.W, self.H,
                            textured_flats=self.textured_flats,
                            draw_sprites=self.sprites)
        self._photo = photo_from_rgb(buf, self.W, self.H, scale=2)
        if self._img_item is None:
            self._img_item = self.canvas.create_image(
                0, 0, anchor='nw', image=self._photo)
        else:
            self.canvas.itemconfig(self._img_item, image=self._photo)
        self.status.config(
            text='x=%d z=%d sec=%s light=%d  [F]полы=%s [O]объекты=%s'
                 % (self.cam.x, self.cam.z,
                    str(sec), light,
                    'текст' if self.textured_flats else 'плоск',
                    'да' if self.sprites else 'нет'))


# ---------------------------------------------------------------------------
# Главный редактор
# ---------------------------------------------------------------------------

class Editor(object):
    def __init__(self, root, resdir):
        self.root = root
        self.resdir = resdir
        self.assets = None
        self.level = None
        self.level_path = None
        self.geometry_dirty = False
        self.content_dirty = False
        self.undo_stack = []
        self.redo_stack = []
        self.mode = 'select'        # select/wall/object/texture/delete
        self.selection = None       # (kind, index)
        self.active_sector = 0
        self.wall_start_vertex = None
        self.obj_type = 26
        self.paint_tex = 27
        self.paint_part = 'main'
        self.grid = 8
        self.snap = True
        self.show_fill = True
        self.show_objects = True
        self.show_ids = False
        self.zoom = 0.5             # px на мировую единицу
        self.origin = [80, 80]      # мировая точка (0,0) на экране? нет:
        self.ox, self.oy = 60.0, 60.0   # смещение мира на канвасе (px)
        self._drag = None
        self._pan = None
        self.view3d = None
        self._fill_img = None
        self._fill_key = None

        self._build_ui(resdir)
        if self.assets is None:
            self.load_assets(resdir, quiet=True)

    # ---------------- UI ----------------

    def _build_ui(self, resdir):
        self.root.title(APP_TITLE)
        men = tk.Menu(self.root)
        m_file = tk.Menu(men, tearoff=0)
        m_file.add_command(label='Новый уровень', command=self.new_level)
        m_file.add_command(label='Открыть…', command=self.open_dialog,
                           accelerator='Ctrl+O')
        m_file.add_command(label='Сохранить', command=self.save,
                           accelerator='Ctrl+S')
        m_file.add_command(label='Сохранить как…', command=self.save_as)
        m_file.add_separator()
        m_file.add_command(label='Перезагрузить текстуры',
                           command=lambda: self.load_assets(self.resdir))
        m_file.add_command(label='Атласы из папки…',
                           command=self.pick_resdir)
        m_file.add_separator()
        m_file.add_command(label='Выход', command=self.root.quit)
        men.add_cascade(label='Файл', menu=m_file)
        m_edit = tk.Menu(men, tearoff=0)
        m_edit.add_command(label='Отменить', command=self.undo,
                           accelerator='Ctrl+Z')
        m_edit.add_command(label='Повторить', command=self.redo,
                           accelerator='Ctrl+Y')
        men.add_cascade(label='Правка', menu=m_edit)
        m_tools = tk.Menu(men, tearoff=0)
        m_tools.add_command(label='3D предпросмотр', command=self.open_3d,
                            accelerator='F3')
        m_tools.add_command(label='Перестроить BSP+PVS сейчас',
                            command=self.rebuild_now)
        m_tools.add_command(label='Добавить сектор',
                            command=self.add_sector)
        m_tools.add_command(label='Проверить уровень', command=self.validate)
        m_tools.add_command(label='Статистика', command=self.show_stats)
        men.add_cascade(label='Инструменты', menu=m_tools)
        men_v = tk.Menu(men, tearoff=0)
        self._v_fill = tk.BooleanVar(value=True)
        self._v_obj = tk.BooleanVar(value=True)
        self._v_ids = tk.BooleanVar(value=False)
        self._v_snap = tk.BooleanVar(value=True)
        men_v.add_checkbutton(label='Заливка секторов', var=self._v_fill,
                              command=self._toggles)
        men_v.add_checkbutton(label='Объекты', var=self._v_obj,
                              command=self._toggles)
        men_v.add_checkbutton(label='Подписи индексов', var=self._v_ids,
                              command=self._toggles)
        men_v.add_checkbutton(label='Привязка к сетке', var=self._v_snap,
                              command=self._toggles)
        men.add_cascade(label='Вид', menu=men_v)
        self.root.config(menu=men)

        # панель инструментов
        tb = tk.Frame(self.root, relief='raised', bd=1)
        tb.pack(side='top', fill='x')
        self._mode_btns = {}
        for mid, txt in (('select', 'Выбор [S]'), ('wall', 'Стена [W]'),
                         ('object', 'Объект [O]'), ('texture', 'Текстура [T]'),
                         ('delete', 'Удалить [D]')):
            b = tk.Button(tb, text=txt, width=12,
                          command=lambda m=mid: self.set_mode(m))
            b.pack(side='left')
            self._mode_btns[mid] = b
        tk.Button(tb, text='3D [F3]', width=8,
                  command=self.open_3d).pack(side='left', padx=8)
        tk.Label(tb, text='сетка:').pack(side='left')
        self.grid_var = tk.StringVar(value='8')
        sp = tk.Spinbox(tb, from_=1, to=64, width=3,
                        textvariable=self.grid_var,
                        command=self._grid_changed)
        sp.pack(side='left')
        tk.Label(tb, text='масштаб:').pack(side='left')
        tk.Button(tb, text='-', width=2,
                  command=lambda: self.set_zoom(self.zoom / 1.25)
                  ).pack(side='left')
        tk.Button(tb, text='+', width=2,
                  command=lambda: self.set_zoom(self.zoom * 1.25)
                  ).pack(side='left')
        # тип ставимого объекта
        tk.Label(tb, text='  объект:').pack(side='left')
        onames = ['%d %s' % (t, C.OBJ_NAMES.get(t, ''))
                  for t in sorted(C.OBJ_NAMES)]
        self.obj_type_var = tk.StringVar(value='26 Бочка')
        om = ttk.Combobox(tb, values=onames, state='readonly', width=16,
                          textvariable=self.obj_type_var)
        om.pack(side='left')
        om.bind('<<ComboboxSelected>>', lambda e: self._obj_type_changed())
        # кисть текстур
        tk.Button(tb, text='Кисть…', command=self.pick_brush).pack(
            side='left', padx=(8, 0))
        self.paint_part_var = tk.StringVar(value='main')
        om2 = ttk.Combobox(tb, values=['main', 'upper', 'lower'],
                           state='readonly', width=6,
                           textvariable=self.paint_part_var)
        om2.pack(side='left')
        om2.bind('<<ComboboxSelected>>', lambda e: self._paint_part_changed())

        main = tk.PanedWindow(self.root, orient='horizontal',
                              sashrelief='raised')
        main.pack(fill='both', expand=True)
        left = tk.Frame(main)
        right = tk.Frame(main, width=280)
        main.add(left, minsize=400)
        main.add(right, minsize=240)

        self.canvas = tk.Canvas(left, bg='#181818', highlightthickness=0)
        hsb = tk.Scrollbar(left, orient='horizontal',
                           command=self.canvas.xview)
        vsb = tk.Scrollbar(left, orient='vertical',
                           command=self.canvas.yview)
        self.canvas.configure(xscrollcommand=hsb.set,
                              yscrollcommand=vsb.set)
        hsb.pack(side='bottom', fill='x')
        vsb.pack(side='right', fill='y')
        self.canvas.pack(fill='both', expand=True)

        self.props = tk.Frame(right, relief='sunken', bd=1)
        self.props.pack(fill='both', expand=True, padx=2, pady=2)
        self.props_lbl = tk.Label(self.props, text='Ничего не выбрано',
                                  anchor='nw', justify='left')
        self.props_lbl.pack(fill='both', expand=True)

        self.status = tk.Label(self.root, text='Откройте уровень',
                               anchor='w', relief='sunken')
        self.status.pack(side='bottom', fill='x')

        cv = self.canvas
        cv.bind('<Button-1>', self.on_click)
        cv.bind('<B1-Motion>', self.on_drag)
        cv.bind('<ButtonRelease-1>', self.on_release)
        cv.bind('<Button-3>', self.on_pan_start)
        cv.bind('<B3-Motion>', self.on_pan)
        cv.bind('<MouseWheel>', self.on_wheel)
        cv.bind('<Button-4>', lambda e: self.on_wheel(e, 1))
        cv.bind('<Button-5>', lambda e: self.on_wheel(e, -1))
        cv.bind('<Motion>', self.on_move)
        self.root.bind('<Control-o>', lambda e: self.open_dialog())
        self.root.bind('<Control-s>', lambda e: self.save())
        self.root.bind('<Control-z>', lambda e: self.undo())
        self.root.bind('<Control-y>', lambda e: self.redo())
        self.root.bind('<F3>', lambda e: self.open_3d())
        self.root.bind('<Key>', self.on_key)

    def _toggles(self):
        self.show_fill = self._v_fill.get()
        self.show_objects = self._v_obj.get()
        self.show_ids = self._v_ids.get()
        self.snap = self._v_snap.get()
        self._fill_key = None
        self.redraw()

    def _grid_changed(self):
        try:
            self.grid = max(1, int(self.grid_var.get()))
        except ValueError:
            pass

    def _obj_type_changed(self):
        try:
            self.obj_type = int(self.obj_type_var.get().split(' ')[0])
        except (ValueError, IndexError):
            pass

    def _paint_part_changed(self):
        self.paint_part = self.paint_part_var.get()

    def pick_brush(self):
        if self.assets is None:
            messagebox.showwarning(APP_TITLE, 'Сначала загрузите атласы')
            return
        p = TexturePicker(self.props, self.assets, mode='wall',
                          title='Кисть: текстура')
        if p.result is not None:
            self.paint_tex = p.result
            self.set_status('кисть = текстура %d (часть: %s)'
                            % (p.result, self.paint_part))

    def add_sector(self):
        if self.level is None:
            return
        self.snapshot()
        base = self.level.sectors[self.active_sector] if (
            0 <= self.active_sector < len(self.level.sectors)) else None
        if base is not None:
            sd = dict(base)
        else:
            sd = dict(floor=0, ceil=96, ceil_tex=1, floor_tex=1,
                      light_packed=9 << 4, tag=0, type=0)
        self.level.sectors.append(sd)
        n = len(self.level.sectors)
        # PVS вырастет при сохранении (перестроится); прямоугольную
        # расширим нулями, чтобы валидация не споткнулась
        for row in self.level.pvs:
            row.append(0)
        self.level.pvs.append(bytearray(n))
        self.active_sector = n - 1
        self.selection = ('sector', self.active_sector)
        self.geometry_dirty = True
        self.content_dirty = True
        self.show_props()
        self.redraw()
        self.set_status('сектор %d создан (копия активного); рисуйте его '
                        'стены в режиме [W]' % self.active_sector)

    # ---------------- координаты ----------------

    def w2s(self, x, z):
        return (self.ox + x * self.zoom, self.oy + z * self.zoom)

    def s2w(self, sx, sy):
        return ((sx - self.ox) / self.zoom, (sy - self.oy) / self.zoom)

    def set_zoom(self, z):
        self.zoom = max(0.05, min(8.0, z))
        self._fill_key = None
        self.redraw()

    # ---------------- загрузка ----------------

    def load_assets(self, resdir, quiet=False):
        if not resdir or not os.path.isdir(os.path.join(resdir, 'textures')):
            if not quiet:
                messagebox.showwarning(APP_TITLE,
                                       'Папка с атласами не найдена: %s'
                                       % resdir)
            self.assets = None
            return
        self.resdir = resdir
        self.assets = C.Assets.load(resdir)
        if not quiet:
            messagebox.showinfo(
                APP_TITLE,
                'Атласы загружены: стен %d, полов %d, объектов %d%s'
                % (len(self.assets.wall_tex), len(self.assets.flat_sprites),
                   len(self.assets.obj_tex),
                   ('\nПредупреждения:\n' + '\n'.join(self.assets.errors))
                   if self.assets.errors else ''))
        self._fill_key = None
        self.redraw()

    def pick_resdir(self):
        d = filedialog.askdirectory(title='Папка res/gamedata')
        if d:
            self.load_assets(d)

    def guess_resdir(self, level_path):
        # .../res/gamedata/levels/level_XX -> .../res/gamedata
        p = os.path.dirname(os.path.abspath(level_path))
        if os.path.basename(p) == 'levels':
            return os.path.dirname(p)
        cand = os.path.join(os.getcwd(), 'res', 'gamedata')
        return cand if os.path.isdir(cand) else self.resdir

    def open_dialog(self):
        p = filedialog.askopenfilename(
            title='Открыть уровень',
            filetypes=[('level_*', 'level_*'), ('все файлы', '*.*')])
        if p:
            self.open_level(p)

    def open_level(self, path):
        data = open(path, 'rb').read()
        lv = C.parse_level(data)
        self.level = lv
        self.level_path = path
        self.geometry_dirty = False
        self.content_dirty = False
        self.undo_stack = []
        self.redo_stack = []
        self.selection = None
        if self.assets is None:
            self.load_assets(self.guess_resdir(path), quiet=True)
        # отцентрируем вид по карте
        xs = [v[0] for v in lv.vertices]
        zs = [v[1] for v in lv.vertices]
        if xs:
            cw = max(400, self.canvas.winfo_width())
            ch = max(300, self.canvas.winfo_height())
            w = max(1, max(xs) - min(xs))
            h = max(1, max(zs) - min(zs))
            self.zoom = min(2.0, max(0.05,
                                     min(cw / float(w), ch / float(h)) * 0.9))
            self.ox = -min(xs) * self.zoom + 30
            self.oy = -min(zs) * self.zoom + 30
        self.root.title('%s — %s' % (APP_TITLE, os.path.basename(path)))
        self.set_status('загружено: %s' % os.path.basename(path))
        self._fill_key = None
        self.redraw()

    def new_level(self):
        if not self._confirm_discard():
            return
        self.level = mk_level_new()
        self.level_path = None
        self.geometry_dirty = True
        self.undo_stack = []
        self.redo_stack = []
        self.selection = None
        self.root.title('%s — <новый>' % APP_TITLE)
        self.redraw()

    def _confirm_discard(self):
        if self.content_dirty or self.geometry_dirty:
            return messagebox.askyesno(APP_TITLE,
                                       'Есть несохранённые изменения. '
                                       'Продолжить?')
        return True

    # ---------------- сохранение ----------------

    def save(self):
        if self.level is None:
            return
        if self.level_path is None:
            return self.save_as()
        self._save_to(self.level_path)

    def save_as(self):
        if self.level is None:
            return
        p = filedialog.asksaveasfilename(title='Сохранить уровень',
                                         initialfile='level_new')
        if p:
            self.level_path = p
            self._save_to(p)

    def _save_to(self, path):
        lv = self.level
        errs, warn = C.validate_level(lv, self.assets)
        if errs:
            if not messagebox.askyesno(
                    APP_TITLE,
                    'Есть ошибки целостности:\n%s\n\nВсё равно сохранить?'
                    % '\n'.join(errs[:10])):
                return
        if self.geometry_dirty:
            try:
                rep = C.rebuild_derived(lv, validate=True)
            except Exception as ex:
                messagebox.showerror(APP_TITLE,
                                     'Не удалось перестроить BSP: %s' % ex)
                return
            note = ('BSP перестроен: узлов %d, листьев %d, сегментов %d, '
                    'split %d, листьев со смешанными секторами %d, '
                    'несовпадений %d'
                    % (len(lv.nodes), len(lv.leaves), len(lv.segments),
                       rep.splits, rep.mixed_leaves, len(rep.fail_samples)))
            if rep.mixed_leaves or rep.fail_samples:
                if not messagebox.askyesno(APP_TITLE, note +
                                           '\n\nСохранить всё равно?'):
                    return
            else:
                self.set_status(note)
        try:
            if os.path.exists(path):
                bak = path + '.bak'
                with open(bak, 'wb') as f:
                    f.write(open(path, 'rb').read())
            data = C.dump_level(lv)
            with open(path, 'wb') as f:
                f.write(data)
        except Exception as ex:
            messagebox.showerror(APP_TITLE, 'Ошибка записи: %s' % ex)
            return
        self.geometry_dirty = False
        self.content_dirty = False
        self.set_status('сохранено %s (%d байт)'
                        % (os.path.basename(path), len(data)))

    def rebuild_now(self):
        if self.level is None:
            return
        rep = C.rebuild_derived(self.level, validate=True)
        self.geometry_dirty = False
        self.content_dirty = True
        messagebox.showinfo(
            APP_TITLE,
            'Готово: узлов %d, листьев %d, сегментов %d\nsplit %d, '
            'смешанных листьев %d, несовпадений %d'
            % (len(self.level.nodes), len(self.level.leaves),
               len(self.level.segments), rep.splits, rep.mixed_leaves,
               len(rep.fail_samples)))
        self.redraw()

    def validate(self):
        if self.level is None:
            return
        errs, warn = C.validate_level(self.level, self.assets)
        txt = 'Ошибки (%d):\n%s\n\nПредупреждения (%d):\n%s' % (
            len(errs), '\n'.join(errs[:30]) or '—',
            len(warn), '\n'.join(warn[:30]) or '—')
        messagebox.showinfo(APP_TITLE + ' — проверка', txt)

    def show_stats(self):
        if self.level is None:
            return
        st = self.level.stats()
        messagebox.showinfo(APP_TITLE + ' — статистика',
                            '\n'.join('%s: %d' % kv for kv in st.items()))

    # ---------------- undo/redo ----------------

    def snapshot(self):
        if self.level is not None:
            self.undo_stack.append(copy.deepcopy(self.level))
            if len(self.undo_stack) > 64:
                self.undo_stack.pop(0)
            self.redo_stack = []

    def undo(self):
        if not self.undo_stack:
            return
        self.redo_stack.append(copy.deepcopy(self.level))
        self.level = self.undo_stack.pop()
        self.geometry_dirty = True
        self._fill_key = None
        self.redraw()

    def redo(self):
        if not self.redo_stack:
            return
        self.undo_stack.append(copy.deepcopy(self.level))
        self.level = self.redo_stack.pop()
        self.geometry_dirty = True
        self._fill_key = None
        self.redraw()

    # ---------------- режимы/статусы ----------------

    def set_mode(self, m):
        self.mode = m
        self.wall_start_vertex = None
        for mid, b in self._mode_btns.items():
            b.config(relief='sunken' if mid == m else 'raised')
        hints = {
            'select': 'Клик — выбрать, drag — двигать вершину/объект',
            'wall': 'Клик по вершине (или пустому месту) — начать стену, '
                    'второй клик — закончить. Shift — двусторонняя',
            'object': 'Клик — поставить объект выбранного типа',
            'texture': 'Клик по стене — применить текстуру к %s'
                       % self.paint_part,
            'delete': 'Клик — удалить вершину/стену/объект',
        }
        self.set_status(hints.get(m, ''))

    def set_status(self, txt):
        self.status.config(text=txt)

    def on_key(self, ev):
        k = ev.keysym.lower()
        if k == 's':
            self.set_mode('select')
        elif k == 'w':
            self.set_mode('wall')
        elif k == 'o':
            self.set_mode('object')
        elif k == 't':
            self.set_mode('texture')
        elif k == 'd':
            self.set_mode('delete')
        elif k == 'escape':
            self.wall_start_vertex = None
            self.redraw()

    # ---------------- мышь ----------------

    def on_move(self, ev):
        if self.level is None:
            return
        x, z = self.s2w(self.canvas.canvasx(ev.x),
                        self.canvas.canvasy(ev.y))
        s = C.find_sector_at(self.level, x, z)
        self.set_status('x=%d z=%d  сектор=%s' % (x, z, str(s)))

    def on_wheel(self, ev, direction=None):
        d = direction if direction is not None else (1 if ev.delta > 0 else -1)
        self.set_zoom(self.zoom * (1.11 if d > 0 else 0.9))

    def on_pan_start(self, ev):
        self._pan = (ev.x, ev.y)

    def on_pan(self, ev):
        if self._pan:
            dx = ev.x - self._pan[0]
            dy = ev.y - self._pan[1]
            self.ox += dx
            self.oy += dy
            self._pan = (ev.x, ev.y)
            self.redraw()

    def _nearest_vertex(self, x, z, lim_px=8):
        best = None
        lim = lim_px / self.zoom
        for i, (vx, vz) in enumerate(self.level.vertices):
            d = math.hypot(vx - x, vz - z)
            if d < lim:
                lim = d
                best = i
        return best

    def _nearest_wall(self, x, z, lim_px=8):
        best = None
        lim = lim_px / self.zoom
        for i, w in enumerate(self.level.walls):
            x1, z1 = self.level.vertices[w['sv']]
            x2, z2 = self.level.vertices[w['ev']]
            d = self._pt_seg(x, z, x1, z1, x2, z2)
            if d < lim:
                lim = d
                best = i
        return best

    @staticmethod
    def _pt_seg(px, pz, x1, z1, x2, z2):
        dx, dz = x2 - x1, z2 - z1
        ll = dx * dx + dz * dz
        if ll == 0:
            return math.hypot(px - x1, pz - z1)
        t = max(0.0, min(1.0, ((px - x1) * dx + (pz - z1) * dz) / float(ll)))
        return math.hypot(px - (x1 + t * dx), pz - (z1 + t * dz))

    def _nearest_object(self, x, z, lim_px=10):
        best = None
        lim = lim_px / self.zoom
        for i, ob in enumerate(self.level.objects):
            d = math.hypot(ob['x'] - x, ob['z'] - z)
            if d < lim:
                lim = d
                best = i
        return best

    def on_click(self, ev):
        if self.level is None:
            return
        cx = self.canvas.canvasx(ev.x)
        cy = self.canvas.canvasy(ev.y)
        x, z = self.s2w(cx, cy)
        if self.snap and self.mode in ('wall', 'object'):
            x = round(x / self.grid) * self.grid
            z = round(z / self.grid) * self.grid
        shift = bool(ev.state & 0x0001)

        if self.mode == 'select':
            vi = self._nearest_vertex(x, z)
            oi = self._nearest_object(x, z) if self.show_objects else None
            wi = self._nearest_wall(x, z)
            if vi is not None:
                self.selection = ('vertex', vi)
                self.snapshot()
                self._drag = ('vertex', vi)
            elif oi is not None:
                self.selection = ('object', oi)
                self.snapshot()
                self._drag = ('object', oi)
            elif wi is not None:
                self.selection = ('wall', wi)
                self._drag = None
            else:
                s = C.find_sector_at(self.level, x, z)
                if s is not None:
                    self.selection = ('sector', s)
                    self.active_sector = s
                else:
                    self.selection = None
                self._drag = None
            self.show_props()
            self.redraw()

        elif self.mode == 'wall':
            vi = self._nearest_vertex(x, z)
            if vi is None:
                self.snapshot()
                self.level.vertices.append((int(x), int(z)))
                vi = len(self.level.vertices) - 1
                self.geometry_dirty = True
            if self.wall_start_vertex is None:
                self.wall_start_vertex = vi
                self.set_status('стена от вершины %d — клик по второй' % vi)
            else:
                v1 = self.wall_start_vertex
                self.wall_start_vertex = None
                if v1 != vi:
                    self._add_wall(v1, vi, two_sided=shift)
            self.redraw()

        elif self.mode == 'object':
            self.snapshot()
            self.level.objects.append(dict(x=int(x), z=int(z), angle=0,
                                           type=self.obj_type, param=0))
            self.content_dirty = True
            self.selection = ('object', len(self.level.objects) - 1)
            self.show_props()
            self.redraw()

        elif self.mode == 'texture':
            wi = self._nearest_wall(x, z)
            if wi is not None:
                self.snapshot()
                self._paint_wall(wi, x, z)
                self.content_dirty = True
                self.redraw()

        elif self.mode == 'delete':
            ok = self._delete_at(x, z)
            if ok:
                self.redraw()

    def on_drag(self, ev):
        if self.level is None or self._drag is None:
            return
        kind, idx = self._drag
        x, z = self.s2w(self.canvas.canvasx(ev.x),
                        self.canvas.canvasy(ev.y))
        if kind == 'vertex' and 0 <= idx < len(self.level.vertices):
            nx = max(-32768, min(32767, int(round(x))))
            nz = max(-32768, min(32767, int(round(z))))
            self.level.vertices[idx] = (nx, nz)
            self._geom_touched()
            self.redraw(light=True)
        elif kind == 'object' and 0 <= idx < len(self.level.objects):
            nx = max(-32768, min(32767, int(round(x))))
            nz = max(-32768, min(32767, int(round(z))))
            self.level.objects[idx]['x'] = nx
            self.level.objects[idx]['z'] = nz
            self.content_dirty = True
            self.redraw(light=True)

    def on_release(self, ev):
        if self._drag is not None:
            self._drag = None
            self.redraw()
            self.show_props()

    def _geom_touched(self):
        # drag вызывает часто — снапшот делаем только в on_click начале drag
        self.geometry_dirty = True
        self.content_dirty = True
        self._fill_key = None

    # ---------------- действия режимов ----------------

    def _add_wall(self, v1, v2, two_sided=False):
        lv = self.level
        self.snapshot()
        # сектор по нормали «справа» от направления v1->v2
        x1, z1 = lv.vertices[v1]
        x2, z2 = lv.vertices[v2]
        mx, mz = (x1 + x2) / 2.0, (z1 + z2) / 2.0
        dx, dz = x2 - x1, z2 - z1
        L = max(1.0, math.hypot(dx, dz))
        s_front = C.find_sector_at(lv, mx + dz / L * 2, mz - dx / L * 2)
        if s_front is None:
            s_front = self.active_sector
        lv.surfaces.append(dict(ox=0, oy=0, upper=self.paint_tex,
                                lower=self.paint_tex, main=self.paint_tex,
                                sector=s_front))
        fs = len(lv.surfaces) - 1
        bs = -1
        if two_sided:
            s_back = C.find_sector_at(lv, mx - dz / L * 2, mz + dx / L * 2)
            if s_back is not None and s_back != s_front:
                lv.surfaces.append(dict(ox=0, oy=0, upper=self.paint_tex,
                                        lower=self.paint_tex,
                                        main=self.paint_tex,
                                        sector=s_back))
                bs = len(lv.surfaces) - 1
        lv.walls.append(dict(sv=v1, ev=v2, flags=1, type=0, special=0,
                             front=fs, back=bs))
        self.geometry_dirty = True
        self.content_dirty = True
        self.selection = ('wall', len(lv.walls) - 1)
        self.show_props()
        self.set_status('стена добавлена (%sсторонняя, сектор %d)'
                        % (' дву' if bs >= 0 else ' одно', s_front))

    def _paint_wall(self, wi, x, z):
        """Применить paint_tex к части стены, ближайшей к клику по нормали."""
        lv = self.level
        w = lv.walls[wi]
        x1, z1 = lv.vertices[w['sv']]
        x2, z2 = lv.vertices[w['ev']]
        dx, dz = x2 - x1, z2 - z1
        # какая сторона ближе: «справа» = front
        cross = (x - x1) * dz - (z - z1) * dx
        si = w['back'] if cross > 0 and w['back'] >= 0 else w['front']
        if si < 0:
            self.set_status('у стены нет этой стороны')
            return
        lv.surfaces[si][self.paint_part] = self.paint_tex
        self.set_status('стена %d, surface %d, %s = текстура %d'
                        % (wi, si, self.paint_part, self.paint_tex))

    def _delete_at(self, x, z):
        lv = self.level
        oi = self._nearest_object(x, z) if self.show_objects else None
        vi = self._nearest_vertex(x, z)
        wi = self._nearest_wall(x, z)
        if oi is not None:
            self.snapshot()
            del lv.objects[oi]
            self.content_dirty = True
            self.selection = None
            return True
        if vi is not None:
            used = [i for i, w in enumerate(lv.walls)
                    if w['sv'] == vi or w['ev'] == vi]
            if used:
                if not messagebox.askyesno(
                        APP_TITLE,
                        'Вершина %d используется стенами %s.\n'
                        'Удалить вместе с ними?' % (vi, used)):
                    return False
                self.snapshot()
                for i in sorted(used, reverse=True):
                    del lv.walls[i]
            else:
                self.snapshot()
            del lv.vertices[vi]
            # поправить индексы вершин в стенах
            for w in lv.walls:
                if w['sv'] > vi:
                    w['sv'] -= 1
                if w['ev'] > vi:
                    w['ev'] -= 1
            self.geometry_dirty = True
            self.content_dirty = True
            self.selection = None
            return True
        if wi is not None:
            self.snapshot()
            del lv.walls[wi]
            self.geometry_dirty = True
            self.content_dirty = True
            self.selection = None
            return True
        return False

    # ---------------- 2D отрисовка ----------------

    def redraw(self, light=False):
        if self.level is None:
            return
        cv = self.canvas
        cv.delete('all')
        lv = self.level
        z = self.zoom
        # заливка секторов (кэшированная картинка)
        if self.show_fill and self.assets is not None:
            key = (round(z, 3), round(self.ox, 1), round(self.oy, 1),
                   self._geom_rev())
            if self._fill_key != key and not light:
                self._render_fill()
                self._fill_key = key
            if self._fill_img is not None:
                cv.create_image(0, 0, anchor='nw', image=self._fill_img,
                                tags='fill')
        # сетка
        self._draw_grid()
        # стены
        for i, w in enumerate(lv.walls):
            x1, z1 = self.w2s(*lv.vertices[w['sv']])
            x2, z2 = self.w2s(*lv.vertices[w['ev']])
            color = '#e8e8e8'
            if w['type'] == 1 or (w['flags'] & 8):
                color = '#40d040'         # двери — зелёные
            elif w['type'] in (11, 51):
                color = '#4080ff'         # выходы — синие
            elif w['back'] >= 0:
                color = '#c0c0c0'         # порталы — светло-серые
            if w['flags'] & 16:
                color = '#c080ff'         # секреты
            sel = self.selection == ('wall', i)
            cv.create_line(x1, z1, x2, z2, fill='#ffd040' if sel else color,
                           width=3 if sel else 2, tags='wall')
            if self.show_ids:
                cv.create_text((x1 + x2) / 2 + 4, (z1 + z2) / 2 + 4,
                               text=str(i), fill='#707070',
                               font=('TkDefaultFont', 7))
        # вершины
        for i, (vx, vz) in enumerate(lv.vertices):
            sx, sz = self.w2s(vx, vz)
            r = 3
            sel = self.selection == ('vertex', i)
            cv.create_oval(sx - r, sz - r, sx + r, sz + r,
                           fill='#ff8040' if sel else '#f0f0f0',
                           outline='')
        if self.wall_start_vertex is not None:
            vx, vz = lv.vertices[self.wall_start_vertex]
            sx, sz = self.w2s(vx, vz)
            cv.create_oval(sx - 5, sz - 5, sx + 5, sz + 5,
                           outline='#ff4040', width=2)
        # объекты
        if self.show_objects:
            for i, ob in enumerate(lv.objects):
                sx, sz = self.w2s(ob['x'], ob['z'])
                if 1 <= ob['type'] <= 4:
                    color = '#30c0ff'
                elif ob['type'] >= 3000:
                    color = '#ff5050'
                elif ob['type'] >= 2000:
                    color = '#ffe040'
                else:
                    color = '#50ffa0'
                sel = self.selection == ('object', i)
                r = 5
                cv.create_oval(sx - r, sz - r, sx + r, sz + r,
                               fill=color,
                               outline='#ffffff' if sel else '')
                ang = (-ob['angle'] * 1144 + 102943) / 65536.0
                cv.create_line(sx, sz, sx + math.cos(ang) * 12,
                               sz + math.sin(ang) * 12, fill=color)
                nm = C.OBJ_NAMES.get(ob['type'], str(ob['type']))
                cv.create_text(sx + 8, sz - 8, text=nm, anchor='w',
                               fill=color, font=('TkDefaultFont', 7))
        cv.configure(scrollregion=cv.bbox('all') or (0, 0, 100, 100))

    _geom_rev_n = [0]

    def _geom_rev(self):
        return len(self.level.vertices) * 1000003 + len(self.level.walls)

    def _draw_grid(self):
        cv = self.canvas
        g = self.grid * self.zoom
        if g < 4:
            return
        w = max(self.canvas.winfo_width(), 600)
        h = max(self.canvas.winfo_height(), 400)
        x0 = self.ox % g
        x = x0
        while x < w:
            cv.create_line(x, 0, x, h, fill='#242424')
            x += g
        z0 = self.oy % g
        zz = z0
        while zz < h:
            cv.create_line(0, zz, w, zz, fill='#242424')
            zz += g

    def _render_fill(self):
        """Растровая заливка секторов (scanline even-odd, за один проход
        по рёбрам каждого сектора) средним цветом текстуры пола × свет."""
        lv = self.level
        vw = max(self.canvas.winfo_width(), 640)
        vh = max(self.canvas.winfo_height(), 480)
        self._fill_img = None
        step = 4
        cols = vw // step
        rows = vh // step
        edges = lv.sectors_edges()
        px = bytearray(b'\x18\x18\x18' * (cols * rows))
        zinv = 1.0 / self.zoom
        for s, es in enumerate(edges):
            if not es:
                continue
            sd = lv.sectors[s]
            light = (sd['light_packed'] >> 4) & 15
            c = self.assets.flat_avg_color(sd['floor_tex'], light)
            rgb = bytes((c[0] // 3 + 16, c[1] // 3 + 16, c[2] // 3 + 16))
            # перевести рёбра в экранные координаты ячеек
            sedges = []
            for (x1, z1, x2, z2) in es:
                cx1 = (x1 * self.zoom + self.ox) / step
                cy1 = (z1 * self.zoom + self.oy) / step
                cx2 = (x2 * self.zoom + self.ox) / step
                cy2 = (z2 * self.zoom + self.oy) / step
                sedges.append((cx1, cy1, cx2, cy2))
            for ry in range(rows):
                yy = ry + 0.5
                xs = []
                for (cx1, cy1, cx2, cy2) in sedges:
                    if (cy1 > yy) != (cy2 > yy):
                        xs.append(cx1 + (yy - cy1) * (cx2 - cx1)
                                  / (cy2 - cy1))
                xs.sort()
                base = ry * cols
                for k in range(0, len(xs) - 1, 2):
                    xa = max(0, int(xs[k]))
                    xb = min(cols - 1, int(xs[k + 1]))
                    for rx in range(xa, xb + 1):
                        o = (base + rx) * 3
                        px[o:o + 3] = rgb
        try:
            img = photo_from_rgb(px, cols, rows)
            self._fill_img = img.zoom(step, step)
            self._fill_keep = img  # не даём GC забрать оригинал
        except Exception:
            self._fill_img = None

    # ---------------- панель свойств ----------------

    def show_props(self):
        for ch in self.props.winfo_children():
            ch.destroy()
        if self.level is None or self.selection is None:
            self.props_lbl = tk.Label(self.props, text='Ничего не выбрано',
                                      anchor='nw', justify='left')
            self.props_lbl.pack(fill='both', expand=True)
            return
        kind, idx = self.selection
        if kind == 'vertex':
            self._props_vertex(idx)
        elif kind == 'wall':
            self._props_wall(idx)
        elif kind == 'sector':
            self._props_sector(idx)
        elif kind == 'object':
            self._props_object(idx)
        elif kind == 'surface':
            self._props_surface(idx)

    def _row(self, parent, label, init, cb, lo=None, hi=None):
        f = tk.Frame(parent)
        f.pack(fill='x', pady=1)
        tk.Label(f, text=label, width=12, anchor='w').pack(side='left')
        var = tk.StringVar(value=str(init))
        if lo is not None:
            w = tk.Spinbox(f, from_=lo, to=hi, width=7, textvariable=var,
                           command=lambda: cb(var.get()))
        else:
            w = tk.Entry(f, width=8, textvariable=var)
        w.pack(side='left')
        w.bind('<Return>', lambda e: cb(var.get()))
        w.bind('<FocusOut>', lambda e: cb(var.get()))
        return var

    def _props_vertex(self, i):
        lv = self.level
        tk.Label(self.props, text='Вершина %d' % i,
                 font=('TkDefaultFont', 10, 'bold')).pack(anchor='w')
        x, z = lv.vertices[i]

        def set_x(v):
            try:
                self.snapshot()
                lv.vertices[i] = (max(-32768, min(32767, int(float(v)))),
                                  lv.vertices[i][1])
                self._geom_touched(); self.redraw()
            except ValueError:
                pass

        def set_z(v):
            try:
                self.snapshot()
                lv.vertices[i] = (lv.vertices[i][0],
                                  max(-32768, min(32767, int(float(v)))))
                self._geom_touched(); self.redraw()
            except ValueError:
                pass
        self._row(self.props, 'X', x, set_x)
        self._row(self.props, 'Z', z, set_z)

    def _props_wall(self, i):
        lv = self.level
        w = lv.walls[i]
        tk.Label(self.props, text='Стена %d' % i,
                 font=('TkDefaultFont', 10, 'bold')).pack(anchor='w')
        tk.Label(self.props, text='v: %d → %d' % (w['sv'], w['ev']),
                 anchor='w').pack(fill='x')

        frm = tk.LabelFrame(self.props, text='флаги')
        frm.pack(fill='x')
        for bit, nm in ((1, 'solid'), (2, 'passable'), (4, 'transparent'),
                        (8, 'door'), (16, 'secret')):
            v = tk.IntVar(value=1 if (w['flags'] & bit) else 0)

            def mk(bit=bit, var=v):
                def f():
                    self.snapshot()
                    if var.get():
                        w['flags'] |= bit
                    else:
                        w['flags'] &= ~bit & 0xFF
                    self.content_dirty = True
                return f
            tk.Checkbutton(frm, text=nm, variable=v,
                           command=mk()).pack(anchor='w')

        def set_type(v):
            try:
                self.snapshot()
                w['type'] = int(v.split(' ')[0]) & 0xFF
                self.content_dirty = True
                self.redraw()
            except ValueError:
                pass
        cur = '%d %s' % (w['type'], WALL_TYPES.get(w['type'], ''))
        cb = ttk.Combobox(self.props, values=['%d %s' % kv for kv in
                                              sorted(WALL_TYPES.items())],
                          state='readonly', width=24)
        cb.set(cur)
        cb.pack(fill='x', pady=2)
        cb.bind('<<ComboboxSelected>>', lambda e: set_type(cb.get()))

        def set_special(v):
            try:
                self.snapshot()
                w['special'] = int(v) & 0xFF
                self.content_dirty = True
            except ValueError:
                pass
        self._row(self.props, 'special', w['special'], set_special, 0, 255)

        # surfaces
        sf = tk.LabelFrame(self.props, text='стороны (surfaces)')
        sf.pack(fill='x', pady=4)
        for side, si in (('front', w['front']), ('back', w['back'])):
            btn = tk.Button(sf, text='%s: %s'
                            % (side, ('#%d' % si) if si >= 0 else 'нет'),
                            command=lambda s=side: self._edit_wall_side(i, s))
            btn.pack(fill='x')
        tk.Button(self.props, text='Разрезать стену пополам',
                  command=lambda: self._split_wall(i)).pack(anchor='w',
                                                            pady=3)

    def _split_wall(self, i):
        lv = self.level
        w = lv.walls[i]
        x1, z1 = lv.vertices[w['sv']]
        x2, z2 = lv.vertices[w['ev']]
        mx = int(round((x1 + x2) / 2.0))
        mz = int(round((z1 + z2) / 2.0))
        if (mx, mz) == (x1, z1) or (mx, mz) == (x2, z2):
            self.set_status('стена слишком короткая для разрезания')
            return
        self.snapshot()
        lv.vertices.append((mx, mz))
        nv = len(lv.vertices) - 1
        w2 = dict(w)
        w2['sv'] = nv
        w['ev'] = nv
        lv.walls.insert(i + 1, w2)
        self.geometry_dirty = True
        self.content_dirty = True
        self.show_props()
        self.redraw()
        self.set_status('стена %d разрезана новой вершиной %d' % (i, nv))

    def _edit_wall_side(self, wi, side):
        lv = self.level
        w = lv.walls[wi]
        si = w[side]
        if si < 0:
            if messagebox.askyesno(APP_TITLE,
                                   'Создать %s surface?' % side):
                self.snapshot()
                lv.surfaces.append(dict(ox=0, oy=0, upper=27, lower=27,
                                        main=27, sector=self.active_sector))
                w[side] = len(lv.surfaces) - 1
                self.geometry_dirty = True
                self.show_props()
                self.redraw()
            return
        self.selection = ('surface', si)
        self.show_props()

    def _props_surface(self, si):
        lv = self.level
        s = lv.surfaces[si]
        tk.Label(self.props, text='Surface %d' % si,
                 font=('TkDefaultFont', 10, 'bold')).pack(anchor='w')
        for part, nm in (('main', 'осн.'), ('upper', 'верх.'),
                         ('lower', 'низ.')):
            f = tk.Frame(self.props)
            f.pack(fill='x', pady=1)
            tk.Label(f, text=nm, width=6, anchor='w').pack(side='left')
            tex = None
            if self.assets is not None:
                tex = self.assets.resolve_wall_tex(s[part])
            txt = 'id %d' % s[part]
            if tex is not None:
                txt += ' (%dx%d)' % (tex.w, tex.h)
            elif s[part]:
                txt += ' (нет в атласах!)'
            tk.Button(f, text=txt,
                      command=lambda p=part: self._pick_surface_tex(si, p)
                      ).pack(side='left')

        def set_off(key):
            def cb(v):
                try:
                    self.snapshot()
                    s[key] = max(-32768, min(32767, int(float(v))))
                    self.content_dirty = True
                except ValueError:
                    pass
            return cb
        self._row(self.props, 'offset X', s['ox'], set_off('ox'))
        self._row(self.props, 'offset Y', s['oy'], set_off('oy'))

        def set_sector(v):
            try:
                val = int(v)
            except ValueError:
                return
            if 0 <= val < len(lv.sectors):
                self.snapshot()
                s['sector'] = val
                self.geometry_dirty = True
                self.content_dirty = True
        self._row(self.props, 'сектор', s['sector'], set_sector, 0,
                  max(0, len(lv.sectors) - 1))
        tk.Button(self.props, text='← назад к стене',
                  command=self._back_to_wall).pack(anchor='w', pady=4)

    def _back_to_wall(self):
        # вернуть выбор на стену, владеющую текущим surface
        if self.selection and self.selection[0] == 'surface':
            si = self.selection[1]
            for i, w in enumerate(self.level.walls):
                if w['front'] == si or w['back'] == si:
                    self.selection = ('wall', i)
                    break
        self.show_props()
        self.redraw()

    def _pick_surface_tex(self, si, part):
        if self.assets is None:
            messagebox.showwarning(APP_TITLE, 'Сначала загрузите атласы')
            return
        p = TexturePicker(self.props, self.assets, mode='wall',
                          title='Текстура для %s' % part)
        if p.result is not None:
            self.snapshot()
            self.level.surfaces[si][part] = p.result
            self.content_dirty = True
            self.show_props()
            self.redraw()

    def _props_sector(self, s):
        lv = self.level
        sd = lv.sectors[s]
        tk.Label(self.props, text='Сектор %d' % s,
                 font=('TkDefaultFont', 10, 'bold')).pack(anchor='w')
        self.active_sector = s

        def mk_set(key, lo, hi):
            def cb(v):
                try:
                    self.snapshot()
                    sd[key] = max(lo, min(hi, int(float(v))))
                    self.geometry_dirty = True
                    self.content_dirty = True
                    self._fill_key = None
                    # self.redraw() лёгкий не нужен — заливка изменилась
                    self.redraw()
                except ValueError:
                    pass
            return cb
        self._row(self.props, 'пол (выс.)', sd['floor'], mk_set('floor',
                                                              -8192, 8192))
        self._row(self.props, 'потолок', sd['ceil'], mk_set('ceil',
                                                            -8192, 8192))
        lv_light = (sd['light_packed'] >> 4) & 15
        lf = tk.Frame(self.props)
        lf.pack(fill='x')
        tk.Label(lf, text='свет', width=12, anchor='w').pack(side='left')
        light_var = tk.IntVar(value=lv_light)

        def light_cb(v):
            val = int(float(v))
            self.snapshot()
            sd['light_packed'] = ((val & 15) << 4) | (sd['light_packed'] & 0xF)
            self.content_dirty = True
            self._fill_key = None
            self.redraw()
        tk.Scale(lf, from_=0, to=16, orient='horizontal', variable=light_var,
                 command=light_cb, length=110).pack(side='left')

        for key, nm in (('floor_tex', 'текстура пола'),
                        ('ceil_tex', 'текстура потолка')):
            f = tk.Frame(self.props)
            f.pack(fill='x', pady=1)
            tk.Label(f, text=nm, width=14, anchor='w').pack(side='left')
            tk.Button(f, text='id %d' % sd[key],
                      command=lambda k=key: self._pick_flat_tex(s, k)
                      ).pack(side='left')
        if sd['ceil_tex'] == 51 or sd['floor_tex'] == 51:
            tk.Label(self.props, text='(51 = небо)', anchor='w',
                     fg='#80a0ff').pack(fill='x')
        self._row(self.props, 'tag', sd['tag'], mk_set('tag', -32768, 32767))

        def set_type(v):
            try:
                self.snapshot()
                sd['type'] = int(v.split(' ')[0])
                self.content_dirty = True
            except ValueError:
                pass
        cb = ttk.Combobox(self.props, values=['%d %s' % kv for kv in
                                              sorted(SECTOR_TYPES.items())],
                          state='readonly', width=22)
        cb.set('%d %s' % (sd['type'], SECTOR_TYPES.get(sd['type'], '')))
        cb.pack(fill='x', pady=2)
        cb.bind('<<ComboboxSelected>>', lambda e: set_type(cb.get()))

    def _pick_flat_tex(self, s, key):
        if self.assets is None:
            messagebox.showwarning(APP_TITLE, 'Сначала загрузите атласы')
            return
        p = TexturePicker(self.props, self.assets, mode='flat',
                          title='Текстура %s (id 51 = небо)' % key)
        if p.result is not None:
            self.snapshot()
            self.level.sectors[s][key] = p.result
            self.content_dirty = True
            self._fill_key = None
            self.show_props()
            self.redraw()

    def _props_object(self, i):
        lv = self.level
        ob = lv.objects[i]
        tk.Label(self.props, text='Объект %d' % i,
                 font=('TkDefaultFont', 10, 'bold')).pack(anchor='w')
        names = ['%d %s' % (t, C.OBJ_NAMES.get(t, ''))
                 for t in sorted(C.OBJ_NAMES)]
        cur = '%d %s' % (ob['type'], C.OBJ_NAMES.get(ob['type'], ''))
        cb = ttk.Combobox(self.props, values=names, width=26)
        cb.set(cur)
        cb.pack(fill='x', pady=2)

        def set_type(v):
            try:
                self.snapshot()
                ob['type'] = int(v.split(' ')[0])
                self.content_dirty = True
                self.redraw()
            except (ValueError, IndexError):
                pass
        cb.bind('<<ComboboxSelected>>', lambda e: set_type(cb.get()))
        cb.bind('<Return>', lambda e: set_type(cb.get()))

        def mk(key, lo, hi):
            def f(v):
                try:
                    self.snapshot()
                    ob[key] = max(lo, min(hi, int(float(v))))
                    self.content_dirty = True
                    self.redraw()
                except ValueError:
                    pass
            return f
        self._row(self.props, 'X', ob['x'], mk('x', -32768, 32767))
        self._row(self.props, 'Z', ob['z'], mk('z', -32768, 32767))
        self._row(self.props, 'угол (raw)', ob['angle'],
                  mk('angle', -32768, 32767))
        self._row(self.props, 'param', ob['param'], mk('param',
                                                       -32768, 32767))

    # ---------------- 3D ----------------

    def open_3d(self):
        if self.level is None:
            return
        if self.assets is None:
            self.load_assets(self.resdir, quiet=True)
        if self.view3d is not None and not self.view3d._closed:
            self.view3d.top.lift()
            return
        self.view3d = View3D(self)
        self.view3d.render()


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def main(argv):
    resdir = None
    level_path = None
    i = 1
    while i < len(argv):
        if argv[i] == '--resdir' and i + 1 < len(argv):
            resdir = argv[i + 1]
            i += 2
        elif not argv[i].startswith('-'):
            level_path = argv[i]
            i += 1
        else:
            i += 1
    if resdir is None:
        cand = os.path.join(os.getcwd(), 'res', 'gamedata')
        resdir = cand if os.path.isdir(cand) else None
    if not TK_OK:
        print('tkinter недоступен в этой среде. Можно headless-тест ядра:\n'
              '  python3 scripts/co3d_level_core.py selftest res/gamedata')
        return 1
    root = tk.Tk()
    root.geometry('1100x680')
    ed = Editor(root, resdir)
    if level_path:
        ed.open_level(level_path)
    root.mainloop()
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
