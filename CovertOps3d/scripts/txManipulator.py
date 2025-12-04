# tx1_editor_advanced_complete.py
# Продвинутый TX1 редактор с полным контролем параметров текстур и K-means квантованием

import struct
import os
import sys
import colorsys
import numpy as np
from PIL import Image, ImageTk
import tkinter as tk
from tkinter import ttk, filedialog, messagebox, simpledialog
from typing import List, Optional, Tuple

# Попробуем импортировать sklearn для K-means, но сделаем fallback
try:
    from sklearn.cluster import KMeans
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False
    print("Warning: sklearn not available. Using PIL quantization instead.")

class TXTexture:
    def __init__(self):
        self.id = 0
        self.width = 0
        self.height = 0
        self.palette_index = 0
        self.bit_depth = 0
        self.pixel_data = bytearray()
        self.pixels = []  # row-major список индексов
        self.name = ""

    def __str__(self):
        return f"Texture {self.id}: {self.width}x{self.height}, {self.bit_depth}bpp, pal={self.palette_index}"

class TXContainer:
    def __init__(self):
        self.signature = 0
        self.texture_count = 0
        self.palette_count = 0
        self.reserved = 0
        self.textures: List[TXTexture] = []
        self.palettes: List[List[int]] = []
        self.original_file_path = ""

    def read_uint16_be(self, data: bytes, offset: int) -> tuple[int, int]:
        val = struct.unpack_from('>H', data, offset)[0]
        return val, offset + 2

    def read_uint32_be(self, data: bytes, offset: int) -> tuple[int, int]:
        val = struct.unpack_from('>I', data, offset)[0]
        return val, offset + 4

    def read_int32_be(self, data: bytes, offset: int) -> tuple[int, int]:
        val = struct.unpack_from('>i', data, offset)[0]
        return val, offset + 4

    def load_from_file(self, filename: str) -> bool:
        try:
            self.original_file_path = filename
            with open(filename, 'rb') as f:
                data = f.read()
            return self.load_from_data(data)
        except Exception as e:
            messagebox.showerror("Ошибка", f"Не удалось открыть файл:\n{e}")
            return False

    def load_from_data(self, data: bytes) -> bool:
        try:
            if len(data) < 16:
                raise ValueError("Файл слишком мал")

            offset = 0
            self.signature, offset = self.read_uint16_be(data, offset)
            self.texture_count, offset = self.read_uint16_be(data, offset)
            self.palette_count, offset = self.read_uint16_be(data, offset)
            self.reserved, offset = self.read_uint32_be(data, offset)

            print(f"TX1: sig=0x{self.signature:04X}, tex={self.texture_count}, pal={self.palette_count}")

            # --- Текстуры ---
            self.textures = []
            for i in range(self.texture_count):
                if offset + 10 > len(data):
                    break

                tex = TXTexture()
                tex.id = data[offset]; offset += 1
                tex.width, offset = self.read_uint16_be(data, offset)
                tex.height, offset = self.read_uint16_be(data, offset)
                tex.palette_index, offset = self.read_uint16_be(data, offset)
                tex.bit_depth, offset = self.read_uint16_be(data, offset)
                tex.name = f"Tex_{tex.id:03d}"

                bits = tex.width * tex.height * tex.bit_depth
                byte_len = (bits + 7) // 8
                if offset + byte_len > len(data):
                    print(f"Ошибка: не хватает данных для текстуры {i}")
                    break

                raw = data[offset:offset + byte_len]
                offset += byte_len
                tex.pixel_data = raw
                tex.pixels = self.unpack_pixels(tex, raw)
                self.textures.append(tex)

            # --- Палитры ---
            self.palettes = []
            for i in range(self.palette_count):
                if offset + 4 > len(data):
                    break
                count, offset = self.read_int32_be(data, offset)
                if offset + count * 4 > len(data):
                    break
                palette = []
                for _ in range(count):
                    color, offset = self.read_uint32_be(data, offset)
                    palette.append(color)
                self.palettes.append(palette)

            return True
        except Exception as e:
            import traceback
            traceback.print_exc()
            messagebox.showerror("Ошибка парсинга", str(e))
            return False

    def unpack_pixels(self, tex: TXTexture, raw_data: bytes) -> List[int]:
        """Правильная column-major распаковка как в оригинальной игре"""
        w, h = tex.width, tex.height
        total = w * h
        pixels = [0] * total
        bit_depth = tex.bit_depth
        bit_pos = 0

        for x in range(w):          # по столбцам!
            for y in range(h):      # по строкам внутри столбца
                pixel_val = 0
                for b in range(bit_depth):
                    byte_idx = bit_pos // 8
                    bit_idx = bit_pos % 8
                    if byte_idx < len(raw_data):
                        bit = (raw_data[byte_idx] >> (7 - bit_idx)) & 1
                        pixel_val = (pixel_val << 1) | bit
                    bit_pos += 1
                idx = y * w + x                     # переводим в row-major для PIL
                if idx < total:
                    pixels[idx] = pixel_val
        return pixels

    def pack_pixels(self, tex: TXTexture, pixels: List[int]) -> bytes:
        """Обратная упаковка в column-major"""
        w, h = tex.width, tex.height
        bit_depth = tex.bit_depth
        total = w * h
        bits = []

        # Собираем биты в column-major порядке
        for x in range(w):
            for y in range(h):
                idx = y * w + x
                val = pixels[idx] if idx < len(pixels) else 0
                val &= (1 << bit_depth) - 1
                # MSB first
                for b in range(bit_depth - 1, -1, -1):
                    bits.append((val >> b) & 1)

        # Упаковываем в байты
        packed = bytearray()
        for i in range(0, len(bits), 8):
            byte_val = 0
            for j in range(8):
                if i + j < len(bits):
                    byte_val |= bits[i + j] << (7 - j)
            packed.append(byte_val)

        expected = (total * bit_depth + 7) // 8
        return bytes(packed[:expected])

    def get_texture_image(self, idx: int, scale: int = 1) -> Optional[Image.Image]:
        if idx < 0 or idx >= len(self.textures):
            return None
        tex = self.textures[idx]
        palette = self.palettes[tex.palette_index] if tex.palette_index < len(self.palettes) else None

        img = Image.new("RGBA", (tex.width, tex.height))
        pixels = img.load()

        for y in range(tex.height):
            for x in range(tex.width):
                i = y * tex.width + x
                if i >= len(tex.pixels):
                    pixels[x, y] = (255, 0, 255, 255)
                    continue
                color_idx = tex.pixels[i]
                if palette and color_idx < len(palette):
                    c = palette[color_idx]
                    a = (c >> 24) & 0xFF
                    r = (c >> 16) & 0xFF
                    g = (c >> 8) & 0xFF
                    b = c & 0xFF
                    pixels[x, y] = (r, g, b, 255 if a == 0 else a)
                else:
                    pixels[x, y] = (255, 0, 255, 255)  # magenta error

        if scale > 1:
            img = img.resize((tex.width * scale, tex.height * scale), Image.NEAREST)
        return img

    def create_new_texture(self, image_path: str, new_width: int, new_height: int, 
                          bit_depth: int, quantization_method: str = "kmeans") -> Tuple[TXTexture, List[int]]:
        """Создает новую текстуру с заданными параметрами"""
        try:
            # Загружаем и обрабатываем изображение
            img = Image.open(image_path)
            print(f"Original image: {img.size}, mode: {img.mode}")
            
            # Изменяем размер если нужно
            if img.size != (new_width, new_height):
                print(f"Resizing to {new_width}x{new_height}")
                img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
            
            # Определяем количество цветов
            max_colors = 1 << bit_depth
            print(f"Target colors: {max_colors}, method: {quantization_method}")
            
            # Квантование в зависимости от метода
            if quantization_method == "kmeans" and SKLEARN_AVAILABLE:
                quantized_img, palette = self._quantize_kmeans(img, max_colors)
            else:
                quantized_img, palette = self._quantize_pil(img, max_colors, quantization_method)
            
            # Создаем текстуру
            texture = TXTexture()
            texture.id = 0  # Будет установлен позже
            texture.width = new_width
            texture.height = new_height
            texture.bit_depth = bit_depth
            texture.palette_index = 0  # Будет установлен позже
            texture.name = f"New_tex_{new_width}x{new_height}_{bit_depth}bpp"
            
            # Получаем пиксели
            if quantized_img.mode == 'P':
                texture.pixels = list(quantized_img.getdata())
            else:
                texture.pixels = [quantized_img.getpixel((x, y)) 
                                for y in range(new_height) for x in range(new_width)]
            
            # Упаковываем пиксели
            texture.pixel_data = self.pack_pixels(texture, texture.pixels)
            
            return texture, palette
            
        except Exception as e:
            print(f"Error creating new texture: {e}")
            import traceback
            traceback.print_exc()
            raise

    def _quantize_kmeans(self, img: Image.Image, max_colors: int) -> Tuple[Image.Image, List[int]]:
        """Квантование с использованием K-means"""
        if img.mode != 'RGB':
            img = img.convert('RGB')
        
        # Конвертируем в numpy array
        img_array = np.array(img)
        h, w, c = img_array.shape
        
        # Reshape для K-means
        pixels = img_array.reshape(-1, 3)
        
        # Применяем K-means
        kmeans = KMeans(n_clusters=max_colors, random_state=0, n_init=10)
        labels = kmeans.fit_predict(pixels)
        
        # Создаем палитру
        palette_colors = kmeans.cluster_centers_.astype(int)
        palette = []
        for color in palette_colors:
            r, g, b = color
            argb_color = (0xFF << 24) | (r << 16) | (g << 8) | b
            palette.append(argb_color)
        
        # Создаем индексированное изображение
        indexed_data = labels.reshape(h, w)
        quantized_img = Image.fromarray(indexed_data.astype('uint8'), mode='P')
        
        # Устанавливаем палитру
        pil_palette = []
        for color in palette_colors:
            pil_palette.extend([int(c) for c in color])
        # Дополняем до 768 цветов
        while len(pil_palette) < 768:
            pil_palette.append(0)
        quantized_img.putpalette(pil_palette)
        
        return quantized_img, palette

    def _quantize_pil(self, img: Image.Image, max_colors: int, method: str) -> Tuple[Image.Image, List[int]]:
        """Квантование с использованием PIL методов"""
        if img.mode != 'RGB':
            img = img.convert('RGB')
        
        # Выбираем метод квантования
        if method == "median_cut":
            quantize_method = Image.Quantize.MEDIANCUT
        elif method == "max_coverage":
            quantize_method = Image.Quantize.MAXCOVERAGE
        else:  # default
            quantize_method = Image.Quantize.MEDIANCUT
        
        # Квантование
        quantized = img.quantize(colors=max_colors, method=quantize_method)
        
        # Получаем палитру
        palette_colors = quantized.getpalette()
        palette = []
        for i in range(max_colors):
            r = palette_colors[i * 3] if i * 3 < len(palette_colors) else 0
            g = palette_colors[i * 3 + 1] if i * 3 + 1 < len(palette_colors) else 0
            b = palette_colors[i * 3 + 2] if i * 3 + 2 < len(palette_colors) else 0
            argb_color = (0xFF << 24) | (r << 16) | (g << 8) | b
            palette.append(argb_color)
        
        return quantized, palette

    def replace_texture_advanced(self, texture_index: int, image_path: str, 
                                new_width: int, new_height: int, bit_depth: int,
                                quantization_method: str = "kmeans",
                                create_new_palette: bool = True) -> bool:
        """Расширенная замена текстуры с полным контролем параметров"""
        try:
            # Создаем новую текстуру
            new_texture, new_palette = self.create_new_texture(
                image_path, new_width, new_height, bit_depth, quantization_method
            )
            
            # Копируем ID и настраиваем палитру
            original_texture = self.textures[texture_index]
            new_texture.id = original_texture.id
            
            if create_new_palette:
                # Создаем новую палитру
                self.palettes.append(new_palette)
                new_texture.palette_index = len(self.palettes) - 1
            else:
                # Используем существующую палитру (если подходит)
                new_texture.palette_index = original_texture.palette_index
                if original_texture.palette_index < len(self.palettes):
                    # Обрезаем/расширяем палитру если нужно
                    existing_palette = self.palettes[original_texture.palette_index]
                    max_colors = 1 << bit_depth
                    if len(existing_palette) != max_colors:
                        # Адаптируем палитру
                        if len(existing_palette) > max_colors:
                            self.palettes[original_texture.palette_index] = existing_palette[:max_colors]
                        else:
                            # Дополняем палитру
                            additional_colors = max_colors - len(existing_palette)
                            for i in range(additional_colors):
                                color = self._create_default_color(i)
                                existing_palette.append(color)
            
            # Заменяем текстуру
            self.textures[texture_index] = new_texture
            
            print(f"Texture replaced: {new_width}x{new_height}, {bit_depth}bpp, {quantization_method} quantization")
            return True
            
        except Exception as e:
            print(f"Error in advanced texture replacement: {e}")
            import traceback
            traceback.print_exc()
            return False

    def _create_default_color(self, index: int) -> int:
        """Создает цвет по умолчанию"""
        hue = (index * 137) % 360 / 360.0  # Золотое сечение для распределения
        r, g, b = [int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.7, 0.8)]
        return (0xFF << 24) | (r << 16) | (g << 8) | b

    def add_new_texture(self, image_path: str, texture_id: int, new_width: int, new_height: int,
                       bit_depth: int, quantization_method: str = "kmeans") -> bool:
        """Добавляет совершенно новую текстуру в контейнер"""
        try:
            new_texture, new_palette = self.create_new_texture(
                image_path, new_width, new_height, bit_depth, quantization_method
            )
            
            new_texture.id = texture_id
            self.palettes.append(new_palette)
            new_texture.palette_index = len(self.palettes) - 1
            
            self.textures.append(new_texture)
            self.texture_count = len(self.textures)
            
            print(f"New texture added: ID {texture_id}, {new_width}x{new_height}, {bit_depth}bpp")
            return True
            
        except Exception as e:
            print(f"Error adding new texture: {e}")
            return False

    def save_to_file(self, path: str) -> bool:
        try:
            with open(path, "wb") as f:
                f.write(struct.pack(">H", self.signature))
                f.write(struct.pack(">H", len(self.textures)))
                f.write(struct.pack(">H", len(self.palettes)))
                f.write(struct.pack(">I", self.reserved))

                for tex in self.textures:
                    f.write(struct.pack("B", tex.id))
                    f.write(struct.pack(">H", tex.width))
                    f.write(struct.pack(">H", tex.height))
                    f.write(struct.pack(">H", tex.palette_index))
                    f.write(struct.pack(">H", tex.bit_depth))
                    f.write(tex.pixel_data)

                for pal in self.palettes:
                    f.write(struct.pack(">i", len(pal)))
                    for c in pal:
                        f.write(struct.pack(">I", c))
            return True
        except Exception as e:
            messagebox.showerror("Ошибка сохранения", str(e))
            return False

    def export_texture(self, texture_index: int, filename: str) -> bool:
        """Экспорт текстуры в BMP"""
        img = self.get_texture_image(texture_index)
        if img:
            if img.mode == 'RGBA':
                background = Image.new('RGB', img.size, (255, 255, 255))
                background.paste(img, mask=img.split()[-1])
                img = background
            img.save(filename, 'BMP')
            return True
        return False

class AdvancedReplaceDialog:
    """Диалог расширенной замены текстуры"""
    
    def __init__(self, parent, current_texture=None):
        self.parent = parent
        self.current_texture = current_texture
        self.result = None
        
        self.dialog = tk.Toplevel(parent)
        self.dialog.title("Advanced Texture Replacement")
        self.dialog.geometry("500x600")
        self.dialog.transient(parent)
        self.dialog.grab_set()
        
        self.setup_ui()
    
    def setup_ui(self):
        main_frame = ttk.Frame(self.dialog, padding="20")
        main_frame.pack(fill=tk.BOTH, expand=True)
        
        ttk.Label(main_frame, text="Advanced Texture Settings", 
                 font=("Arial", 14, "bold")).pack(pady=(0, 20))
        
        # Текущая информация
        if self.current_texture:
            info_text = f"Current: {self.current_texture.width}x{self.current_texture.height}, {self.current_texture.bit_depth}bpp"
            ttk.Label(main_frame, text=info_text, foreground="gray").pack(pady=(0, 10))
        
        # Размеры
        size_frame = ttk.LabelFrame(main_frame, text="Dimensions", padding="10")
        size_frame.pack(fill=tk.X, pady=(0, 10))
        
        ttk.Label(size_frame, text="Width:").grid(row=0, column=0, sticky=tk.W)
        self.width_var = tk.IntVar(value=self.current_texture.width if self.current_texture else 64)
        width_spin = ttk.Spinbox(size_frame, from_=1, to=1024, textvariable=self.width_var, width=10)
        width_spin.grid(row=0, column=1, sticky=tk.W, padx=(10, 0))
        
        ttk.Label(size_frame, text="Height:").grid(row=1, column=0, sticky=tk.W, pady=(5, 0))
        self.height_var = tk.IntVar(value=self.current_texture.height if self.current_texture else 64)
        height_spin = ttk.Spinbox(size_frame, from_=1, to=1024, textvariable=self.height_var, width=10)
        height_spin.grid(row=1, column=1, sticky=tk.W, padx=(10, 0), pady=(5, 0))
        
        # Битность
        bit_depth_frame = ttk.LabelFrame(main_frame, text="Bit Depth", padding="10")
        bit_depth_frame.pack(fill=tk.X, pady=(0, 10))
        
        self.bit_depth_var = tk.StringVar(value=str(self.current_texture.bit_depth if self.current_texture else 4))
        
        bit_depths = [("2 bpp (4 colors)", "2"), 
                     ("3 bpp (8 colors)", "3"),
                     ("4 bpp (16 colors)", "4"), 
                     ("8 bpp (256 colors)", "8")]
        
        for i, (text, value) in enumerate(bit_depths):
            ttk.Radiobutton(bit_depth_frame, text=text, 
                           variable=self.bit_depth_var, value=value).pack(anchor=tk.W)
        
        # Метод квантования
        quant_frame = ttk.LabelFrame(main_frame, text="Quantization Method", padding="10")
        quant_frame.pack(fill=tk.X, pady=(0, 10))
        
        self.quant_var = tk.StringVar(value="kmeans" if SKLEARN_AVAILABLE else "median_cut")
        
        if SKLEARN_AVAILABLE:
            ttk.Radiobutton(quant_frame, text="K-means (лучшее качество)", 
                           variable=self.quant_var, value="kmeans").pack(anchor=tk.W)
        
        ttk.Radiobutton(quant_frame, text="Median Cut (PIL)", 
                       variable=self.quant_var, value="median_cut").pack(anchor=tk.W, pady=(5, 0))
        ttk.Radiobutton(quant_frame, text="Max Coverage (PIL)", 
                       variable=self.quant_var, value="max_coverage").pack(anchor=tk.W, pady=(2, 0))
        
        if not SKLEARN_AVAILABLE:
            warning_text = "K-means unavailable. Install: pip install scikit-learn"
            ttk.Label(quant_frame, text=warning_text, foreground="orange", 
                     font=("Arial", 8)).pack(anchor=tk.W, pady=(5, 0))
        
        # Палитра
        palette_frame = ttk.LabelFrame(main_frame, text="Palette", padding="10")
        palette_frame.pack(fill=tk.X, pady=(0, 10))
        
        self.palette_var = tk.StringVar(value="new")
        ttk.Radiobutton(palette_frame, text="Create New Palette", 
                       variable=self.palette_var, value="new").pack(anchor=tk.W)
        ttk.Radiobutton(palette_frame, text="Use Existing Palette", 
                       variable=self.palette_var, value="existing").pack(anchor=tk.W, pady=(5, 0))
        
        # Предпросмотр
        self.preview_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(main_frame, text="Show preview before applying", 
                       variable=self.preview_var).pack(anchor=tk.W, pady=(10, 0))
        
        # Кнопки
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill=tk.X, pady=(20, 0))
        
        ttk.Button(button_frame, text="Cancel", command=self.cancel).pack(side=tk.RIGHT, padx=(5, 0))
        ttk.Button(button_frame, text="Select Image", command=self.confirm).pack(side=tk.RIGHT)
    
    def confirm(self):
        self.result = {
            'width': self.width_var.get(),
            'height': self.height_var.get(),
            'bit_depth': int(self.bit_depth_var.get()),
            'quantization': self.quant_var.get(),
            'palette_mode': self.palette_var.get(),
            'preview': self.preview_var.get()
        }
        self.dialog.destroy()
    
    def cancel(self):
        self.result = None
        self.dialog.destroy()
    
    def show(self):
        self.dialog.wait_window()
        return self.result

class TexturePreviewDialog:
    """Диалог предпросмотра новой текстуры"""
    
    def __init__(self, parent, original_img, new_img, settings):
        self.parent = parent
        self.original_img = original_img
        self.new_img = new_img
        self.settings = settings
        self.result = None
        
        self.dialog = tk.Toplevel(parent)
        self.dialog.title("Texture Preview")
        self.dialog.geometry("700x500")
        self.dialog.transient(parent)
        self.dialog.grab_set()
        
        self.setup_ui()
    
    def setup_ui(self):
        main_frame = ttk.Frame(self.dialog, padding="20")
        main_frame.pack(fill=tk.BOTH, expand=True)
        
        ttk.Label(main_frame, text="Texture Preview", 
                 font=("Arial", 14, "bold")).pack(pady=(0, 20))
        
        # Настройки
        settings_text = (
            f"Size: {self.settings['width']}x{self.settings['height']}\n"
            f"Bit Depth: {self.settings['bit_depth']} bpp\n"
            f"Colors: {1 << self.settings['bit_depth']}\n"
            f"Quantization: {self.settings['quantization']}"
        )
        ttk.Label(main_frame, text=settings_text).pack(pady=(0, 10))
        
        # Изображения
        img_frame = ttk.Frame(main_frame)
        img_frame.pack(fill=tk.BOTH, expand=True)
        
        # Оригинал
        orig_frame = ttk.Frame(img_frame)
        orig_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(0, 10))
        ttk.Label(orig_frame, text="Original").pack()
        
        self.original_photo = ImageTk.PhotoImage(self.original_img)
        orig_canvas = tk.Canvas(orig_frame, width=300, height=300, bg='white')
        orig_canvas.pack(fill=tk.BOTH, expand=True)
        orig_canvas.create_image(150, 150, image=self.original_photo, anchor=tk.CENTER)
        
        # Новая текстура
        new_frame = ttk.Frame(img_frame)
        new_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True, padx=(10, 0))
        ttk.Label(new_frame, text="New Texture").pack()
        
        self.new_photo = ImageTk.PhotoImage(self.new_img)
        new_canvas = tk.Canvas(new_frame, width=300, height=300, bg='white')
        new_canvas.pack(fill=tk.BOTH, expand=True)
        new_canvas.create_image(150, 150, image=self.new_photo, anchor=tk.CENTER)
        
        # Кнопки
        button_frame = ttk.Frame(main_frame)
        button_frame.pack(fill=tk.X, pady=(20, 0))
        
        ttk.Button(button_frame, text="Cancel", command=self.cancel).pack(side=tk.RIGHT, padx=(5, 0))
        ttk.Button(button_frame, text="Apply", command=self.apply).pack(side=tk.RIGHT)
    
    def apply(self):
        self.result = True
        self.dialog.destroy()
    
    def cancel(self):
        self.result = False
        self.dialog.destroy()
    
    def show(self):
        self.dialog.wait_window()
        return self.result

class PaletteViewer:
    """Просмотрщик палитры"""
    
    def __init__(self, parent, palette, title="Palette"):
        self.parent = parent
        self.palette = palette
        
        self.dialog = tk.Toplevel(parent)
        self.dialog.title(title)
        self.dialog.geometry("600x200")
        self.dialog.transient(parent)
        
        self.setup_ui()
    
    def setup_ui(self):
        main_frame = ttk.Frame(self.dialog, padding="10")
        main_frame.pack(fill=tk.BOTH, expand=True)
        
        ttk.Label(main_frame, text=f"Palette with {len(self.palette)} colors", 
                 font=("Arial", 10, "bold")).pack(pady=(0, 10))
        
        # Canvas для отображения палитры
        self.canvas = tk.Canvas(main_frame, height=50, bg='white')
        self.canvas.pack(fill=tk.X, pady=(0, 10))
        
        # Отображаем цвета палитры
        self.draw_palette()
        
        # Информация о цветах
        info_frame = ttk.Frame(main_frame)
        info_frame.pack(fill=tk.X)
        
        ttk.Label(info_frame, text="Click on a color to see RGB values").pack()
        
        # Bind события клика
        self.canvas.bind("<Button-1>", self.on_color_click)
    
    def draw_palette(self):
        """Отрисовка палитры на canvas"""
        self.canvas.delete("all")
        
        color_count = len(self.palette)
        if color_count == 0:
            return
            
        color_width = 600 / color_count
        
        for i, color in enumerate(self.palette):
            x1 = i * color_width
            x2 = (i + 1) * color_width
            
            r = (color >> 16) & 0xFF
            g = (color >> 8) & 0xFF
            b = color & 0xFF
            
            hex_color = f"#{r:02x}{g:02x}{b:02x}"
            self.canvas.create_rectangle(x1, 0, x2, 50, fill=hex_color, outline="")
            
            # Добавляем номер цвета для маленьких палитр
            if color_count <= 16:
                text_color = "white" if (r + g + b) < 384 else "black"
                self.canvas.create_text(x1 + color_width/2, 25, text=str(i), 
                                      fill=text_color, font=("Arial", 8))
    
    def on_color_click(self, event):
        """Обработка клика по цвету"""
        color_count = len(self.palette)
        if color_count == 0:
            return
            
        color_width = 600 / color_count
        color_index = int(event.x / color_width)
        
        if 0 <= color_index < len(self.palette):
            color = self.palette[color_index]
            r = (color >> 16) & 0xFF
            g = (color >> 8) & 0xFF
            b = color & 0xFF
            a = (color >> 24) & 0xFF
            
            messagebox.showinfo(
                f"Color {color_index}",
                f"RGB: ({r}, {g}, {b})\n"
                f"Hex: #{r:02x}{g:02x}{b:02x}\n"
                f"Alpha: {a}"
            )

class TX1Editor(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("TX1 Texture Editor — Advanced")
        self.geometry("1200x700")
        self.container = TXContainer()
        self.current_idx = 0
        self.modified = False

        self.setup_ui()
        self.photo = None

    def setup_ui(self):
        # Меню
        menubar = tk.Menu(self)
        filemenu = tk.Menu(menubar, tearoff=0)
        filemenu.add_command(label="Открыть TX1", command=self.open_file)
        filemenu.add_command(label="Сохранить", command=self.save_file)
        filemenu.add_command(label="Сохранить как...", command=self.save_as)
        filemenu.add_separator()
        filemenu.add_command(label="Выход", command=self.quit)
        menubar.add_cascade(label="Файл", menu=filemenu)
        
        texturemenu = tk.Menu(menubar, tearoff=0)
        texturemenu.add_command(label="Заменить текстуру (просто)", command=self.replace_tex_simple)
        texturemenu.add_command(label="Заменить текстуру (расширенно)", command=self.replace_tex_advanced)
        texturemenu.add_command(label="Добавить новую текстуру", command=self.add_new_texture)
        texturemenu.add_separator()
        texturemenu.add_command(label="Удалить текстуру", command=self.delete_texture)
        menubar.add_cascade(label="Текстуры", menu=texturemenu)
        
        toolsmenu = tk.Menu(menubar, tearoff=0)
        toolsmenu.add_command(label="Просмотр палитры", command=self.view_palette)
        toolsmenu.add_command(label="Отладочная информация", command=self.debug_info)
        menubar.add_cascade(label="Инструменты", menu=toolsmenu)
        
        self.config(menu=menubar)

        # Основной фрейм
        main = ttk.Frame(self, padding=10)
        main.pack(fill=tk.BOTH, expand=True)

        # Левая панель — список текстур
        left = ttk.Frame(main)
        left.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 10))

        ttk.Label(left, text="Текстуры:", font=("Arial", 10, "bold")).pack(anchor="w")
        self.listbox = tk.Listbox(left, width=40, height=35)
        self.listbox.pack(fill=tk.BOTH, expand=True)
        self.listbox.bind("<<ListboxSelect>>", self.on_select)

        # Правая панель
        right = ttk.Frame(main)
        right.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        # Превью
        preview_frame = ttk.LabelFrame(right, text="Превью", padding=10)
        preview_frame.pack(fill=tk.BOTH, expand=True)

        self.canvas_frame = ttk.Frame(preview_frame)
        self.canvas_frame.pack(fill=tk.BOTH, expand=True)
        
        self.canvas = tk.Canvas(self.canvas_frame, bg="#222222")
        self.canvas.pack(fill=tk.BOTH, expand=True)
        
        # Скроллбары для превью
        v_scrollbar = ttk.Scrollbar(self.canvas_frame, orient=tk.VERTICAL, command=self.canvas.yview)
        v_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        h_scrollbar = ttk.Scrollbar(preview_frame, orient=tk.HORIZONTAL, command=self.canvas.xview)
        h_scrollbar.pack(side=tk.BOTTOM, fill=tk.X)
        
        self.canvas.configure(yscrollcommand=v_scrollbar.set, xscrollcommand=h_scrollbar.set)

        # Кнопки
        btn_frame = ttk.Frame(right)
        btn_frame.pack(fill=tk.X, pady=5)

        ttk.Button(btn_frame, text="Простая замена", command=self.replace_tex_simple).pack(side=tk.LEFT, padx=2)
        ttk.Button(btn_frame, text="Расширенная замена", command=self.replace_tex_advanced).pack(side=tk.LEFT, padx=2)
        ttk.Button(btn_frame, text="Экспорт BMP", command=self.export_bmp).pack(side=tk.LEFT, padx=2)
        ttk.Button(btn_frame, text="Добавить текстуру", command=self.add_new_texture).pack(side=tk.LEFT, padx=2)
        
        # Масштаб
        scale_frame = ttk.Frame(btn_frame)
        scale_frame.pack(side=tk.RIGHT)
        ttk.Label(scale_frame, text="Масштаб:").pack(side=tk.LEFT)
        self.scale_var = tk.StringVar(value="4")
        scale_combo = ttk.Combobox(scale_frame, textvariable=self.scale_var, 
                                  values=["1", "2", "4", "8"], width=5, state="readonly")
        scale_combo.pack(side=tk.LEFT, padx=5)
        scale_combo.bind('<<ComboboxSelected>>', self.on_scale_change)

        # Инфо
        info = ttk.LabelFrame(right, text="Информация")
        info.pack(fill=tk.X, pady=5)
        self.info_text = tk.Text(info, height=6, state="disabled")
        self.info_text.pack(fill=tk.X, padx=5, pady=5)
        
        # Статус модификации
        self.status_label = ttk.Label(right, text="", foreground="red")
        self.status_label.pack(anchor=tk.W)

    def replace_tex_simple(self):
        """Простая замена текстуры (сохраняет параметры)"""
        if not self.container.textures:
            return
        
        path = filedialog.askopenfilename(filetypes=[("Images", "*.png *.jpg *.bmp *.gif")])
        if path:
            texture = self.container.textures[self.current_idx]
            if self.container.replace_texture_advanced(
                self.current_idx, path, 
                texture.width, texture.height, texture.bit_depth,
                "median_cut", False
            ):
                self.modified = True
                self.update_status()
                self.update_list()
                messagebox.showinfo("Готово", "Текстура успешно заменена!")
                self.show_texture(self.current_idx)

    def replace_tex_advanced(self):
        """Расширенная замена текстуры с выбором параметров"""
        if not self.container.textures:
            return
        
        texture = self.container.textures[self.current_idx]
        dialog = AdvancedReplaceDialog(self, texture)
        settings = dialog.show()
        
        if not settings:
            return
        
        path = filedialog.askopenfilename(filetypes=[("Images", "*.png *.jpg *.bmp *.gif")])
        if not path:
            return
        
        # Предпросмотр если нужно
        if settings['preview']:
            try:
                # Создаем временную текстуру для предпросмотра
                temp_texture, temp_palette = self.container.create_new_texture(
                    path, settings['width'], settings['height'], 
                    settings['bit_depth'], settings['quantization']
                )
                
                # Создаем изображение для предпросмотра
                preview_img = Image.new("RGBA", (temp_texture.width, temp_texture.height))
                preview_pixels = preview_img.load()
                
                for y in range(temp_texture.height):
                    for x in range(temp_texture.width):
                        idx = y * temp_texture.width + x
                        if idx < len(temp_texture.pixels):
                            color_idx = temp_texture.pixels[idx]
                            if color_idx < len(temp_palette):
                                color = temp_palette[color_idx]
                                r = (color >> 16) & 0xFF
                                g = (color >> 8) & 0xFF
                                b = color & 0xFF
                                preview_pixels[x, y] = (r, g, b, 255)
                
                # Оригинальное изображение
                original_img = Image.open(path).resize((200, 200), Image.Resampling.LANCZOS)
                preview_img = preview_img.resize((200, 200), Image.Resampling.NEAREST)
                
                # Показываем диалог предпросмотра
                preview_dialog = TexturePreviewDialog(self, original_img, preview_img, settings)
                if not preview_dialog.show():
                    return
                    
            except Exception as e:
                print(f"Preview error: {e}")
                if not messagebox.askyesno("Preview Failed", "Preview generation failed. Continue anyway?"):
                    return
        
        # Применяем изменения
        create_new_palette = (settings['palette_mode'] == 'new')
        if self.container.replace_texture_advanced(
            self.current_idx, path,
            settings['width'], settings['height'], settings['bit_depth'],
            settings['quantization'], create_new_palette
        ):
            self.modified = True
            self.update_status()
            self.update_list()
            messagebox.showinfo("Готово", "Текстура успешно заменена с новыми параметрами!")
            self.show_texture(self.current_idx)

    def add_new_texture(self):
        """Добавление новой текстуры"""
        # Простой диалог для выбора ID
        texture_id = simpledialog.askinteger("New Texture", "Enter texture ID:", minvalue=0, maxvalue=255)
        if texture_id is None:
            return
        
        # Диалог параметров
        dialog = AdvancedReplaceDialog(self)
        settings = dialog.show()
        if not settings:
            return
        
        path = filedialog.askopenfilename(filetypes=[("Images", "*.png *.jpg *.bmp *.gif")])
        if not path:
            return
        
        if self.container.add_new_texture(
            path, texture_id,
            settings['width'], settings['height'], settings['bit_depth'],
            settings['quantization']
        ):
            self.modified = True
            self.update_status()
            self.update_list()
            messagebox.showinfo("Готово", "Новая текстура успешно добавлена!")
            self.listbox.select_set(len(self.container.textures) - 1)
            self.show_texture(len(self.container.textures) - 1)

    def delete_texture(self):
        """Удаление текущей текстуры"""
        if not self.container.textures:
            return
        
        if messagebox.askyesno("Confirm", "Delete current texture?"):
            del self.container.textures[self.current_idx]
            self.container.texture_count = len(self.container.textures)
            self.modified = True
            self.update_status()
            self.update_list()
            
            if self.container.textures:
                self.current_idx = min(self.current_idx, len(self.container.textures) - 1)
                self.listbox.select_set(self.current_idx)
                self.show_texture(self.current_idx)
            else:
                self.canvas.delete("all")
                self.info_text.config(state="normal")
                self.info_text.delete(1.0, tk.END)
                self.info_text.config(state="disabled")

    def open_file(self):
        path = filedialog.askopenfilename(filetypes=[("TX1 files", "*.tx1"), ("All files", "*.*")])
        if not path:
            return
        if self.container.load_from_file(path):
            self.title(f"TX1 Editor — {os.path.basename(path)}")
            self.update_list()
            if self.container.textures:
                self.listbox.select_set(0)
                self.show_texture(0)
            self.modified = False
            self.update_status()

    def update_list(self):
        self.listbox.delete(0, tk.END)
        for i, t in enumerate(self.container.textures):
            status = " *" if self.modified else ""
            self.listbox.insert(tk.END, f"{i:3d} | ID {t.id:3d} | {t.width}x{t.height} | {t.bit_depth}bpp | pal {t.palette_index}{status}")

    def on_select(self, event):
        sel = self.listbox.curselection()
        if sel:
            self.show_texture(sel[0])

    def on_scale_change(self, event):
        self.show_texture(self.current_idx)

    def show_texture(self, idx: int):
        self.current_idx = idx
        if idx < 0 or idx >= len(self.container.textures):
            return
            
        tex = self.container.textures[idx]
        scale = int(self.scale_var.get())
        img = self.container.get_texture_image(idx, scale=scale)
        if img:
            self.photo = ImageTk.PhotoImage(img)
            self.canvas.delete("all")
            self.canvas.create_image(0, 0, anchor="nw", image=self.photo)
            self.canvas.config(scrollregion=self.canvas.bbox("all"))

        info = f"ID: {tex.id}\nРазмер: {tex.width}×{tex.height}\nБитность: {tex.bit_depth} bpp\nПалитра: {tex.palette_index} ({len(self.container.palettes[tex.palette_index]) if tex.palette_index < len(self.container.palettes) else 0} цветов)\nПиксельных данных: {len(tex.pixel_data)} байт"
        self.info_text.config(state="normal")
        self.info_text.delete(1.0, tk.END)
        self.info_text.insert(tk.END, info)
        self.info_text.config(state="disabled")

    def export_bmp(self):
        if not self.container.textures:
            return
        tex = self.container.textures[self.current_idx]
        path = filedialog.asksaveasfilename(defaultextension=".bmp", initialfile=f"tex_{tex.id:03d}.bmp")
        if path:
            if self.container.export_texture(self.current_idx, path):
                messagebox.showinfo("Готово", f"Экспортировано: {path}")
            else:
                messagebox.showerror("Ошибка", "Не удалось экспортировать текстуру")

    def save_file(self):
        if not self.container.textures:
            return
        
        if not self.container.original_file_path:
            self.save_as()
            return
        
        if self.container.save_to_file(self.container.original_file_path):
            self.modified = False
            self.update_status()
            self.update_list()
            messagebox.showinfo("Готово", f"Файл сохранён:\n{self.container.original_file_path}")

    def save_as(self):
        if not self.container.textures:
            return
        path = filedialog.asksaveasfilename(defaultextension=".tx1", filetypes=[("TX1", "*.tx1")])
        if path and self.container.save_to_file(path):
            self.modified = False
            self.update_status()
            self.update_list()
            messagebox.showinfo("Готово", f"Файл сохранён:\n{path}")
            self.container.original_file_path = path
            self.title(f"TX1 Editor — {os.path.basename(path)}")

    def view_palette(self):
        """Просмотр текущей палитры"""
        if not self.container.textures or not self.container.palettes:
            messagebox.showwarning("Warning", "No palette available")
            return
        
        texture = self.container.textures[self.current_idx]
        if texture.palette_index >= len(self.container.palettes):
            messagebox.showwarning("Warning", "Texture references invalid palette")
            return
        
        palette = self.container.palettes[texture.palette_index]
        
        # Показываем просмотрщик палитры
        PaletteViewer(self, palette, f"Palette {texture.palette_index} for Texture {texture.id}")

    def debug_info(self):
        """Вывод отладочной информации"""
        if not self.container.textures:
            messagebox.showwarning("Warning", "No file loaded")
            return
        
        texture = self.container.textures[self.current_idx]
        
        info_text = (
            f"Texture ID: {texture.id}\n"
            f"Size: {texture.width}x{texture.height}\n"
            f"Bit Depth: {texture.bit_depth}bpp\n"
            f"Palette Index: {texture.palette_index}\n"
            f"Pixel Data: {len(texture.pixel_data)} bytes\n"
            f"Unpacked Pixels: {len(texture.pixels)}\n"
            f"Expected Pixels: {texture.width * texture.height}\n"
            f"Total Textures: {len(self.container.textures)}\n"
            f"Total Palettes: {len(self.container.palettes)}"
        )
        
        messagebox.showinfo("Debug Information", info_text)

    def update_status(self):
        if self.modified:
            self.status_label.config(text="MODIFIED - Не забудьте сохранить файл!")
        else:
            self.status_label.config(text="")

if __name__ == "__main__":
    app = TX1Editor()
    app.mainloop()