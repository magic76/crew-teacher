import math
from PIL import Image, ImageDraw

def create_cyber_orb_icon(size=512):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # 1. Rounded App Background Squircle (Navy Dark Slate Gradient)
    radius = int(size * 0.22)
    # Background base
    bg = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    bg_draw = ImageDraw.Draw(bg)
    
    for y in range(size):
        ratio = y / float(size)
        # Deep Indigo to Slate 950 gradient
        r = int(15 * (1 - ratio) + 2 * ratio)
        g = int(23 * (1 - ratio) + 6 * ratio)
        b = int(42 * (1 - ratio) + 23 * ratio)
        bg_draw.line([(0, y), (size, y)], fill=(r, g, b, 255))
    
    # Mask for squircle
    mask = Image.new('L', (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, size, size], radius=radius, fill=255)
    
    # Composite squircle
    app_icon = Image.composite(bg, Image.new('RGBA', (size, size), (0, 0, 0, 0)), mask)
    draw = ImageDraw.Draw(app_icon)
    
    # 2. Subtle border glow
    draw.rounded_rectangle([2, 2, size - 3, size - 3], radius=radius - 2, outline=(99, 102, 241, 100), width=int(size * 0.015))
    
    # Center & Dimensions
    cx, cy = size / 2.0, size / 2.0
    outer_r = size * 0.38
    inner_r = size * 0.27
    
    # 3. Outer Orbit Ring with glowing dots / dashes
    num_steps = 180
    for i in range(num_steps):
        angle = 2 * math.pi * (i / float(num_steps))
        deg = math.degrees(angle)
        # Dash effect: draw 3/4 of circle
        if int(deg / 15) % 2 == 0:
            px = cx + outer_r * math.cos(angle)
            py = cy + outer_r * math.sin(angle)
            
            # Gradient color around circle (Cyan -> Indigo -> Purple -> Cyan)
            t = (angle % (2 * math.pi)) / (2 * math.pi)
            if t < 0.33:
                k = t / 0.33
                cr, cg, cb = int(56*(1-k) + 129*k), int(189*(1-k) + 140*k), int(248*(1-k) + 248*k)
            elif t < 0.66:
                k = (t - 0.33) / 0.33
                cr, cg, cb = int(129*(1-k) + 192*k), int(140*(1-k) + 132*k), int(248*(1-k) + 252*k)
            else:
                k = (t - 0.66) / 0.34
                cr, cg, cb = int(192*(1-k) + 56*k), int(132*(1-k) + 189*k), int(252*(1-k) + 248*k)
                
            dot_r = size * 0.018
            draw.ellipse([px - dot_r, py - dot_r, px + dot_r, py + dot_r], fill=(cr, cg, cb, 230))
            
    # 4. Central Orb (Glossy Glass Dark Purple Sphere)
    orb_box = [cx - inner_r, cy - inner_r, cx + inner_r, cy + inner_r]
    draw.ellipse(orb_box, fill=(30, 27, 75, 230), outline=(129, 140, 248, 220), width=int(size * 0.015))
    
    # Inner subtle glow
    glow_r = inner_r * 0.85
    draw.ellipse([cx - glow_r, cy - glow_r * 1.1, cx + glow_r, cy + glow_r * 0.7], fill=(56, 189, 248, 40))
    
    # 5. Soundwave Bars in Center (Dynamic Vibrant Audio Spectrum)
    # Bars layout: 4 vertical rounded rects
    bar_width = size * 0.045
    bar_spacing = size * 0.075
    heights = [size * 0.16, size * 0.32, size * 0.44, size * 0.18]
    colors = [
        (56, 189, 248, 255),   # Cyan
        (129, 140, 248, 255),  # Indigo
        (192, 132, 252, 255),  # Lavender Purple
        (56, 189, 248, 255)    # Cyan
    ]
    
    start_x = cx - (len(heights) - 1) * bar_spacing / 2.0
    for idx, h in enumerate(heights):
        bx = start_x + idx * bar_spacing
        top = cy - h / 2.0
        bot = cy + h / 2.0
        # Draw shadow
        draw.rounded_rectangle([bx - bar_width/2, top, bx + bar_width/2, bot], radius=bar_width/2, fill=colors[idx])
        # Add highlight on bar top
        draw.ellipse([bx - bar_width/2 + 2, top + 2, bx + bar_width/2 - 2, top + bar_width - 2], fill=(255, 255, 255, 180))
        
    return app_icon

icon_512 = create_cyber_orb_icon(512)
icon_512.save("/data/data/com.termux/files/home/crew-teacher/assets/icon.png")

# Also generate Android mipmap densities
densities = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

for folder, s in densities.items():
    resized = icon_512.resize((s, s), Image.Resampling.LANCZOS)
    resized.save(f"/data/data/com.termux/files/home/crew-teacher/app/src/main/res/{folder}/ic_launcher.png")
    resized.save(f"/data/data/com.termux/files/home/crew-teacher/app/src/main/res/{folder}/ic_launcher_round.png")

print("Generated all icon sizes successfully!")
