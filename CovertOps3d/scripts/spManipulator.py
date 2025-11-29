#!/usr/bin/env python3
# sp_editor_complete.py
# Complete SP editor - create sprites of any resolution with fixed 4-bit depth

import struct
import os
from tkinter import *
from tkinter import filedialog, messagebox, ttk
from PIL import Image, ImageTk
import numpy as np

# ---------------------
# Utilities (big-endian)
# ---------------------
def read_u16_be(buf, off):
    return (buf[off] << 8) | buf[off + 1]

def read_u32_be(buf, off):
    return (buf[off] << 24) | (buf[off+1] << 16) | (buf[off+2] << 8) | buf[off+3]

def write_u16_be(value):
    return bytes([(value >> 8) & 0xFF, value & 0xFF])

def write_u32_be(value):
    return bytes([(value >> 24) & 0xFF, (value >> 16) & 0xFF, 
                  (value >> 8) & 0xFF, value & 0xFF])

# ---------------------
# BitReader MSB-first
# ---------------------
class BitReader:
    def __init__(self, data: bytes):
        self.data = data or b""
        self.byte = 0
        self.bit = 0

    def read_bits(self, n: int) -> int:
        val = 0
        for _ in range(n):
            if self.byte >= len(self.data):
                val <<= (n - _)
                break
            cur = self.data[self.byte]
            bit_val = (cur >> (7 - self.bit)) & 1
            val = (val << 1) | bit_val
            self.bit += 1
            if self.bit == 8:
                self.bit = 0
                self.byte += 1
        return val

# ---------------------
# BitWriter MSB-first
# ---------------------
class BitWriter:
    def __init__(self):
        self.current_byte = 0
        self.bit_pos = 0
        self.bytes = []

    def write_bits(self, value, num_bits):
        for i in range(num_bits):
            bit = (value >> (num_bits - 1 - i)) & 1
            self.current_byte = (self.current_byte << 1) | bit
            self.bit_pos += 1
            if self.bit_pos == 8:
                self.bytes.append(self.current_byte)
                self.current_byte = 0
                self.bit_pos = 0

    def get_bytes(self):
        if self.bit_pos > 0:
            self.current_byte <<= (8 - self.bit_pos)
            self.bytes.append(self.current_byte)
        return bytes(self.bytes)

# ---------------------
# SP Parser
# ---------------------
def parse_sp_file(path):
    with open(path, "rb") as f:
        buf = f.read()

    if len(buf) < 10:
        raise ValueError("File too short to be a valid SP")

    magic = read_u16_be(buf, 0)
    if magic not in (0x9953, 0x9954):
        raise ValueError(f"Bad magic: 0x{magic:04X} (expected 0x9953 or 0x9954)")

    sprite_count = read_u16_be(buf, 2)
    palette_count = read_u16_be(buf, 4)
    off = 10

    sprites = []
    for i in range(sprite_count):
        if off + 1 > len(buf):
            break
        raw_signed = struct.unpack(">b", buf[off:off+1])[0]; off += 1
        
        sprite_type = 'sprite' if raw_signed >= 0 else 'texture'
        
        if off + 12 > len(buf):
            break
        w = read_u16_be(buf, off); h = read_u16_be(buf, off+2); off += 4
        hoff = read_u16_be(buf, off); voff = read_u16_be(buf, off+2); off += 4
        pal_off = read_u16_be(buf, off); depth = read_u16_be(buf, off+2); off += 4

        total_bits = w * h * depth
        data_bytes = total_bits // 8 + (1 if total_bits % 8 else 0)
        available = min(data_bytes, max(0, len(buf) - off))
        data = buf[off:off+available]
        off += available

        sprites.append({
            "raw_id": raw_signed,
            "id_unsigned": raw_signed & 0xFF,
            "w": w,
            "h": h,
            "hoff": hoff,
            "voff": voff,
            "pal_off": pal_off,
            "depth": depth,
            "data": data,
            "expected_bytes": data_bytes,
            "available_bytes": available,
            "type": sprite_type
        })

    palettes = []
    for p in range(palette_count):
        if off + 4 > len(buf):
            break
        count = read_u32_be(buf, off); off += 4
        cols = []
        for _ in range(count):
            if off + 4 > len(buf):
                break
            val = read_u32_be(buf, off); off += 4
            r = (val >> 16) & 0xFF
            g = (val >> 8) & 0xFF
            b = val & 0xFF
            cols.append((r, g, b))
        palettes.append(cols)

    return sprites, palettes

# ---------------------
# Render sprite
# ---------------------
def render_sprite_to_image(sprite, palettes, palette_index=None):
    w = sprite["w"]
    h = sprite["h"]
    depth = sprite["depth"]
    data = sprite["data"] or b""
    
    if palette_index is None:
        palette_index = sprite["pal_off"]
    
    sprite_type = sprite.get("type", "sprite")
    palette = palettes[palette_index] if (0 <= palette_index < len(palettes)) else None

    if w <= 0 or h <= 0:
        return None

    if sprite_type == 'sprite':
        return render_sprite_type(sprite, palette, w, h, depth, data)
    else:
        return render_texture_type(sprite, palette, w, h, depth, data)

def render_sprite_type(sprite, palette, w, h, depth, data):
    br = BitReader(data)
    mask = (1 << depth) - 1 if depth > 0 else 0

    pixels = []
    total = w * h
    for i in range(total):
        v = br.read_bits(depth) & mask if depth > 0 else 0
        pixels.append(v)
    
    if w == 64 and h == 64:
        rotated = [0] * (w * h)
        for x in range(w):
            for y in range(h):
                src_idx = y * w + x
                dst_idx = x * h + (h - 1 - y)
                if src_idx < len(pixels) and dst_idx < len(rotated):
                    rotated[dst_idx] = pixels[src_idx]
        pixels = rotated

    img_pixels = []
    for color_idx in pixels:
        if palette and 0 <= color_idx < len(palette):
            img_pixels.append(palette[color_idx])
        else:
            maxv = max(1, mask)
            grey = int((color_idx / maxv) * 255) if maxv > 0 else 0
            img_pixels.append((grey, grey, grey))

    img = Image.new("RGB", (w, h))
    img.putdata(img_pixels)
    return img

def render_texture_type(sprite, palette, w, h, depth, data):
    br = BitReader(data)
    mask = (1 << depth) - 1 if depth > 0 else 0
    
    rows = []
    for x in range(w):
        row_data = [0] * ((h + 1) // 2)
        
        for y in range(h):
            v = br.read_bits(depth) & mask if depth > 0 else 0
            byte_index = y // 2
            if y % 2 == 0:
                row_data[byte_index] = (v << 4) & 0xF0
            else:
                row_data[byte_index] |= v & 0x0F
        rows.append(row_data)
    
    img = Image.new("RGB", (w, h))
    pixels = img.load()
    
    for x in range(w):
        if x < len(rows):
            row_data = rows[x]
            for y in range(h):
                byte_index = y // 2
                if byte_index < len(row_data):
                    if y % 2 == 0:
                        color_idx = (row_data[byte_index] >> 4) & 0x0F
                    else:
                        color_idx = row_data[byte_index] & 0x0F
                else:
                    color_idx = 0
                
                if palette and 0 <= color_idx < len(palette):
                    pixels[x, y] = palette[color_idx]
                else:
                    maxv = max(1, mask)
                    grey = int((color_idx / maxv) * 255) if maxv > 0 else 0
                    pixels[x, y] = (grey, grey, grey)
    
    return img

# ---------------------
# Image processing with fixed 4-bit depth
# ---------------------
def process_image_with_alpha(image):
    """Process image: convert alpha to black and ensure RGB mode"""
    # Convert to RGBA to handle alpha channel
    if image.mode != 'RGBA':
        image = image.convert('RGBA')
    
    # Create new image with white background to see alpha areas
    background = Image.new('RGBA', image.size, (0, 0, 0, 255))  # Black background
    alpha_composite = Image.alpha_composite(background, image)
    
    # Convert to RGB
    rgb_image = alpha_composite.convert('RGB')
    
    return rgb_image

def has_black_color(image, threshold=5):
    """Check if image contains black pixels (RGB all below threshold)"""
    pixels = list(image.getdata())
    black_pixels = [p for p in pixels if all(c < threshold for c in p)]
    return len(black_pixels) > 0

def extract_colors_from_image(image, max_colors=16):
    """Extract unique colors from image and create optimized palette"""
    # Convert to RGB if needed
    if image.mode != 'RGB':
        image = image.convert('RGB')
    
    # Get all unique colors
    pixels = list(image.getdata())
    unique_colors = list(set(pixels))
    
    # If we have more colors than needed, use color quantization
    if len(unique_colors) > max_colors:
        # Use PIL's quantization to reduce colors
        quantized = image.quantize(colors=max_colors)
        palette_colors = quantized.getpalette()
        if palette_colors:
            unique_colors = []
            for i in range(max_colors):
                r = palette_colors[i * 3]
                g = palette_colors[i * 3 + 1]
                b = palette_colors[i * 3 + 2]
                unique_colors.append((r, g, b))
        else:
            # Fallback: take most frequent colors
            from collections import Counter
            color_counts = Counter(pixels)
            unique_colors = [color for color, _ in color_counts.most_common(max_colors)]
    
    return unique_colors

def create_palette_from_image(image):
    """Create optimized 16-color palette from image with smart black handling"""
    # Process alpha channel first
    processed_image = process_image_with_alpha(image)
    
    # Extract colors from processed image
    colors = extract_colors_from_image(processed_image, 15)  # 15 + potential black = 16 colors
    
    # Check if image actually contains black color
    has_black = has_black_color(processed_image)
    
    palette = []
    
    if has_black:
        # If black exists, put it at index 0
        palette = [(0, 0, 0)]  # Black at index 0
        
        # Add other colors, avoiding duplicates and black
        for color in colors:
            if color != (0, 0, 0) and color not in palette:
                palette.append(color)
    else:
        # If no black, start with the extracted colors
        palette = colors.copy()
    
    # Fill remaining slots with the first available color
    while len(palette) < 16:
        if palette:  # If we have colors, duplicate the first one
            palette.append(palette[0])
        else:  # Fallback: use black
            palette.append((0, 0, 0))
    
    return palette[:16]  # Ensure exactly 16 colors

def image_to_4bit_sprite_data(image, sprite_type, palette_colors):
    """Convert image to 4-bit sprite data using provided palette"""
    w, h = image.size
    
    # Process alpha channel first
    processed_image = process_image_with_alpha(image)
    
    # Find closest color in palette for each pixel
    rgb_pixels = list(processed_image.getdata())
    indexed_pixels = []
    
    for pixel in rgb_pixels:
        min_dist = float('inf')
        best_idx = 0
        for i, palette_color in enumerate(palette_colors):
            # Calculate color distance (RGB Euclidean distance)
            dist = sum((a - b) ** 2 for a, b in zip(pixel, palette_color))
            if dist < min_dist:
                min_dist = dist
                best_idx = i
        indexed_pixels.append(best_idx)
    
    # Convert to bitstream
    writer = BitWriter()
    
    if sprite_type == 'sprite':
        # Linear writing for sprites
        for pixel_idx in indexed_pixels:
            writer.write_bits(pixel_idx, 4)
    else:
        # Column-major writing for textures
        for x in range(w):
            for y in range(h):
                pixel_idx = indexed_pixels[y * w + x]
                writer.write_bits(pixel_idx, 4)
    
    return writer.get_bytes()

def create_default_palette():
    """Create a default 16-color palette with black at index 0"""
    palette = [
        (0, 0, 0),       # 0: Black (transparent)
        (255, 0, 0),     # 1: Red
        (0, 255, 0),     # 2: Green
        (0, 0, 255),     # 3: Blue
        (255, 255, 0),   # 4: Yellow
        (255, 0, 255),   # 5: Magenta
        (0, 255, 255),   # 6: Cyan
        (255, 255, 255), # 7: White
        (128, 0, 0),     # 8: Dark Red
        (0, 128, 0),     # 9: Dark Green
        (0, 0, 128),     # 10: Dark Blue
        (128, 128, 0),   # 11: Dark Yellow
        (128, 0, 128),   # 12: Dark Magenta
        (0, 128, 128),   # 13: Dark Cyan
        (128, 128, 128), # 14: Gray
        (64, 64, 64)     # 15: Dark Gray
    ]
    return palette

# ---------------------
# SP File Writer
# ---------------------
def write_sp_file(path, sprites, palettes, magic=0x9953):
    """Write sprites and palettes to SP file"""
    with open(path, 'wb') as f:
        f.write(write_u16_be(magic))
        f.write(write_u16_be(len(sprites)))
        f.write(write_u16_be(len(palettes)))
        f.write(b'\x00\x00\x00\x00')
        
        for sprite in sprites:
            f.write(bytes([sprite['raw_id'] & 0xFF]))
            f.write(write_u16_be(sprite['w']))
            f.write(write_u16_be(sprite['h']))
            f.write(write_u16_be(sprite['hoff']))
            f.write(write_u16_be(sprite['voff']))
            f.write(write_u16_be(sprite['pal_off']))
            f.write(write_u16_be(sprite['depth']))
            f.write(sprite['data'])
        
        for palette in palettes:
            f.write(write_u32_be(len(palette)))
            for color in palette:
                argb = (0xFF << 24) | (color[0] << 16) | (color[1] << 8) | color[2]
                f.write(write_u32_be(argb))

# ---------------------
# Enhanced GUI
# ---------------------
class SPEditorApp:
    def __init__(self, root):
        self.root = root
        root.title("SP Editor - Create/Edit SP Files")
        root.geometry("1400x900")

        self.sprites = []
        self.palettes = []
        self.image_refs = []
        self.selected_palette_index = 0
        self.current_file = None
        self.scale_factor = 1.0
        self.original_magic = 0x9953  # Store original file magic

        self.setup_ui()

    def setup_ui(self):
        # Top buttons
        top = Frame(self.root)
        top.pack(fill="x", padx=6, pady=6)
        
        Button(top, text="Open SP...", command=self.on_open).pack(side="left")
        Button(top, text="New SP", command=self.on_new_sp).pack(side="left", padx=6)
        Button(top, text="Save SP", command=self.on_save).pack(side="left")
        Button(top, text="Save SP As...", command=self.on_save_as).pack(side="left", padx=6)
        Button(top, text="Add Sprite", command=self.on_add_sprite).pack(side="left")
        Button(top, text="Replace Sprite", command=self.on_replace_sprite).pack(side="left", padx=6)
        Button(top, text="Export PNG", command=self.on_export).pack(side="left")
        
        self.file_label = Label(top, text="No file loaded")
        self.file_label.pack(side="left", padx=20)

        # Main content
        body = Frame(self.root)
        body.pack(fill="both", expand=True, padx=6, pady=6)

        # Left panel - sprite list
        left = Frame(body)
        left.pack(side="left", fill="y")
        
        Label(left, text="Sprites").pack(anchor="w")
        self.listbox = Listbox(left, width=40, height=25)
        self.listbox.pack(fill="y", expand=True)
        self.listbox.bind("<<ListboxSelect>>", lambda e: self.on_select())

        # Sprite list controls
        list_controls = Frame(left)
        list_controls.pack(fill="x", pady=(5, 0))
        
        Button(list_controls, text="Delete Sprite", command=self.on_delete_sprite).pack(side="left")
        Button(list_controls, text="Move Up", command=self.on_move_up).pack(side="left", padx=5)
        Button(list_controls, text="Move Down", command=self.on_move_down).pack(side="left")

        # Right panel - preview and info
        right = Frame(body)
        right.pack(side="left", fill="both", expand=True, padx=(10, 0))

        # Info text
        info_frame = Frame(right)
        info_frame.pack(fill="x", pady=(0, 10))
        
        Label(info_frame, text="Sprite Info").pack(anchor="w")
        self.info = Text(info_frame, height=8, wrap="word")
        self.info.pack(fill="x")

        # Preview area with controls
        preview_frame = Frame(right)
        preview_frame.pack(fill="both", expand=True)
        
        # Scale controls
        scale_frame = Frame(preview_frame)
        scale_frame.pack(fill="x", pady=(0, 5))
        
        Label(scale_frame, text="Scale:").pack(side="left")
        self.scale_var = DoubleVar(value=1.0)
        scale_slider = Scale(scale_frame, from_=0.1, to=5.0, resolution=0.1, 
                            orient=HORIZONTAL, variable=self.scale_var,
                            command=self.on_scale_change, length=200)
        scale_slider.pack(side="left", padx=5)
        Label(scale_frame, textvariable=self.scale_var).pack(side="left")
        Button(scale_frame, text="Fit", command=self.on_fit).pack(side="left", padx=10)
        Button(scale_frame, text="1:1", command=self.on_actual_size).pack(side="left")
        
        # Canvas with scrollbars
        canvas_frame = Frame(preview_frame)
        canvas_frame.pack(fill="both", expand=True)
        
        self.h_scrollbar = Scrollbar(canvas_frame, orient=HORIZONTAL)
        self.v_scrollbar = Scrollbar(canvas_frame, orient=VERTICAL)
        
        self.canvas = Canvas(canvas_frame, bg="black", 
                            xscrollcommand=self.h_scrollbar.set,
                            yscrollcommand=self.v_scrollbar.set)
        
        self.h_scrollbar.config(command=self.canvas.xview)
        self.v_scrollbar.config(command=self.canvas.yview)
        
        self.canvas.grid(row=0, column=0, sticky="nsew")
        self.h_scrollbar.grid(row=1, column=0, sticky="ew")
        self.v_scrollbar.grid(row=0, column=1, sticky="ns")
        
        canvas_frame.grid_rowconfigure(0, weight=1)
        canvas_frame.grid_columnconfigure(0, weight=1)

        # Palette controls
        palette_frame = Frame(right)
        palette_frame.pack(fill="x", pady=(10, 0))
        
        Label(palette_frame, text="Palette:").pack(side="left")
        
        self.palette_var = StringVar()
        self.palette_combo = ttk.Combobox(palette_frame, textvariable=self.palette_var, 
                                         state="readonly", width=20)
        self.palette_combo.pack(side="left", padx=(5, 10))
        self.palette_combo.bind('<<ComboboxSelected>>', self.on_palette_change)
        
        Button(palette_frame, text="Add Palette", command=self.on_add_palette).pack(side="left", padx=5)
        Button(palette_frame, text="Edit Palette", command=self.on_edit_palette).pack(side="left")
        
        # Palette preview
        Label(right, text="Palette Preview").pack(anchor="w", pady=(10, 5))
        self.palette_canvas = Canvas(right, width=256, height=50, bg="white")
        self.palette_canvas.pack(fill="x")

    def on_scale_change(self, value):
        self.scale_factor = float(value)
        self.update_preview()

    def on_fit(self):
        sel = self.listbox.curselection()
        if not sel:
            return
            
        idx = sel[0]
        sprite = self.sprites[idx]
        
        cw = max(1, self.canvas.winfo_width() or 400)
        ch = max(1, self.canvas.winfo_height() or 400)
        
        scale_x = cw / sprite['w']
        scale_y = ch / sprite['h']
        self.scale_factor = min(scale_x, scale_y, 5.0)
        
        self.scale_var.set(self.scale_factor)

    def on_actual_size(self):
        self.scale_var.set(1.0)

    def on_new_sp(self):
        """Create new empty SP file"""
        self.sprites = []
        self.palettes = [create_default_palette()]
        self.current_file = None
        self.original_magic = 0x9953
        self.listbox.delete(0, END)
        self.update_palette_combo()
        self.file_label.config(text="New SP file")

    def on_open(self):
        path = filedialog.askopenfilename(filetypes=[("SP files", "*.sp"), ("All files", "*.*")])
        if not path:
            return
        try:
            sprites, palettes = parse_sp_file(path)
            # Store original magic value
            with open(path, "rb") as f:
                buf = f.read()
            self.original_magic = read_u16_be(buf, 0)
        except Exception as ex:
            messagebox.showerror("Error", f"Failed to parse SP: {ex}")
            return

        self.current_file = path
        self.sprites = sprites
        self.palettes = palettes
        self.listbox.delete(0, END)
        
        for i, s in enumerate(sprites):
            sprite_type = "Sprite" if s['type'] == 'sprite' else "Texture"
            id_display = f"{s['raw_id']}" if s['raw_id'] >= 0 else f"{s['raw_id']}"
            self.listbox.insert(END, f"{i}: {sprite_type} ID={id_display} ({s['w']}x{s['h']})")

        self.update_palette_combo()
        
        if sprites:
            self.listbox.selection_set(0)
            self.on_select()
            
        self.file_label.config(text=f"File: {os.path.basename(path)}")

    def update_palette_combo(self):
        self.palette_combo['values'] = [f"Palette {i} ({len(self.palettes[i])} colors)" 
                                       for i in range(len(self.palettes))]
        if self.palettes:
            self.palette_combo.current(0)
            self.selected_palette_index = 0

    def on_palette_change(self, event):
        try:
            self.selected_palette_index = self.palette_combo.current()
            self.update_preview()
        except Exception as e:
            print(f"Error changing palette: {e}")

    def on_select(self):
        sel = self.listbox.curselection()
        if not sel:
            return
            
        idx = sel[0]
        sprite = self.sprites[idx]
        
        if 0 <= sprite['pal_off'] < len(self.palettes):
            self.palette_combo.current(sprite['pal_off'])
            self.selected_palette_index = sprite['pal_off']
        
        self.update_preview()

    def update_preview(self):
        sel = self.listbox.curselection()
        if not sel:
            self.canvas.delete("all")
            self.info.delete("1.0", END)
            return
            
        idx = sel[0]
        sprite = self.sprites[idx]
        
        info_lines = [
            f"Index: {idx}",
            f"Type: {'Sprite' if sprite['type'] == 'sprite' else 'Texture'}",
            f"Raw ID (signed): {sprite['raw_id']}",
            f"Size: {sprite['w']} x {sprite['h']}",
            f"Offsets: hoff={sprite['hoff']} voff={sprite['voff']}",
            f"Palette offset: {sprite['pal_off']}",
            f"Bit depth: {sprite['depth']} ({1 << sprite['depth']} colors)",
        ]
        self.info.delete("1.0", END)
        self.info.insert("1.0", "\n".join(info_lines))

        img = render_sprite_to_image(sprite, self.palettes, self.selected_palette_index)
        if img is None:
            self.canvas.delete("all")
            self.canvas.create_text(200, 200, text="Failed to render", fill="white")
            return

        new_w = int(sprite['w'] * self.scale_factor)
        new_h = int(sprite['h'] * self.scale_factor)
        
        if new_w <= 0: new_w = 1
        if new_h <= 0: new_h = 1
            
        disp = img.resize((new_w, new_h), Image.NEAREST)
        photo = ImageTk.PhotoImage(disp)
        
        self.image_refs.append(photo)
        self.canvas.delete("all")
        self.canvas.create_image(0, 0, anchor="nw", image=photo)
        
        self.canvas.config(scrollregion=self.canvas.bbox("all"))
        
        if len(self.image_refs) > 10:
            self.image_refs = self.image_refs[-10:]
            
        self.preview_palette()

    def preview_palette(self):
        self.palette_canvas.delete("all")
        if not self.palettes or self.selected_palette_index >= len(self.palettes):
            return
            
        palette = self.palettes[self.selected_palette_index]
        if not palette:
            return
            
        color_count = len(palette)
        show_count = min(color_count, 16)
        cell_width = 256 / show_count
        
        for i, color in enumerate(palette[:show_count]):
            r, g, b = color
            hex_color = f'#{r:02x}{g:02x}{b:02x}'
            x1 = i * cell_width
            x2 = (i + 1) * cell_width
            self.palette_canvas.create_rectangle(x1, 0, x2, 50, fill=hex_color, outline='')
            text_color = 'white' if (r + g + b) < 384 else 'black'
            self.palette_canvas.create_text(x1 + cell_width/2, 25, text=str(i), fill=text_color)
            
        # Highlight black color
        if (0, 0, 0) in palette[:show_count]:
            black_index = palette[:show_count].index((0, 0, 0))
            x1 = black_index * cell_width
            x2 = (black_index + 1) * cell_width
            self.palette_canvas.create_rectangle(x1, 0, x2, 50, outline='red', width=2)

    def on_add_sprite(self):
        """Add new sprite from image file with auto-generated palette"""
        path = filedialog.askopenfilename(
            filetypes=[("Image files", "*.png *.jpg *.jpeg *.bmp *.gif"), ("All files", "*.*")]
        )
        if not path:
            return
            
        try:
            new_img = Image.open(path)
        except Exception as e:
            messagebox.showerror("Error", f"Failed to open image: {e}")
            return
        
        # Ask for sprite type
        sprite_type = messagebox.askquestion("Sprite Type", "Is this a regular sprite?\n\nClick 'Yes' for Sprite, 'No' for Texture")
        sprite_type = 'sprite' if sprite_type == 'yes' else 'texture'
        
        # Ask for sprite ID
        id_str = simpledialog.askstring("Sprite ID", "Enter sprite ID (signed byte, -128 to 127):", initialvalue="0")
        if id_str is None:
            return
            
        try:
            sprite_id = int(id_str)
            if not (-128 <= sprite_id <= 127):
                raise ValueError("ID out of range")
        except ValueError:
            messagebox.showerror("Error", "Invalid sprite ID. Must be between -128 and 127.")
            return
        
        # Create optimized palette from image
        new_palette = create_palette_from_image(new_img)
        has_black = has_black_color(process_image_with_alpha(new_img))
        
        # Add new palette to palettes list
        palette_index = len(self.palettes)
        self.palettes.append(new_palette)
        
        # Convert image to 4-bit sprite data using the new palette
        sprite_data = image_to_4bit_sprite_data(new_img, sprite_type, new_palette)
        
        # Create new sprite
        new_sprite = {
            "raw_id": sprite_id,
            "id_unsigned": sprite_id & 0xFF,
            "w": new_img.width,
            "h": new_img.height,
            "hoff": 0,  # default offsets
            "voff": 0,
            "pal_off": palette_index,  # Use the new palette
            "depth": 4,  # Fixed 4-bit depth
            "data": sprite_data,
            "expected_bytes": len(sprite_data),
            "available_bytes": len(sprite_data),
            "type": sprite_type
        }
        
        self.sprites.append(new_sprite)
        
        # Update UI
        self.update_palette_combo()
        sprite_type_str = "Sprite" if sprite_type == 'sprite' else "Texture"
        self.listbox.insert(END, f"{len(self.sprites)-1}: {sprite_type_str} ID={sprite_id} ({new_img.width}x{new_img.height})")
        self.listbox.selection_clear(0, END)
        self.listbox.selection_set(END)
        self.on_select()
        
        black_status = "with black at index 0" if has_black else "without black (no black pixels found)"
        messagebox.showinfo("Success", 
            f"Sprite added successfully!\n"
            f"Size: {new_img.width}x{new_img.height}\n"
            f"4-bit depth, 16 colors\n"
            f"New palette created {black_status}\n"
            f"Alpha channel converted to black")

    def on_replace_sprite(self):
        """Replace selected sprite with new image of any resolution"""
        sel = self.listbox.curselection()
        if not sel:
            messagebox.showwarning("Replace", "Select a sprite first")
            return
            
        idx = sel[0]
        old_sprite = self.sprites[idx]
        
        path = filedialog.askopenfilename(
            filetypes=[("Image files", "*.png *.jpg *.jpeg *.bmp *.gif"), ("All files", "*.*")]
        )
        if not path:
            return
            
        try:
            new_img = Image.open(path)
        except Exception as e:
            messagebox.showerror("Error", f"Failed to open image: {e}")
            return
        
        # Ask user if they want to keep original sprite type or change it
        original_type = old_sprite['type']
        sprite_type = messagebox.askquestion("Sprite Type", 
            f"Original sprite type: {original_type}\n\nKeep original type?\n\nClick 'Yes' to keep '{original_type}', 'No' to change to {'texture' if original_type == 'sprite' else 'sprite'}")
        
        if sprite_type == 'yes':
            sprite_type = original_type
        else:
            sprite_type = 'texture' if original_type == 'sprite' else 'sprite'
        
        # Create new optimized palette from image
        new_palette = create_palette_from_image(new_img)
        has_black = has_black_color(process_image_with_alpha(new_img))
        
        # Update the existing palette or create new one
        palette_index = old_sprite['pal_off']
        if palette_index < len(self.palettes):
            self.palettes[palette_index] = new_palette
        else:
            self.palettes.append(new_palette)
            palette_index = len(self.palettes) - 1
        
        # Convert to 4-bit sprite data using the new palette
        new_data = image_to_4bit_sprite_data(new_img, sprite_type, new_palette)
        
        # Create completely new sprite with new dimensions but same ID and other parameters
        new_sprite = {
            "raw_id": old_sprite['raw_id'],  # Keep original ID
            "id_unsigned": old_sprite['id_unsigned'],
            "w": new_img.width,  # New width
            "h": new_img.height, # New height
            "hoff": old_sprite['hoff'],  # Keep original offsets
            "voff": old_sprite['voff'],
            "pal_off": palette_index,  # Updated palette index
            "depth": 4,  # Fixed 4-bit depth
            "data": new_data,
            "expected_bytes": len(new_data),
            "available_bytes": len(new_data),
            "type": sprite_type  # Updated sprite type
        }
        
        # Replace the sprite in the list
        self.sprites[idx] = new_sprite
        
        # Update UI
        self.update_palette_combo()
        self.refresh_sprite_list()
        self.listbox.selection_set(idx)
        self.on_select()
        
        black_status = "with black at index 0" if has_black else "without black (no black pixels found)"
        messagebox.showinfo("Success", 
            f"Sprite replaced successfully!\n"
            f"New size: {new_img.width}x{new_img.height} (was {old_sprite['w']}x{old_sprite['h']})\n"
            f"Type: {sprite_type}\n"
            f"New palette created {black_status}\n"
            f"Alpha channel converted to black")

    def on_delete_sprite(self):
        sel = self.listbox.curselection()
        if not sel:
            return
            
        idx = sel[0]
        if messagebox.askyesno("Confirm", f"Delete sprite {idx}?"):
            self.sprites.pop(idx)
            self.listbox.delete(0, END)
            for i, s in enumerate(self.sprites):
                sprite_type = "Sprite" if s['type'] == 'sprite' else "Texture"
                self.listbox.insert(END, f"{i}: {sprite_type} ID={s['raw_id']} ({s['w']}x{s['h']})")
            
            if self.sprites:
                self.listbox.selection_set(min(idx, len(self.sprites)-1))
                self.on_select()

    def on_move_up(self):
        sel = self.listbox.curselection()
        if not sel or sel[0] == 0:
            return
            
        idx = sel[0]
        self.sprites[idx], self.sprites[idx-1] = self.sprites[idx-1], self.sprites[idx]
        self.refresh_sprite_list()
        self.listbox.selection_set(idx-1)
        self.on_select()

    def on_move_down(self):
        sel = self.listbox.curselection()
        if not sel or sel[0] == len(self.sprites)-1:
            return
            
        idx = sel[0]
        self.sprites[idx], self.sprites[idx+1] = self.sprites[idx+1], self.sprites[idx]
        self.refresh_sprite_list()
        self.listbox.selection_set(idx+1)
        self.on_select()

    def refresh_sprite_list(self):
        self.listbox.delete(0, END)
        for i, s in enumerate(self.sprites):
            sprite_type = "Sprite" if s['type'] == 'sprite' else "Texture"
            self.listbox.insert(END, f"{i}: {sprite_type} ID={s['raw_id']} ({s['w']}x{s['h']})")

    def on_add_palette(self):
        """Add new palette"""
        new_palette = create_default_palette()
        self.palettes.append(new_palette)
        self.update_palette_combo()
        self.palette_combo.current(len(self.palettes)-1)
        messagebox.showinfo("Success", "New palette added")

    def on_edit_palette(self):
        """Simple palette editor"""
        if not self.palettes:
            return
            
        # For now, just show current palette info
        palette = self.palettes[self.selected_palette_index]
        info = f"Palette {self.selected_palette_index} has {len(palette)} colors\n\n"
        for i, color in enumerate(palette[:16]):
            info += f"{i}: RGB{color}\n"
        
        messagebox.showinfo("Palette Info", info)

    def on_export(self):
        sel = self.listbox.curselection()
        if not sel:
            messagebox.showwarning("Export", "Select a sprite first")
            return
        idx = sel[0]
        sprite = self.sprites[idx]
        img = render_sprite_to_image(sprite, self.palettes)
        if img is None:
            messagebox.showerror("Export", "Failed to render sprite")
            return
        path = filedialog.asksaveasfilename(
            defaultextension=".png", 
            filetypes=[("PNG","*.png")],
            initialfile=f"sprite_{idx:03d}_id{(sprite['raw_id'] & 0xFF):03d}.png"
        )
        if not path:
            return
        img.save(path)
        messagebox.showinfo("Export", f"Saved {path}")

    def on_save(self):
        if not self.current_file:
            self.on_save_as()
            return
            
        try:
            write_sp_file(self.current_file, self.sprites, self.palettes, self.original_magic)
            messagebox.showinfo("Success", f"SP file saved: {self.current_file}")
        except Exception as e:
            messagebox.showerror("Error", f"Failed to save SP file: {e}")

    def on_save_as(self):
        if not self.sprites:
            messagebox.showwarning("Save", "No sprites to save")
            return
            
        path = filedialog.asksaveasfilename(
            defaultextension=".sp",
            filetypes=[("SP files", "*.sp"), ("All files", "*.*")]
        )
        if not path:
            return
            
        self.current_file = path
        try:
            write_sp_file(path, self.sprites, self.palettes, self.original_magic)
            messagebox.showinfo("Success", f"SP file saved: {path}")
            self.file_label.config(text=f"File: {os.path.basename(path)}")
        except Exception as e:
            messagebox.showerror("Error", f"Failed to save SP file: {e}")

from tkinter import simpledialog

def main():
    root = Tk()
    app = SPEditorApp(root)
    root.mainloop()

if __name__ == "__main__":
    main()