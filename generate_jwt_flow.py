"""Generate a PNG diagram showing the JWT end-to-end flow."""
from PIL import Image, ImageDraw, ImageFont

W, H = 3200, 2400
BG = (255, 255, 255)
img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)


def load_font(size, bold=False):
    candidates = [
        "C:\\Windows\\Fonts\\segoeuib.ttf" if bold else "C:\\Windows\\Fonts\\segoeui.ttf",
        "C:\\Windows\\Fonts\\arialbd.ttf" if bold else "C:\\Windows\\Fonts\\arial.ttf",
    ]
    for c in candidates:
        try:
            return ImageFont.truetype(c, size)
        except Exception:
            continue
    return ImageFont.load_default()


F_TITLE = load_font(54, bold=True)
F_LANE = load_font(34, bold=True)
F_SCENARIO = load_font(40, bold=True)
F_LABEL = load_font(24)
F_LABEL_B = load_font(24, bold=True)
F_NOTE = load_font(22)

# Colors
C_FRONTEND = (52, 120, 246)     # blue
C_FILTER = (244, 130, 32)        # orange
C_SECURITY = (46, 161, 96)       # green
C_CONTROLLER = (155, 89, 182)    # purple
C_LIFELINE = (200, 200, 200)
C_ARROW_OK = (40, 40, 40)
C_ARROW_FAIL = (200, 50, 50)
C_TEXT = (30, 30, 30)
C_NOTE_BG = (255, 248, 220)
C_NOTE_BORDER = (220, 200, 100)
C_OK_BG = (230, 250, 235)
C_FAIL_BG = (252, 232, 232)
C_HEADER_BG = (245, 245, 250)

# ---- Title ----
d.text((W // 2 - 360, 30), "JWT Authentication — End-to-End Flow", fill=C_TEXT, font=F_TITLE)

# ---- Lane headers (4 actors) ----
LANE_Y = 130
LANE_H = 90
LANE_W = 600
LANE_GAP = 80
LANE_X0 = 200

actors = [
    ("Frontend (React)", C_FRONTEND),
    ("JwtFilter", C_FILTER),
    ("SecurityConfig", C_SECURITY),
    ("Controller", C_CONTROLLER),
]

lane_centers = []
for i, (name, color) in enumerate(actors):
    x = LANE_X0 + i * (LANE_W + LANE_GAP)
    d.rounded_rectangle([x, LANE_Y, x + LANE_W, LANE_Y + LANE_H], radius=14, fill=color)
    bbox = d.textbbox((0, 0), name, font=F_LANE)
    tw = bbox[2] - bbox[0]
    d.text((x + (LANE_W - tw) // 2, LANE_Y + 25), name, fill=(255, 255, 255), font=F_LANE)
    lane_centers.append(x + LANE_W // 2)

# ---- Lifelines (dashed vertical) ----
LIFE_TOP = LANE_Y + LANE_H
LIFE_BOTTOM = H - 60


def dashed_vline(x, y1, y2, color, dash=15, gap=10):
    y = y1
    while y < y2:
        d.line([(x, y), (x, min(y + dash, y2))], fill=color, width=3)
        y += dash + gap


for cx in lane_centers:
    dashed_vline(cx, LIFE_TOP, LIFE_BOTTOM, C_LIFELINE)


# ---- Helper: arrow ----
def arrow(x1, y, x2, color=C_ARROW_OK, label_top=None, label_bot=None, dashed=False, width=4):
    if dashed:
        # dashed horizontal
        step = 24
        cur = x1
        direction = 1 if x2 > x1 else -1
        while (direction == 1 and cur < x2 - 14) or (direction == -1 and cur > x2 + 14):
            nx = cur + direction * 14
            d.line([(cur, y), (nx, y)], fill=color, width=width)
            cur += direction * step
    else:
        d.line([(x1, y), (x2, y)], fill=color, width=width)
    # arrowhead
    head = 18
    if x2 > x1:
        d.polygon([(x2, y), (x2 - head, y - 9), (x2 - head, y + 9)], fill=color)
    else:
        d.polygon([(x2, y), (x2 + head, y - 9), (x2 + head, y + 9)], fill=color)
    # labels
    midx = (x1 + x2) // 2
    if label_top:
        bbox = d.textbbox((0, 0), label_top, font=F_LABEL_B)
        tw = bbox[2] - bbox[0]
        d.text((midx - tw // 2, y - 50), label_top, fill=color, font=F_LABEL_B)
    if label_bot:
        bbox = d.textbbox((0, 0), label_bot, font=F_LABEL)
        tw = bbox[2] - bbox[0]
        d.text((midx - tw // 2, y + 12), label_bot, fill=C_TEXT, font=F_LABEL)


def scenario_band(y_top, y_bot, color_bg, title, title_color):
    d.rounded_rectangle([100, y_top, W - 100, y_bot], radius=18, fill=color_bg, outline=title_color, width=3)
    d.rectangle([100, y_top, W - 100, y_top + 50], fill=title_color)
    d.rounded_rectangle([100, y_top, W - 100, y_top + 50], radius=18, fill=title_color)
    d.text((130, y_top + 6), title, fill=(255, 255, 255), font=F_SCENARIO)


# ===========================================================
# Scenario 1: Login flow
# ===========================================================
S1_TOP = LIFE_TOP + 30
S1_BOT = S1_TOP + 460
scenario_band(S1_TOP, S1_BOT, C_OK_BG, "  Step 1 — Login (public, no token needed)", (60, 130, 60))

y = S1_TOP + 110
# Frontend → Controller (skip filter — login is permitAll)
arrow(lane_centers[0], y, lane_centers[3],
      label_top="POST /login  { email, password }",
      label_bot="public endpoint — bypasses filter")

y += 110
# Controller does work (self-loop)
sx = lane_centers[3]
d.rectangle([sx - 220, y - 30, sx + 220, y + 50], fill=(255, 255, 255), outline=C_CONTROLLER, width=3)
d.text((sx - 200, y - 20), "userService.login(email, password)", fill=C_TEXT, font=F_LABEL)
d.text((sx - 200, y + 10), "jwtUtil.generateToken(...)", fill=C_TEXT, font=F_LABEL)

y += 130
# Controller → Frontend
arrow(lane_centers[3], y, lane_centers[0], color=(60, 130, 60),
      label_top='200 OK  { "token": "eyJ...", "user": {...} }',
      label_bot="frontend stores token in localStorage", dashed=True)

# ===========================================================
# Scenario 2: Authenticated request with valid token
# ===========================================================
S2_TOP = S1_BOT + 60
S2_BOT = S2_TOP + 700
scenario_band(S2_TOP, S2_BOT, C_OK_BG, "  Step 2 — Protected request with valid token", (60, 130, 60))

y = S2_TOP + 110
arrow(lane_centers[0], y, lane_centers[1],
      label_top="GET /orders?userid=1",
      label_bot="Authorization: Bearer eyJ...")

y += 110
# JwtFilter does work
sx = lane_centers[1]
d.rectangle([sx - 230, y - 50, sx + 230, y + 70], fill=(255, 255, 255), outline=C_FILTER, width=3)
d.text((sx - 210, y - 40), "jwtUtil.parse(token)", fill=C_TEXT, font=F_LABEL)
d.text((sx - 210, y - 10), "verify signature + expiry", fill=C_TEXT, font=F_LABEL)
d.text((sx - 210, y + 20), "extract roles from claims", fill=C_TEXT, font=F_LABEL)
d.text((sx - 210, y + 50), "SecurityContext.set(user)", fill=C_TEXT, font=F_LABEL)

y += 160
arrow(lane_centers[1], y, lane_centers[2],
      label_top="forward to security rules",
      label_bot="user X identified, roles=[customer]")

y += 110
# SecurityConfig check
sx = lane_centers[2]
d.rectangle([sx - 230, y - 30, sx + 230, y + 50], fill=(255, 255, 255), outline=C_SECURITY, width=3)
d.text((sx - 210, y - 20), ".anyRequest().authenticated()", fill=C_TEXT, font=F_LABEL)
d.text((sx - 210, y + 10), "→ ✓ allowed", fill=(60, 130, 60), font=F_LABEL_B)

y += 130
arrow(lane_centers[2], y, lane_centers[3],
      label_top="invoke handler")

y += 80
arrow(lane_centers[3], y, lane_centers[0], color=(60, 130, 60),
      label_top="200 OK  { orders: [...] }", dashed=True)

# ===========================================================
# Scenario 3: Request without token
# ===========================================================
S3_TOP = S2_BOT + 60
S3_BOT = S3_TOP + 380
scenario_band(S3_TOP, S3_BOT, C_FAIL_BG, "  Step 3 — Protected request WITHOUT token (or expired/invalid)", (180, 50, 50))

y = S3_TOP + 110
arrow(lane_centers[0], y, lane_centers[1],
      label_top="GET /orders   (no Authorization header)",
      label_bot="or token is expired/tampered")

y += 110
sx = lane_centers[1]
d.rectangle([sx - 230, y - 30, sx + 230, y + 50], fill=(255, 255, 255), outline=C_FILTER, width=3)
d.text((sx - 210, y - 20), "no Bearer header → skip", fill=C_TEXT, font=F_LABEL)
d.text((sx - 210, y + 10), "SecurityContext stays empty", fill=C_TEXT, font=F_LABEL)

y += 110
arrow(lane_centers[1], y, lane_centers[0], color=C_ARROW_FAIL,
      label_top="403 Forbidden", dashed=True)

# ===========================================================
# Footer note
# ===========================================================
NOTE_Y = S3_BOT + 30
d.rounded_rectangle([100, NOTE_Y, W - 100, NOTE_Y + 80], radius=12,
                     fill=C_NOTE_BG, outline=C_NOTE_BORDER, width=2)
d.text((130, NOTE_Y + 12),
       "Public endpoints (/login, /register, GET /products) skip the filter chain — they don't need a token.",
       fill=C_TEXT, font=F_NOTE)
d.text((130, NOTE_Y + 44),
       "All other endpoints require a valid JWT or return 401/403.",
       fill=C_TEXT, font=F_NOTE)

img.save("jwt_flow.png")
print("Saved jwt_flow.png")
