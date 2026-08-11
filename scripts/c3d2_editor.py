#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""C3D2 package editor: geometry, external materials, entities and flythrough.

This is intentionally a new editor for the clean C3D2/C3B pipeline.  The old
``level_editor.py`` remains a legacy level_XX/TX/SP importer and is not used by
this tool.

Run from the repository root::

    python3 scripts/c3d2_editor.py res/gamedata/custom/doom-e1m1

Optional editor-only dependencies::

    python3 -m pip install pygame pillow

Pygame provides the GUI and realtime 3D flythrough. Pillow is used only when
importing source images; it allows the importer to accept the image formats it
supports (PNG, JPEG, WebP, TIFF, GIF, BMP and many more).  C3D2 runtime code,
the compiler and Java ME build do not depend on either package.
"""

import argparse
import copy
import math
import os
import sys
import traceback

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

import c3d2_core as C3
import c3d2_entities as ENTITIES
import c3d2_texture_tools as TEXTURES
import co3d_level_core as LEGACY

try:
    import pygame
except ImportError:
    pygame = None


APP_TITLE = 'C3D2 Editor — CovertOps3D'
DEFAULT_PACKAGE = os.path.join(ROOT, 'res', 'gamedata', 'custom', 'doom-e1m1')
MAP_BACKGROUND = (19, 24, 30)
GRID_COLOR = (38, 48, 60)
TEXT_COLOR = (225, 232, 240)
MUTED_TEXT = (155, 172, 188)
ACCENT = (78, 177, 255)
WARNING = (255, 185, 70)
ERROR = (255, 95, 105)


class EditorError(RuntimeError):
    pass


class PackageModel(object):
    """Editable C3D2 package state, independent of Pygame widgets."""

    def __init__(self, package_dir, create=False):
        self.package_dir = os.path.abspath(package_dir)
        self.level_path = os.path.join(self.package_dir, 'level.c3d.json')
        self.c3b_path = os.path.join(self.package_dir, 'level.c3b')
        self.document = None
        self.level = None
        self.materials = {}
        self.dirty = False

        if os.path.exists(self.level_path):
            self.reload()
        elif create:
            self._create_empty_package()
        else:
            raise EditorError('level.c3d.json not found in ' + self.package_dir)

    @property
    def manifest_path(self):
        material_path = self.document.materials if self.document else 'materials.c3m'
        return os.path.join(self.package_dir, *material_path.replace('\\', '/').split('/'))

    @property
    def entity_path(self):
        self.ensure_external_entities()
        return os.path.join(self.package_dir,
                            *self.document.entities.replace('\\', '/').split('/'))

    def _create_empty_package(self):
        if not os.path.isdir(self.package_dir):
            os.makedirs(self.package_dir)
        texture_dir = os.path.join(self.package_dir, 'textures')
        if not os.path.isdir(texture_dir):
            os.makedirs(texture_dir)
        self.document = C3.new_document()
        self.level = self.document.level
        # A new map must be launchable before the first artist import.
        TEXTURES.create_starter_materials(self.package_dir)
        self.save_source()
        self.refresh_materials()

    def reload(self):
        self.document = C3.load_source(self.level_path)
        self.level = self.document.level
        self.ensure_external_entities(mark_dirty=False)
        self.refresh_materials()
        self.dirty = False

    def ensure_external_entities(self, mark_dirty=True):
        """Migrates an early inline-object source in memory to entities.ini."""
        if self.document.entities is None:
            self.document.entities = 'entities.ini'
            if mark_dirty:
                self.dirty = True

    def refresh_materials(self):
        self.materials = TEXTURES.load_manifest(self.manifest_path)

    def save_source(self):
        self.ensure_external_entities(mark_dirty=False)
        if not os.path.isdir(self.package_dir):
            os.makedirs(self.package_dir)
        C3.dump_source(self.document, self.level_path)
        ENTITIES.dump_entities(self.level.objects, self.entity_path)
        self.dirty = False

    def compile(self):
        self.save_source()
        output, report = C3.compile_source(self.level_path, self.c3b_path)
        return output, report

    def import_material(self, source_path, kind, slot=1, wall_width=64,
                        wall_height=128, fit=True):
        if not os.path.isdir(self.package_dir):
            os.makedirs(self.package_dir)
        result = TEXTURES.import_material(self.package_dir, source_path, kind, slot,
                                          wall_width, wall_height, fit)
        self.refresh_materials()
        return result

    def material_path(self, key):
        value = self.materials.get(key)
        if not value:
            return None
        return os.path.join(self.package_dir, *value.replace('\\', '/').split('/'))

    def mark_dirty(self):
        self.dirty = True


# ---------------------------------------------------------------------------
# Native file/folder dialogs are optional: Pygame drag-and-drop remains usable
# without Tk. Keeping them isolated avoids making Tk a runtime dependency.
# ---------------------------------------------------------------------------

def _tk_dialog(kind, title, initial=None):
    try:
        import tkinter as tk
        from tkinter import filedialog
        root = tk.Tk()
        root.withdraw()
        root.attributes('-topmost', True)
        try:
            if kind == 'directory':
                result = filedialog.askdirectory(title=title, initialdir=initial or ROOT)
            else:
                result = filedialog.askopenfilename(
                    title=title,
                    initialdir=initial or ROOT,
                    filetypes=[('Images', '*.png *.jpg *.jpeg *.webp *.tif *.tiff *.gif *.bmp *.ico'),
                               ('All files', '*.*')])
        finally:
            root.destroy()
        return result or None
    except Exception:
        return None


def _tk_integer(title, prompt, initial):
    try:
        import tkinter as tk
        from tkinter import simpledialog
        root = tk.Tk()
        root.withdraw()
        root.attributes('-topmost', True)
        try:
            value = simpledialog.askinteger(title, prompt, initialvalue=int(initial), parent=root)
        finally:
            root.destroy()
        return value
    except Exception:
        return None


def _clamp(value, lower, upper):
    return lower if value < lower else upper if value > upper else value


def _distance_to_segment(px, pz, x1, z1, x2, z2):
    dx = x2 - x1
    dz = z2 - z1
    length2 = dx * dx + dz * dz
    if length2 <= 0:
        return math.hypot(px - x1, pz - z1)
    ratio = ((px - x1) * dx + (pz - z1) * dz) / float(length2)
    ratio = _clamp(ratio, 0.0, 1.0)
    return math.hypot(px - (x1 + ratio * dx), pz - (z1 + ratio * dz))


# ---------------------------------------------------------------------------
# Pygame material cache. Textures are requested only by the GUI/flythrough;
# no image object enters compiler or Java ME code.
# ---------------------------------------------------------------------------

class MaterialCache(object):
    def __init__(self, model):
        self.model = model
        self.surfaces = {}
        self.colors = {}

    def clear(self):
        self.surfaces = {}
        self.colors = {}

    def refresh(self):
        self.model.refresh_materials()
        self.clear()

    def get_surface(self, key):
        if key in self.surfaces:
            return self.surfaces[key]
        path = self.model.material_path(key)
        surface = None
        if path and os.path.exists(path):
            try:
                surface = pygame.image.load(path).convert()
            except Exception:
                surface = None
        self.surfaces[key] = surface
        return surface

    def get_color(self, key, fallback=(96, 104, 116)):
        if key in self.colors:
            return self.colors[key]
        surface = self.get_surface(key)
        if surface is None:
            self.colors[key] = fallback
            return fallback
        width, height = surface.get_size()
        sample_x = min(8, width)
        sample_y = min(8, height)
        red = green = blue = count = 0
        for y in range(sample_y):
            py = (y * height) // sample_y
            for x in range(sample_x):
                px = (x * width) // sample_x
                color = surface.get_at((px, py))
                red += color.r
                green += color.g
                blue += color.b
                count += 1
        result = (red // max(1, count), green // max(1, count), blue // max(1, count))
        self.colors[key] = result
        return result


# ---------------------------------------------------------------------------
# Realtime 2.5D flythrough. This is an editor preview, not a replacement for
# the Java renderer: it uses portal-aware ray traversal with Pygame textured
# wall strips so creators can walk a C3D2 level before exporting it.
# ---------------------------------------------------------------------------

class Flythrough(object):
    def __init__(self, app):
        self.app = app
        self.model = app.model
        self.cache = app.material_cache
        self.x = 0.0
        self.z = 0.0
        self.yaw = -math.pi * 0.5
        self.sides = []
        self.reset_camera()

    def reset_camera(self):
        level = self.model.level
        spawn = None
        for entity in level.objects:
            if 1 <= int(entity['type']) <= 4:
                spawn = entity
                break
        if spawn is not None:
            self.x = float(spawn['x'])
            self.z = float(spawn['z'])
            # Legacy angle 0 becomes a 90-degree engine rotation, whose
            # forward vector is -Z. Keep the editor camera consistent.
            self.yaw = -math.pi * 0.5 - math.radians(float(spawn.get('angle', 0)))
        else:
            self.x = 0.0
            self.z = 0.0
            self.yaw = -math.pi * 0.5
        self.sides = level.sector_sides()

    def update(self, delta):
        keys = pygame.key.get_pressed()
        turn_speed = 2.2
        move_speed = 150.0 * (2.0 if keys[pygame.K_LSHIFT] or keys[pygame.K_RSHIFT] else 1.0)
        if keys[pygame.K_LEFT] or keys[pygame.K_q]:
            self.yaw -= turn_speed * delta
        if keys[pygame.K_RIGHT] or keys[pygame.K_e]:
            self.yaw += turn_speed * delta

        forward = 0.0
        strafe = 0.0
        if keys[pygame.K_w] or keys[pygame.K_UP]:
            forward += move_speed * delta
        if keys[pygame.K_s] or keys[pygame.K_DOWN]:
            forward -= move_speed * delta
        if keys[pygame.K_a]:
            strafe -= move_speed * delta
        if keys[pygame.K_d]:
            strafe += move_speed * delta
        if forward or strafe:
            nx = self.x + math.cos(self.yaw) * forward - math.sin(self.yaw) * strafe
            nz = self.z + math.sin(self.yaw) * forward + math.cos(self.yaw) * strafe
            if LEGACY.find_sector_at(self.model.level, nx, nz) is not None:
                self.x = nx
                self.z = nz

    def current_sector(self):
        return LEGACY.find_sector_at(self.model.level, self.x, self.z)

    def render(self, screen):
        level = self.model.level
        width, height = screen.get_size()
        current_sector = self.current_sector()
        if current_sector is None:
            screen.fill((8, 8, 12))
            self.app.draw_text(screen, 'Камера вне сектора — R: к спавну', 18,
                               (18, 18), WARNING)
            return

        sector = level.sectors[current_sector]
        top_color = self._flat_color(sector['ceil_tex'], True)
        bottom_color = self._flat_color(sector['floor_tex'], False)
        horizon = height // 2
        screen.fill(top_color)
        pygame.draw.rect(screen, bottom_color, (0, horizon, width, height - horizon))

        ray_count = _clamp(width // 6, 100, 180)
        strip_width = max(1, (width + ray_count - 1) // ray_count)
        fov = math.radians(82.0)
        projection = (width * 0.5) / math.tan(fov * 0.5)
        eye = sector['floor'] + 40.0

        for ray in range(ray_count):
            relative = ((ray + 0.5) / float(ray_count) - 0.5) * fov
            angle = self.yaw + relative
            direction_x = math.cos(angle)
            direction_z = math.sin(angle)
            hit = self._trace(direction_x, direction_z, current_sector)
            if hit is None:
                continue
            distance = hit['distance'] * math.cos(relative)
            if distance < 0.01:
                continue
            hit_sector = level.sectors[hit['sector']]
            top = int(horizon - (hit_sector['ceil'] - eye) * projection / distance)
            bottom = int(horizon + (eye - hit_sector['floor']) * projection / distance)
            top = _clamp(top, -height, height * 2)
            bottom = _clamp(bottom, -height, height * 2)
            if bottom <= top:
                continue
            x = ray * strip_width
            self._draw_wall_strip(screen, hit, x, strip_width, top, bottom)

        overlay = pygame.Surface((width, 34), pygame.SRCALPHA)
        overlay.fill((0, 0, 0, 170))
        screen.blit(overlay, (0, 0))
        self.app.draw_text(screen,
                           '3D пролёт  WASD: движение  Q/E или ←/→: поворот  Shift: быстрее  R: спавн  F3/Esc: редактор',
                           16, (10, 9), TEXT_COLOR)
        self.app.draw_text(screen, 'x=%d z=%d sector=%d' %
                           (int(self.x), int(self.z), current_sector), 16,
                           (10, 22), MUTED_TEXT)
        pygame.draw.line(screen, (255, 255, 255), (width // 2 - 5, horizon),
                         (width // 2 + 5, horizon), 1)
        pygame.draw.line(screen, (255, 255, 255), (width // 2, horizon - 5),
                         (width // 2, horizon + 5), 1)

    def _flat_color(self, texture_id, upper):
        if texture_id == 51:
            return self.cache.get_color('sky', (60, 95, 145))
        return self.cache.get_color('flat.%d' % int(texture_id),
                                    (78, 88, 98) if upper else (56, 65, 57))

    def _draw_wall_strip(self, screen, hit, x, strip_width, top, bottom):
        surface_data = hit['side']['surf']
        texture_id = int(surface_data.get('main', 0))
        texture = self.cache.get_surface('wall.%d' % texture_id)
        visible_width = min(strip_width, screen.get_width() - x)
        visible_height = min(screen.get_height(), bottom) - max(0, top)
        if visible_width <= 0 or visible_height <= 0:
            return
        if texture is None:
            pygame.draw.rect(screen, (190, 0, 190),
                             (x, max(0, top), visible_width, visible_height))
            return

        texture_width, texture_height = texture.get_size()
        segment = hit['side']
        segment_length = math.hypot(segment['x2'] - segment['x1'],
                                    segment['z2'] - segment['z1'])
        source_x = int((hit['u'] * segment_length) % texture_width)
        try:
            column = texture.subsurface((source_x, 0, 1, texture_height))
            scaled = pygame.transform.scale(column, (visible_width, max(1, bottom - top)))
            screen.blit(scaled, (x, top))
        except Exception:
            color = self.cache.get_color('wall.%d' % texture_id, (160, 70, 110))
            pygame.draw.rect(screen, color, (x, max(0, top), visible_width, visible_height))

    def _trace(self, direction_x, direction_z, start_sector):
        level = self.model.level
        sector_index = start_sector
        px = self.x
        pz = self.z
        total_distance = 0.0
        # Portal traversal is bounded; malformed geometry cannot make a frame
        # loop forever. The current level compiler remains the authoritative
        # BSP validator for export.
        for _step in range(24):
            closest = None
            for side in self.sides[sector_index]:
                result = _ray_segment(px, pz, direction_x, direction_z,
                                      side['x1'], side['z1'], side['x2'], side['z2'])
                if result is None:
                    continue
                distance, fraction = result
                if closest is None or distance < closest['distance']:
                    closest = dict(distance=distance, u=fraction, side=side,
                                   sector=sector_index)
            if closest is None:
                return None

            total_distance += closest['distance']
            other_sector = closest['side']['other_sector']
            if other_sector is None:
                closest['distance'] = total_distance
                return closest

            current = level.sectors[sector_index]
            other = level.sectors[other_sector]
            opening_floor = max(current['floor'], other['floor'])
            opening_ceiling = min(current['ceil'], other['ceil'])
            if opening_ceiling - opening_floor < 16:
                closest['distance'] = total_distance
                return closest

            epsilon = 0.02
            # px/pz are local to the current traversal step; only the value
            # returned to the renderer remains relative to the camera.
            px += direction_x * (closest['distance'] + epsilon)
            pz += direction_z * (closest['distance'] + epsilon)
            sector_index = other_sector
        return None


def _ray_segment(px, pz, dx, dz, x1, z1, x2, z2):
    sx = x2 - x1
    sz = z2 - z1
    denominator = dx * sz - dz * sx
    if -0.0000001 < denominator < 0.0000001:
        return None
    qx = x1 - px
    qz = z1 - pz
    distance = (qx * sz - qz * sx) / denominator
    fraction = (qx * dz - qz * dx) / denominator
    if distance <= 0.001 or fraction < 0.0 or fraction > 1.0:
        return None
    return distance, fraction


# ---------------------------------------------------------------------------
# Main Pygame editor window.
# ---------------------------------------------------------------------------

class EditorApp(object):
    PANEL_WIDTH = 316

    def __init__(self, model):
        self.model = model
        self.screen = pygame.display.set_mode((1360, 820), pygame.RESIZABLE)
        pygame.display.set_caption(APP_TITLE)
        self.clock = pygame.time.Clock()
        self.font = pygame.font.Font(None, 18)
        self.small_font = pygame.font.Font(None, 15)
        self.large_font = pygame.font.Font(None, 23)
        self.running = True
        self.mode = 'edit'
        self.tool = 'select'
        self.selected = None
        self.current_sector_id = 0
        self.wall_anchor = None
        self.drag = None
        self.pan = None
        self.undo_stack = []
        self.redo_stack = []
        self.grid = 8
        self.snap = True
        self.zoom = 1.6
        self.offset_x = 0.0
        self.offset_y = 0.0
        self.current_kind = TEXTURES.MATERIAL_WALL
        self.current_slot = 1
        self.wall_width = 64
        self.wall_height = 128
        self.fit_import = True
        self.wall_part = 'main'
        self.flat_target = 'floor'
        self.entity_type = 1
        self.entity_angle = 0
        self.entity_param = 0
        self.button_actions = []
        self.log_lines = []
        self.material_cache = MaterialCache(model)
        self.flythrough = Flythrough(self)
        self.center_view()
        self.log('Пакет открыт: ' + self.model.package_dir)

    # -------------------------- package actions --------------------------

    def load_package(self, directory):
        try:
            self.model = PackageModel(directory)
            self.material_cache = MaterialCache(self.model)
            self.flythrough = Flythrough(self)
            self.selected = None
            self.current_sector_id = 0
            self.wall_anchor = None
            self.undo_stack = []
            self.redo_stack = []
            self.center_view()
            self.log('Загружен пакет: ' + self.model.package_dir)
        except Exception as error:
            self.log('Ошибка открытия: ' + str(error), ERROR)

    def open_dialog(self):
        directory = _tk_dialog('directory', 'Открыть C3D2 package', self.model.package_dir)
        if directory:
            self.load_package(directory)
        else:
            self.log('Диалог папки недоступен: перетащите package в окно', WARNING)

    def save(self):
        try:
            self.model.save_source()
            self.log('Сохранены level.c3d.json и entities.ini')
        except Exception as error:
            self.log('Ошибка сохранения: ' + str(error), ERROR)

    def compile(self):
        try:
            output, report = self.model.compile()
            self.log('C3B: %s | nodes=%d leaves=%d segments=%d splits=%d' %
                     (os.path.basename(output), len(self.model.level.nodes),
                      len(self.model.level.leaves), len(self.model.level.segments),
                      report.splits))
        except Exception as error:
            self.log('Ошибка C3B/BSP: ' + str(error), ERROR)

    def import_current_material(self, source_path=None):
        if source_path is None:
            source_path = _tk_dialog('file', 'Импорт текстуры', self.model.package_dir)
        if not source_path:
            self.log('Импорт отменён. Можно перетащить изображение в окно.', WARNING)
            return
        try:
            slot = self.current_slot if self.current_kind != TEXTURES.MATERIAL_SKY else 1
            result = self.model.import_material(source_path, self.current_kind, slot,
                                                self.wall_width, self.wall_height,
                                                self.fit_import)
            self.material_cache.refresh()
            self.current_kind = result['kind']
            if self.current_kind != TEXTURES.MATERIAL_SKY:
                self.current_slot = slot
            self.log('%s → %s (%dx%d, K-means 16)' %
                     (os.path.basename(source_path), result['relative_path'],
                      result['width'], result['height']))
        except Exception as error:
            self.log('Ошибка импорта: ' + str(error), ERROR)

    # -------------------------- undo / model edits ------------------------

    def snapshot(self):
        self.undo_stack.append(copy.deepcopy(self.model.level))
        if len(self.undo_stack) > 64:
            del self.undo_stack[0]
        self.redo_stack = []

    def _replace_level(self, level):
        self.model.level = level
        self.model.document.level = level
        self.model.mark_dirty()
        self.flythrough.sides = level.sector_sides()

    def undo(self):
        if not self.undo_stack:
            self.log('Нет действий для отмены', MUTED_TEXT)
            return
        self.redo_stack.append(copy.deepcopy(self.model.level))
        self._replace_level(self.undo_stack.pop())
        self.selected = None
        self.log('Отмена')

    def redo(self):
        if not self.redo_stack:
            self.log('Нет действий для повтора', MUTED_TEXT)
            return
        self.undo_stack.append(copy.deepcopy(self.model.level))
        self._replace_level(self.redo_stack.pop())
        self.selected = None
        self.log('Повтор')

    def mark_geometry_changed(self):
        self.model.mark_dirty()
        self.flythrough.sides = self.model.level.sector_sides()

    def add_entity(self, x, z):
        self.snapshot()
        entity = dict(x=int(round(x)), z=int(round(z)), angle=int(self.entity_angle),
                      type=int(self.entity_type), param=int(self.entity_param))
        self.model.level.objects.append(entity)
        self.selected = ('entity', len(self.model.level.objects) - 1)
        self.model.mark_dirty()
        self.log('Entity type=%d поставлен в x=%d z=%d' %
                 (entity['type'], entity['x'], entity['z']))

    def add_sector(self):
        self.snapshot()
        level = self.model.level
        source = level.sectors[_clamp(self.current_sector_id, 0, len(level.sectors) - 1)]
        sector = dict(floor=source['floor'], ceil=source['ceil'],
                      floor_tex=source['floor_tex'], ceil_tex=source['ceil_tex'],
                      light_packed=source['light_packed'], tag=0, type=0)
        level.sectors.append(sector)
        self.current_sector_id = len(level.sectors) - 1
        self.selected = ('sector', self.current_sector_id)
        self.mark_geometry_changed()
        self.log('Добавлен sector.%d — создайте для него замкнутые clockwise стены.'
                 % self.current_sector_id, WARNING)

    def add_portal_back(self):
        if not self.selected or self.selected[0] != 'wall':
            self.log('Выберите стену, затем нужный сектор, чтобы добавить portal side.', WARNING)
            return
        level = self.model.level
        wall = level.walls[self.selected[1]]
        target = _clamp(self.current_sector_id, 0, len(level.sectors) - 1)
        self.snapshot()
        if wall['back'] < 0:
            main = self.current_slot if self.current_kind == TEXTURES.MATERIAL_WALL else 1
            level.surfaces.append(dict(ox=0, oy=0, upper=main, lower=main,
                                       main=main, sector=target))
            wall['back'] = len(level.surfaces) - 1
            self.log('Добавлена back side стены в sector.%d' % target)
        else:
            level.surfaces[wall['back']]['sector'] = target
            self.log('Back side стены переназначена на sector.%d' % target)
        self.mark_geometry_changed()

    def add_wall_point(self, x, z):
        level = self.model.level
        vertex = self.nearest_vertex(x, z)
        if vertex is None:
            self.snapshot()
            vertex = len(level.vertices)
            level.vertices.append((int(round(x)), int(round(z))))
            self.mark_geometry_changed()
        if self.wall_anchor is None:
            self.wall_anchor = vertex
            self.log('Начало стены: vertex %d; выберите вторую точку' % vertex)
            return
        if self.wall_anchor == vertex:
            self.wall_anchor = None
            self.log('Создание стены отменено', MUTED_TEXT)
            return
        self.snapshot()
        sector = _clamp(int(self.active_sector()), 0, max(0, len(level.sectors) - 1))
        main = self.current_slot if self.current_kind == TEXTURES.MATERIAL_WALL else 1
        surface = dict(ox=0, oy=0, upper=main, lower=main, main=main, sector=sector)
        level.surfaces.append(surface)
        level.walls.append(dict(sv=self.wall_anchor, ev=vertex, flags=1, type=0,
                                special=0, front=len(level.surfaces) - 1, back=-1))
        self.selected = ('wall', len(level.walls) - 1)
        self.wall_anchor = None
        self.mark_geometry_changed()
        self.log('Стена добавлена. Проверьте направление стрелки; R разворачивает её.', WARNING)

    def reverse_selected_wall(self):
        if not self.selected or self.selected[0] != 'wall':
            self.log('Выберите стену для разворота', WARNING)
            return
        wall = self.model.level.walls[self.selected[1]]
        self.snapshot()
        wall['sv'], wall['ev'] = wall['ev'], wall['sv']
        if wall['back'] >= 0:
            wall['front'], wall['back'] = wall['back'], wall['front']
        self.mark_geometry_changed()
        self.log('Направление стены изменено')

    def delete_selected(self):
        if not self.selected:
            self.log('Нечего удалять', WARNING)
            return
        kind, index = self.selected
        level = self.model.level
        if kind == 'entity' and 0 <= index < len(level.objects):
            self.snapshot()
            del level.objects[index]
            self.model.mark_dirty()
            self.selected = None
            self.log('Entity удалён')
        elif kind == 'wall' and 0 <= index < len(level.walls):
            self.snapshot()
            del level.walls[index]
            self.mark_geometry_changed()
            self.selected = None
            self.log('Стена удалена; неиспользуемая surface безопасно останется до чистки.')
        elif kind == 'vertex':
            self.log('Вершину удаляйте после связанных стен; это защищает индексы.', WARNING)
        else:
            self.log('Этот элемент нельзя удалить напрямую', WARNING)

    def edit_selected(self):
        if not self.selected:
            self.log('Сначала выберите объект, стену или сектор', WARNING)
            return
        kind, index = self.selected
        level = self.model.level
        if kind == 'entity' and 0 <= index < len(level.objects):
            entity = level.objects[index]
            entity_type = _tk_integer(APP_TITLE, 'Entity type', entity['type'])
            if entity_type is None:
                return
            angle = _tk_integer(APP_TITLE, 'Угол entity', entity.get('angle', 0))
            if angle is None:
                return
            parameter = _tk_integer(APP_TITLE, 'Param entity', entity.get('param', 0))
            if parameter is None:
                return
            sprite = _tk_integer(APP_TITLE, 'Sprite material slot (0 = none)', entity.get('sprite', 0))
            if sprite is None:
                return
            self.snapshot()
            entity['type'] = _clamp(entity_type, -32768, 32767)
            entity['angle'] = _clamp(angle, -32768, 32767)
            entity['param'] = _clamp(parameter, -32768, 32767)
            if sprite > 0:
                entity['sprite'] = _clamp(sprite, 1, 127)
            elif 'sprite' in entity:
                del entity['sprite']
            self.entity_type = entity['type']
            self.entity_angle = entity['angle']
            self.entity_param = entity['param']
            self.model.mark_dirty()
        elif kind == 'sector' and 0 <= index < len(level.sectors):
            sector = level.sectors[index]
            floor = _tk_integer(APP_TITLE, 'Высота пола', sector['floor'])
            if floor is None:
                return
            ceiling = _tk_integer(APP_TITLE, 'Высота потолка', sector['ceil'])
            if ceiling is None:
                return
            light = _tk_integer(APP_TITLE, 'Свет 0..15', (sector['light_packed'] >> 4) & 15)
            if light is None:
                return
            self.snapshot()
            sector['floor'] = _clamp(floor, -32768, 32767)
            sector['ceil'] = _clamp(ceiling, -32768, 32767)
            sector['light_packed'] = _clamp(light, 0, 15) << 4
            self.mark_geometry_changed()
        elif kind == 'wall' and 0 <= index < len(level.walls):
            wall = level.walls[index]
            main = _tk_integer(APP_TITLE, 'Main wall material slot',
                               level.surfaces[wall['front']]['main'])
            if main is None:
                return
            flags = _tk_integer(APP_TITLE, 'Wall flags', wall['flags'])
            if flags is None:
                return
            wall_type = _tk_integer(APP_TITLE, 'Wall type', wall['type'])
            if wall_type is None:
                return
            self.snapshot()
            surface = level.surfaces[wall['front']]
            surface['main'] = _clamp(main, 0, 127)
            wall['flags'] = _clamp(flags, 0, 255)
            wall['type'] = _clamp(wall_type, 0, 255)
            self.mark_geometry_changed()
        else:
            self.log('Диалог недоступен для этого выбора', WARNING)

    def apply_current_material(self):
        if not self.selected:
            self.log('Выберите стену или сектор для материала', WARNING)
            return
        kind, index = self.selected
        level = self.model.level
        self.snapshot()
        if kind == 'wall' and self.current_kind == TEXTURES.MATERIAL_WALL:
            wall = level.walls[index]
            surface = level.surfaces[wall['front']]
            surface[self.wall_part] = int(self.current_slot)
            self.mark_geometry_changed()
            self.log('wall.%d применён к %s' % (self.current_slot, self.wall_part))
        elif kind == 'sector' and self.current_kind in (TEXTURES.MATERIAL_FLAT,
                                                          TEXTURES.MATERIAL_SKY):
            sector = level.sectors[index]
            if self.flat_target == 'floor':
                sector['floor_tex'] = 51 if self.current_kind == TEXTURES.MATERIAL_SKY else int(self.current_slot)
            else:
                sector['ceil_tex'] = 51 if self.current_kind == TEXTURES.MATERIAL_SKY else int(self.current_slot)
            self.mark_geometry_changed()
            self.log('%s материал применён к сектору %d' % (self.flat_target, index))
        elif kind == 'entity' and self.current_kind == TEXTURES.MATERIAL_SPRITE:
            level.objects[index]['sprite'] = int(self.current_slot)
            self.model.mark_dirty()
            self.log('sprite.%d применён к entity.%d' % (self.current_slot, index))
        else:
            self.undo_stack.pop()
            self.log('Выбранный material несовместим с элементом', WARNING)

    # ----------------------------- hit testing ----------------------------

    def map_rect(self):
        width, height = self.screen.get_size()
        return pygame.Rect(self.PANEL_WIDTH, 0, max(1, width - self.PANEL_WIDTH), height)

    def world_to_screen(self, x, z):
        return int(self.offset_x + x * self.zoom), int(self.offset_y + z * self.zoom)

    def screen_to_world(self, x, y):
        return ((x - self.offset_x) / self.zoom, (y - self.offset_y) / self.zoom)

    def snap_position(self, x, z):
        if self.snap:
            return (round(x / self.grid) * self.grid, round(z / self.grid) * self.grid)
        return x, z

    def nearest_vertex(self, x, z, pixels=10):
        best = None
        distance = pixels / self.zoom
        for index, vertex in enumerate(self.model.level.vertices):
            current = math.hypot(x - vertex[0], z - vertex[1])
            if current < distance:
                distance = current
                best = index
        return best

    def nearest_wall(self, x, z, pixels=8):
        best = None
        distance = pixels / self.zoom
        for index, wall in enumerate(self.model.level.walls):
            first = self.model.level.vertices[wall['sv']]
            second = self.model.level.vertices[wall['ev']]
            current = _distance_to_segment(x, z, first[0], first[1], second[0], second[1])
            if current < distance:
                distance = current
                best = index
        return best

    def nearest_entity(self, x, z, pixels=12):
        best = None
        distance = pixels / self.zoom
        for index, entity in enumerate(self.model.level.objects):
            current = math.hypot(x - entity['x'], z - entity['z'])
            if current < distance:
                distance = current
                best = index
        return best

    def active_sector(self):
        return _clamp(self.current_sector_id, 0, max(0, len(self.model.level.sectors) - 1))

    # ------------------------------- input --------------------------------

    def handle_event(self, event):
        if self.mode == 'fly':
            if event.type == pygame.KEYDOWN:
                if event.key in (pygame.K_ESCAPE, pygame.K_F3):
                    self.mode = 'edit'
                    self.log('Возврат в 2D редактор')
                elif event.key == pygame.K_r:
                    self.flythrough.reset_camera()
            return

        if event.type == pygame.DROPFILE:
            path = event.file
            if os.path.isdir(path):
                self.load_package(path)
            elif os.path.basename(path) == 'level.c3d.json':
                self.load_package(os.path.dirname(path))
            else:
                self.import_current_material(path)
            return

        if event.type == pygame.KEYDOWN:
            self.handle_key(event)
        elif event.type == pygame.MOUSEWHEEL:
            self.zoom_at(event.pos if hasattr(event, 'pos') else pygame.mouse.get_pos(), event.y)
        elif event.type == pygame.MOUSEBUTTONDOWN:
            if event.button in (4, 5):
                self.zoom_at(event.pos, 1 if event.button == 4 else -1)
            elif event.button == 1:
                if event.pos[0] < self.PANEL_WIDTH:
                    self.handle_button(event.pos)
                else:
                    self.map_click(event.pos)
            elif event.button in (2, 3):
                self.pan = event.pos
        elif event.type == pygame.MOUSEMOTION:
            self.map_drag(event.pos)
            if self.pan is not None:
                dx = event.pos[0] - self.pan[0]
                dy = event.pos[1] - self.pan[1]
                self.offset_x += dx
                self.offset_y += dy
                self.pan = event.pos
        elif event.type == pygame.MOUSEBUTTONUP:
            if event.button == 1:
                self.drag = None
            elif event.button in (2, 3):
                self.pan = None

    def handle_key(self, event):
        key = event.key
        mods = pygame.key.get_mods()
        if key == pygame.K_F3:
            self.flythrough.reset_camera()
            self.mode = 'fly'
        elif key == pygame.K_F5:
            self.compile()
        elif key == pygame.K_s and mods & pygame.KMOD_CTRL:
            self.save()
        elif key == pygame.K_z and mods & pygame.KMOD_CTRL:
            self.undo()
        elif key == pygame.K_y and mods & pygame.KMOD_CTRL:
            self.redo()
        elif key == pygame.K_1:
            self.tool = 'select'
        elif key == pygame.K_2:
            self.tool = 'wall'
        elif key == pygame.K_3:
            self.tool = 'entity'
        elif key == pygame.K_4:
            self.tool = 'delete'
        elif key == pygame.K_n:
            self.add_sector()
        elif key == pygame.K_b:
            self.add_portal_back()
        elif key == pygame.K_i:
            self.import_current_material()
        elif key == pygame.K_m:
            self.apply_current_material()
        elif key == pygame.K_r:
            self.reverse_selected_wall()
        elif key == pygame.K_DELETE or key == pygame.K_BACKSPACE:
            self.delete_selected()
        elif key == pygame.K_RETURN:
            self.edit_selected()
        elif key == pygame.K_g:
            self.snap = not self.snap
            self.log('Привязка к сетке: ' + ('вкл.' if self.snap else 'выкл.'))
        elif key == pygame.K_p:
            self.wall_part = {'main': 'upper', 'upper': 'lower', 'lower': 'main'}[self.wall_part]
            self.log('Wall part: ' + self.wall_part)
        elif key == pygame.K_h:
            self.flat_target = 'ceiling' if self.flat_target == 'floor' else 'floor'
            self.log('Flat target: ' + self.flat_target)
        elif key == pygame.K_LEFTBRACKET:
            self.current_slot = max(1, self.current_slot - 1)
        elif key == pygame.K_RIGHTBRACKET:
            self.current_slot = min(127, self.current_slot + 1)
        elif key == pygame.K_COMMA:
            self.entity_angle = _clamp(self.entity_angle - 15, -32768, 32767)
        elif key == pygame.K_PERIOD:
            self.entity_angle = _clamp(self.entity_angle + 15, -32768, 32767)
        elif key == pygame.K_PAGEUP:
            self.entity_type = _clamp(self.entity_type + 1, -32768, 32767)
        elif key == pygame.K_PAGEDOWN:
            self.entity_type = _clamp(self.entity_type - 1, -32768, 32767)
        elif key == pygame.K_ESCAPE:
            self.wall_anchor = None
            self.drag = None
            self.selected = None

    def map_click(self, position):
        x, z = self.screen_to_world(position[0], position[1])
        x, z = self.snap_position(x, z)
        if self.tool == 'wall':
            self.add_wall_point(x, z)
            return
        if self.tool == 'entity':
            self.add_entity(x, z)
            return

        entity = self.nearest_entity(x, z)
        vertex = self.nearest_vertex(x, z)
        wall = self.nearest_wall(x, z)
        sector = LEGACY.find_sector_at(self.model.level, x, z)

        if self.tool == 'delete':
            if entity is not None:
                self.selected = ('entity', entity)
            elif wall is not None:
                self.selected = ('wall', wall)
            elif vertex is not None:
                self.selected = ('vertex', vertex)
            elif sector is not None:
                self.selected = ('sector', sector)
            self.delete_selected()
            return

        if entity is not None:
            self.selected = ('entity', entity)
            self.snapshot()
            self.drag = ('entity', entity)
            current = self.model.level.objects[entity]
            self.entity_type = current['type']
            self.entity_angle = current.get('angle', 0)
            self.entity_param = current.get('param', 0)
        elif vertex is not None:
            self.selected = ('vertex', vertex)
            self.snapshot()
            self.drag = ('vertex', vertex)
        elif wall is not None:
            self.selected = ('wall', wall)
        elif sector is not None:
            self.selected = ('sector', sector)
            self.current_sector_id = sector
        else:
            self.selected = None

    def map_drag(self, position):
        if self.drag is None:
            return
        x, z = self.screen_to_world(position[0], position[1])
        x, z = self.snap_position(x, z)
        kind, index = self.drag
        if kind == 'entity' and 0 <= index < len(self.model.level.objects):
            entity = self.model.level.objects[index]
            entity['x'] = int(round(x))
            entity['z'] = int(round(z))
            self.model.mark_dirty()
        elif kind == 'vertex' and 0 <= index < len(self.model.level.vertices):
            self.model.level.vertices[index] = (int(round(x)), int(round(z)))
            self.mark_geometry_changed()

    def zoom_at(self, position, direction):
        old_x, old_z = self.screen_to_world(position[0], position[1])
        factor = 1.15 if direction > 0 else 1.0 / 1.15
        self.zoom = _clamp(self.zoom * factor, 0.08, 12.0)
        self.offset_x = position[0] - old_x * self.zoom
        self.offset_y = position[1] - old_z * self.zoom

    # ------------------------------- drawing ------------------------------

    def center_view(self):
        level = self.model.level
        rect = self.map_rect()
        if not level.vertices:
            self.offset_x = rect.centerx
            self.offset_y = rect.centery
            return
        xs = [point[0] for point in level.vertices]
        zs = [point[1] for point in level.vertices]
        span_x = max(64, max(xs) - min(xs))
        span_z = max(64, max(zs) - min(zs))
        self.zoom = _clamp(min((rect.width - 80) / float(span_x),
                               (rect.height - 100) / float(span_z)), 0.08, 6.0)
        self.offset_x = rect.centerx - (min(xs) + max(xs)) * self.zoom * 0.5
        self.offset_y = rect.centery - (min(zs) + max(zs)) * self.zoom * 0.5

    def draw_text(self, surface, text, size, position, color=TEXT_COLOR):
        font = self.font if size <= 18 else self.large_font
        image = font.render(str(text), True, color)
        surface.blit(image, position)
        return image.get_rect(topleft=position)

    def draw_button(self, surface, rect, label, action, active=False, color=None):
        color = color or ((52, 76, 98) if active else (43, 52, 63))
        pygame.draw.rect(surface, color, rect, border_radius=4)
        pygame.draw.rect(surface, (105, 128, 148), rect, 1, border_radius=4)
        image = self.small_font.render(label, True, TEXT_COLOR)
        surface.blit(image, image.get_rect(center=rect.center))
        self.button_actions.append((rect, action))

    def draw_editor(self):
        self.screen.fill((25, 30, 36))
        self.button_actions = []
        self.draw_map()
        self.draw_panel()
        self.draw_status()

    def draw_map(self):
        level = self.model.level
        rect = self.map_rect()
        pygame.draw.rect(self.screen, MAP_BACKGROUND, rect)
        self.draw_grid(rect)
        pygame.draw.rect(self.screen, (90, 105, 120), rect, 1)
        old_clip = self.screen.get_clip()
        self.screen.set_clip(rect)

        # Walls first; arrowhead indicates front direction and is essential
        # because C3D requires clockwise sector boundaries.
        for index, wall in enumerate(level.walls):
            if wall['sv'] >= len(level.vertices) or wall['ev'] >= len(level.vertices):
                continue
            first = level.vertices[wall['sv']]
            second = level.vertices[wall['ev']]
            start = self.world_to_screen(first[0], first[1])
            end = self.world_to_screen(second[0], second[1])
            color = WARNING if self.selected == ('wall', index) else (193, 204, 214)
            width = 3 if self.selected == ('wall', index) else 2
            pygame.draw.line(self.screen, color, start, end, width)
            self.draw_wall_arrow(start, end, color)
            if self.zoom > 1.4:
                self.draw_text(self.screen, str(index), 15,
                               ((start[0] + end[0]) // 2 + 3, (start[1] + end[1]) // 2 + 3),
                               MUTED_TEXT)

        for index, vertex in enumerate(level.vertices):
            point = self.world_to_screen(vertex[0], vertex[1])
            color = ACCENT if self.selected == ('vertex', index) else (88, 186, 238)
            pygame.draw.circle(self.screen, color, point, 5)
            pygame.draw.circle(self.screen, (10, 20, 30), point, 5, 1)

        for index, entity in enumerate(level.objects):
            point = self.world_to_screen(entity['x'], entity['z'])
            spawn = 1 <= int(entity['type']) <= 4
            color = (104, 225, 139) if spawn else (245, 156, 64)
            if self.selected == ('entity', index):
                color = WARNING
            pygame.draw.circle(self.screen, color, point, 8)
            angle = -math.pi * 0.5 - math.radians(float(entity.get('angle', 0)))
            tip = (int(point[0] + math.cos(angle) * 15),
                   int(point[1] + math.sin(angle) * 15))
            pygame.draw.line(self.screen, (15, 20, 25), point, tip, 3)
            pygame.draw.line(self.screen, color, point, tip, 2)
            self.draw_text(self.screen, '%d:%d' % (index, entity['type']), 15,
                           (point[0] + 9, point[1] - 8), color)

        if self.wall_anchor is not None and self.wall_anchor < len(level.vertices):
            vertex = level.vertices[self.wall_anchor]
            point = self.world_to_screen(vertex[0], vertex[1])
            pygame.draw.circle(self.screen, WARNING, point, 10, 2)
        self.screen.set_clip(old_clip)

    def draw_grid(self, rect):
        step = self.grid * self.zoom
        if step < 10:
            return
        start_x = int(rect.left + ((self.offset_x - rect.left) % step))
        start_y = int(rect.top + ((self.offset_y - rect.top) % step))
        for x in range(start_x, rect.right, max(1, int(step))):
            pygame.draw.line(self.screen, GRID_COLOR, (x, rect.top), (x, rect.bottom))
        for y in range(start_y, rect.bottom, max(1, int(step))):
            pygame.draw.line(self.screen, GRID_COLOR, (rect.left, y), (rect.right, y))

    def draw_wall_arrow(self, first, second, color):
        dx = second[0] - first[0]
        dy = second[1] - first[1]
        length = math.hypot(dx, dy)
        if length < 12:
            return
        nx = dx / length
        ny = dy / length
        mid_x = (first[0] + second[0]) * 0.5
        mid_y = (first[1] + second[1]) * 0.5
        tip = (int(mid_x + nx * 7), int(mid_y + ny * 7))
        left = (int(mid_x - nx * 5 - ny * 4), int(mid_y - ny * 5 + nx * 4))
        right = (int(mid_x - nx * 5 + ny * 4), int(mid_y - ny * 5 - nx * 4))
        pygame.draw.polygon(self.screen, color, (tip, left, right))

    def draw_panel(self):
        width, height = self.screen.get_size()
        panel = pygame.Rect(0, 0, self.PANEL_WIDTH, height)
        pygame.draw.rect(self.screen, (31, 38, 47), panel)
        pygame.draw.line(self.screen, (100, 120, 140), (self.PANEL_WIDTH - 1, 0),
                         (self.PANEL_WIDTH - 1, height), 1)
        self.draw_text(self.screen, 'C3D2 PACKAGE', 23, (12, 10), ACCENT)
        package_name = os.path.basename(self.model.package_dir)
        self.draw_text(self.screen, package_name, 16, (12, 33), MUTED_TEXT)

        y = 58
        self.draw_button(self.screen, pygame.Rect(10, y, 91, 28), 'Open', 'open')
        self.draw_button(self.screen, pygame.Rect(106, y, 91, 28), 'Save', 'save', self.model.dirty)
        self.draw_button(self.screen, pygame.Rect(202, y, 104, 28), 'Build F5', 'build')
        y += 38

        for column, (label, tool) in enumerate((('1 Select', 'select'), ('2 Wall', 'wall'),
                                                  ('3 Entity', 'entity'), ('4 Delete', 'delete'))):
            self.draw_button(self.screen, pygame.Rect(10 + column * 75, y, 70, 28),
                             label, 'tool:' + tool, self.tool == tool)
        y += 34
        self.draw_button(self.screen, pygame.Rect(10, y, 140, 25), '+ Sector N', 'addsector')
        self.draw_button(self.screen, pygame.Rect(156, y, 150, 25), 'Portal back B', 'portalback')
        y += 38

        self.draw_text(self.screen, 'Texture import', 18, (12, y), ACCENT)
        y += 23
        kind_label = {'wall': 'WALL', 'flat': 'FLAT', 'sky': 'SKY', 'sprite': 'SPRITE'}[self.current_kind]
        self.draw_button(self.screen, pygame.Rect(10, y, 100, 27), 'Kind: ' + kind_label, 'kind')
        self.draw_button(self.screen, pygame.Rect(115, y, 91, 27), 'Import I', 'import')
        self.draw_button(self.screen, pygame.Rect(211, y, 95, 27),
                         'Fit' if self.fit_import else 'Stretch', 'fit', self.fit_import)
        y += 34
        if self.current_kind != TEXTURES.MATERIAL_SKY:
            self.draw_button(self.screen, pygame.Rect(10, y, 34, 25), '<', 'slot-')
            self.draw_text(self.screen, 'slot %d' % self.current_slot, 16, (49, y + 5), TEXT_COLOR)
            self.draw_button(self.screen, pygame.Rect(108, y, 34, 25), '>', 'slot+')
        if self.current_kind == TEXTURES.MATERIAL_WALL:
            self.draw_button(self.screen, pygame.Rect(150, y, 46, 25), 'W%d' % self.wall_width, 'wallw')
            self.draw_button(self.screen, pygame.Rect(201, y, 58, 25), 'H%d' % self.wall_height, 'wallh')
        y += 34

        self.draw_text(self.screen, 'Apply material', 18, (12, y), ACCENT)
        y += 23
        self.draw_button(self.screen, pygame.Rect(10, y, 106, 26), 'Apply M', 'apply')
        self.draw_button(self.screen, pygame.Rect(121, y, 86, 26),
                         'wall:' + self.wall_part, 'wallpart')
        self.draw_button(self.screen, pygame.Rect(212, y, 94, 26),
                         'flat:' + self.flat_target, 'flattarget')
        y += 37

        self.draw_text(self.screen, 'Materials', 18, (12, y), ACCENT)
        y += 22
        keys = sorted(self.model.materials.keys(), key=_material_sort_key)
        if not keys:
            self.draw_text(self.screen, 'Импортируйте BMP/PNG/JPEG…', 15, (12, y), MUTED_TEXT)
            y += 22
        for key in keys[:10]:
            color = self.material_cache.get_color(key, (80, 80, 80))
            rect = pygame.Rect(10, y, 296, 22)
            pygame.draw.rect(self.screen, (47, 57, 68), rect, border_radius=3)
            pygame.draw.rect(self.screen, color, pygame.Rect(13, y + 4, 14, 14))
            self.draw_text(self.screen, '%s = %s' % (key, self.model.materials[key]),
                           15, (33, y + 3), TEXT_COLOR)
            self.button_actions.append((rect, 'material:' + key))
            y += 24

        y = max(y + 6, height - 195)
        self.draw_text(self.screen, 'Selection', 18, (12, y), ACCENT)
        y += 22
        for line in self.selection_lines():
            self.draw_text(self.screen, line, 15, (12, y), TEXT_COLOR)
            y += 17
        self.draw_button(self.screen, pygame.Rect(10, y + 3, 142, 26), 'Edit Enter', 'edit')
        self.draw_button(self.screen, pygame.Rect(158, y + 3, 148, 26), 'Fly F3', 'fly')

    def selection_lines(self):
        level = self.model.level
        if not self.selected:
            return ['нет выбора', 'Entity type=%d angle=%d param=%d' %
                    (self.entity_type, self.entity_angle, self.entity_param),
                    'Grid %d / snap %s' % (self.grid, 'on' if self.snap else 'off')]
        kind, index = self.selected
        if kind == 'entity' and index < len(level.objects):
            entity = level.objects[index]
            return ['entity.%d' % index, 'x=%d  z=%d' % (entity['x'], entity['z']),
                    'type=%d angle=%d param=%d sprite=%d' %
                    (entity['type'], entity.get('angle', 0), entity.get('param', 0),
                     entity.get('sprite', 0))]
        if kind == 'wall' and index < len(level.walls):
            wall = level.walls[index]
            surface = level.surfaces[wall['front']]
            return ['wall.%d  %d→%d' % (index, wall['sv'], wall['ev']),
                    'main=%d upper=%d lower=%d' %
                    (surface['main'], surface['upper'], surface['lower']),
                    'flags=%d type=%d' % (wall['flags'], wall['type'])]
        if kind == 'sector' and index < len(level.sectors):
            sector = level.sectors[index]
            return ['sector.%d' % index, 'floor=%d ceil=%d' % (sector['floor'], sector['ceil']),
                    'floor=%d ceil=%d light=%d' %
                    (sector['floor_tex'], sector['ceil_tex'], (sector['light_packed'] >> 4) & 15)]
        if kind == 'vertex' and index < len(level.vertices):
            vertex = level.vertices[index]
            return ['vertex.%d' % index, 'x=%d z=%d' % vertex,
                    'Перетаскивайте мышью']
        return ['выбор устарел']

    def draw_status(self):
        width, height = self.screen.get_size()
        overlay = pygame.Surface((width - self.PANEL_WIDTH, 64), pygame.SRCALPHA)
        overlay.fill((0, 0, 0, 150))
        self.screen.blit(overlay, (self.PANEL_WIDTH, height - 64))
        last = self.log_lines[-3:]
        for index, item in enumerate(last):
            self.draw_text(self.screen, item[0], 15,
                           (self.PANEL_WIDTH + 10, height - 58 + index * 18), item[1])
        self.draw_text(self.screen,
                       'F3 fly | Ctrl+S save | F5 compile | N sector | B portal | I import | M apply | R reverse | G snap',
                       15, (self.PANEL_WIDTH + 10, height - 17), MUTED_TEXT)

    def handle_button(self, position):
        for rect, action in reversed(self.button_actions):
            if rect.collidepoint(position):
                if action == 'open':
                    self.open_dialog()
                elif action == 'save':
                    self.save()
                elif action == 'build':
                    self.compile()
                elif action == 'addsector':
                    self.add_sector()
                elif action == 'portalback':
                    self.add_portal_back()
                elif action == 'import':
                    self.import_current_material()
                elif action == 'fit':
                    self.fit_import = not self.fit_import
                elif action == 'kind':
                    kinds = list(TEXTURES.VALID_MATERIAL_KINDS)
                    self.current_kind = kinds[(kinds.index(self.current_kind) + 1) % len(kinds)]
                elif action == 'slot-':
                    self.current_slot = max(1, self.current_slot - 1)
                elif action == 'slot+':
                    self.current_slot = min(127, self.current_slot + 1)
                elif action == 'wallw':
                    widths = (16, 32, 64, 128, 256)
                    self.wall_width = widths[(widths.index(self.wall_width) + 1) % len(widths)]
                elif action == 'wallh':
                    heights = list(TEXTURES.WALL_HEIGHTS)
                    self.wall_height = heights[(heights.index(self.wall_height) + 1) % len(heights)]
                elif action == 'apply':
                    self.apply_current_material()
                elif action == 'wallpart':
                    self.wall_part = {'main': 'upper', 'upper': 'lower', 'lower': 'main'}[self.wall_part]
                elif action == 'flattarget':
                    self.flat_target = 'ceiling' if self.flat_target == 'floor' else 'floor'
                elif action == 'edit':
                    self.edit_selected()
                elif action == 'fly':
                    self.flythrough.reset_camera()
                    self.mode = 'fly'
                elif action.startswith('tool:'):
                    self.tool = action.split(':', 1)[1]
                    self.wall_anchor = None
                elif action.startswith('material:'):
                    self.select_manifest_material(action.split(':', 1)[1])
                return

    def select_manifest_material(self, key):
        if key == 'sky':
            self.current_kind = TEXTURES.MATERIAL_SKY
            self.log('Выбрано sky')
            return
        prefix, slot = key.split('.', 1)
        if prefix in (TEXTURES.MATERIAL_WALL, TEXTURES.MATERIAL_FLAT,
                      TEXTURES.MATERIAL_SPRITE):
            self.current_kind = prefix
            self.current_slot = int(slot)
            self.log('Выбрано ' + key)

    def log(self, message, color=TEXT_COLOR):
        self.log_lines.append((str(message), color))
        if len(self.log_lines) > 100:
            del self.log_lines[:20]

    def run(self):
        while self.running:
            delta = self.clock.tick(60) / 1000.0
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    self.running = False
                else:
                    self.handle_event(event)
            if self.mode == 'fly':
                self.flythrough.update(delta)
                self.flythrough.render(self.screen)
            else:
                self.draw_editor()
            pygame.display.flip()
        pygame.quit()


def _material_sort_key(key):
    if key == 'sky':
        return (2, 0)
    prefix, dot, suffix = key.partition('.')
    order = 0 if prefix == 'wall' else (1 if prefix == 'flat' else 2)
    try:
        return order, int(suffix)
    except ValueError:
        return order, 9999


def require_pygame():
    if pygame is None:
        raise EditorError('Pygame is not installed. Run: python3 -m pip install pygame pillow')


def validate_package(path):
    model = PackageModel(path)
    info = C3.read_c3b(model.c3b_path) if os.path.exists(model.c3b_path) else None
    print('package:', model.package_dir)
    print('vertices=%d walls=%d sectors=%d entities=%d' %
          (len(model.level.vertices), len(model.level.walls), len(model.level.sectors),
           len(model.level.objects)))
    print('materials:', len(model.materials))
    if info:
        print('C3B:', info)
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description='C3D2 Pygame level editor')
    parser.add_argument('package', nargs='?', default=DEFAULT_PACKAGE,
                        help='directory with level.c3d.json')
    parser.add_argument('--new', action='store_true',
                        help='create a new package if the directory is empty')
    parser.add_argument('--validate', action='store_true',
                        help='load and validate a package without Pygame')
    args = parser.parse_args(argv)

    if args.validate:
        return validate_package(args.package)
    try:
        require_pygame()
        pygame.init()
        pygame.font.init()
        model = PackageModel(args.package, create=args.new)
        EditorApp(model).run()
        return 0
    except EditorError as error:
        print('C3D2 editor:', error, file=sys.stderr)
        return 2
    except Exception:
        traceback.print_exc()
        return 1


if __name__ == '__main__':
    sys.exit(main())
