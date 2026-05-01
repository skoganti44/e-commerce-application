"""Generate a CLEAN, READABLE ER diagram PNG for the e-commerce database."""
from PIL import Image, ImageDraw, ImageFont

# Larger canvas + bigger fonts for clarity
W, H = 3200, 2000
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

F_TITLE  = load_font(56, bold=True)
F_GROUP  = load_font(30, bold=True)
F_TBL    = load_font(32, bold=True)   # table name BIG
F_COL    = load_font(22)              # column text
F_CARD   = load_font(28, bold=True)   # cardinality 1/* BIG
F_LEGEND = load_font(24)

COLORS = {
    "auth":     {"hdr": (60, 90, 160),   "bg": (228, 235, 250)},
    "catalog":  {"hdr": (40, 130, 80),   "bg": (225, 245, 230)},
    "cart":     {"hdr": (200, 130, 30),  "bg": (252, 240, 215)},
    "order":    {"hdr": (170, 60, 60),   "bg": (250, 225, 225)},
    "delivery": {"hdr": (130, 70, 160),  "bg": (240, 228, 250)},
    "ops":      {"hdr": (90, 90, 90),    "bg": (235, 235, 235)},
    "kitchen":  {"hdr": (170, 100, 40),  "bg": (250, 235, 215)},
}

TBL_W   = 360       # bigger boxes
HDR_H   = 60
ROW_H   = 30
PAD_BTM = 12

# Keep ONLY the most important fields so each box stays readable
tables = {
    # Row 1 — Identity (top)
    "Role":               {"xy": (180,  170), "g": "auth",     "f": ["id (PK)", "role", "department"]},
    "userRole":           {"xy": (640,  170), "g": "auth",     "f": ["userroleid (PK)", "userid (FK)", "roleid (FK)"]},
    "User":               {"xy": (1100, 170), "g": "auth",     "f": ["userid (PK)", "name", "email", "password"]},

    # Row 2 — Catalog
    "Categories":         {"xy": (180,  580), "g": "catalog",  "f": ["id (PK)", "name", "description"]},
    "Product_Images":     {"xy": (640,  580), "g": "catalog",  "f": ["id (PK)", "product_id (FK)", "imageUrl"]},
    "Products":           {"xy": (1100, 580), "g": "catalog",  "f": ["id (PK)", "name", "price", "stock", "category_id (FK)"]},
    "daily_stock":        {"xy": (1560, 580), "g": "kitchen",  "f": ["id (PK)", "product_id (FK)", "targetCount", "preparedCount"]},

    # Right column — Orders area
    "Orders":             {"xy": (2050, 580), "g": "order",    "f": ["id (PK)", "user_id (FK)", "totalAmount", "status", "approved_by_user_id (FK)"]},
    "payments":           {"xy": (2510, 580), "g": "order",    "f": ["id (PK)", "order_id (FK)", "paymentMethod", "amount"]},
    "delivery_trips":     {"xy": (2510, 1010), "g": "delivery","f": ["id (PK)", "order_id (FK)", "driver_user_id (FK)", "status", "otpCode"]},

    # Row 3 — Cart + Order details
    "Cart":               {"xy": (180,  1010), "g": "cart",    "f": ["id (PK)", "user_id (FK)"]},
    "cart_items":         {"xy": (640,  1010), "g": "cart",    "f": ["id (PK)", "cart_id (FK)", "product_id (FK)", "quantity"]},
    "order_items":        {"xy": (1560, 1010), "g": "order",   "f": ["id (PK)", "order_id (FK)", "product_id (FK)", "quantity", "price"]},
    "shipping_addresses": {"xy": (2050, 1010), "g": "order",   "f": ["id (PK)", "order_id (FK)", "user_id (FK)", "city", "pincode"]},

    # Row 4 — Bottom
    "tasks":              {"xy": (180,  1500), "g": "ops",     "f": ["id (PK)", "created_by_user_id (FK)", "assigned_user_id (FK)", "status"]},
    "refund_requests":    {"xy": (640,  1500), "g": "ops",     "f": ["id (PK)", "order_id (FK)", "raised_by_user_id (FK)", "amount"]},
    "discount_campaigns": {"xy": (1100, 1500), "g": "ops",     "f": ["id (PK)", "proposed_by_user_id (FK)", "discountPercent"]},
    "supplies":           {"xy": (1560, 1500), "g": "kitchen", "f": ["id (PK)", "name", "currentStock", "threshold"]},
    "Product_Available":  {"xy": (2050, 1500), "g": "catalog", "f": ["id (PK)", "name", "price", "category_id (FK)"]},
    "delivery_issues":    {"xy": (2510, 1500), "g": "delivery","f": ["id (PK)", "trip_id (FK)", "driver_user_id (FK)", "issueType"]},
}

def box_h(t):
    return HDR_H + ROW_H * len(t["f"]) + PAD_BTM

def rect(name):
    t = tables[name]; x, y = t["xy"]
    return (x, y, x + TBL_W, y + box_h(t))

def draw_table(name):
    t = tables[name]; c = COLORS[t["g"]]
    x1, y1, x2, y2 = rect(name)
    d.rectangle([x1, y1, x2, y2], fill=c["bg"], outline=(30, 30, 30), width=3)
    d.rectangle([x1, y1, x2, y1 + HDR_H], fill=c["hdr"], outline=(30, 30, 30), width=3)
    bbox = d.textbbox((0, 0), name, font=F_TBL)
    tw = bbox[2] - bbox[0]
    d.text((x1 + (TBL_W - tw)/2, y1 + 12), name, fill=(255, 255, 255), font=F_TBL)
    for i, f in enumerate(t["f"]):
        fy = y1 + HDR_H + 6 + i * ROW_H
        d.text((x1 + 16, fy), f, fill=(20, 20, 20), font=F_COL)

def anchor(name, side):
    x1, y1, x2, y2 = rect(name)
    cx = (x1 + x2) // 2; cy = (y1 + y2) // 2
    return {"L":(x1,cy),"R":(x2,cy),"T":(cx,y1),"B":(cx,y2)}[side]

def connect(a, asd, b, bsd, ac="1", bc="*", color=(60,60,60), routing="auto", width=3):
    ax, ay = anchor(a, asd); bx, by = anchor(b, bsd)
    if routing == "auto":
        if asd in ("L","R") and bsd in ("L","R"):
            mx = (ax + bx) // 2
            pts = [(ax,ay),(mx,ay),(mx,by),(bx,by)]
        elif asd in ("T","B") and bsd in ("T","B"):
            my = (ay + by) // 2
            pts = [(ax,ay),(ax,my),(bx,my),(bx,by)]
        else:
            pts = [(ax,ay),(bx,ay),(bx,by)] if asd in ("L","R") else [(ax,ay),(ax,by),(bx,by)]
    else:
        pts = routing
    for i in range(len(pts)-1):
        d.line([pts[i], pts[i+1]], fill=color, width=width)

    def card_label(text, near, away):
        nx, ny = near; ax_, ay_ = away
        dx = -1 if ax_ > nx else (1 if ax_ < nx else 0)
        dy = -1 if ay_ > ny else (1 if ay_ < ny else 0)
        ox, oy = nx + dx * 24, ny + dy * 24
        bbox = d.textbbox((ox, oy), text, font=F_CARD)
        pad = 6
        d.rectangle([bbox[0]-pad, bbox[1]-pad, bbox[2]+pad, bbox[3]+pad],
                    fill=(255,255,255), outline=color, width=2)
        d.text((ox, oy), text, fill=color, font=F_CARD)

    card_label(ac, pts[0], pts[1])
    card_label(bc, pts[-1], pts[-2])

# Title
d.text((W/2 - 700, 30),
       "E-Commerce Database — Entity Relationship Diagram",
       fill=(20, 20, 70), font=F_TITLE)

# Group banners (above each row)
def banner(text, x, y, color, w=380):
    d.rectangle([x, y, x + w, y + 44], fill=color)
    d.text((x + 16, y + 8), text, fill=(255,255,255), font=F_GROUP)

banner("IDENTITY & ACCESS", 180,  115, COLORS["auth"]["hdr"])
banner("CATALOG",           180,  525, COLORS["catalog"]["hdr"])
banner("ORDERS & PAYMENTS", 2050, 525, COLORS["order"]["hdr"], w=440)
banner("CART",              180,  955, COLORS["cart"]["hdr"])
banner("OPERATIONS / STANDALONE", 180, 1445, COLORS["ops"]["hdr"], w=560)

# Draw boxes
for n in tables:
    draw_table(n)

# Relationships
# Identity
connect("Role", "R", "userRole", "L", "1", "*", color=(60,90,160))
connect("userRole", "R", "User", "L", "*", "1", color=(60,90,160))

# User -> Cart, Orders, Tasks, refund_requests, discount_campaigns, delivery_trips (driver)
connect("User", "B", "Cart", "T", "1", "*",
        routing=[anchor("User","B"), (anchor("User","B")[0], 470),
                 (anchor("Cart","T")[0], 470), anchor("Cart","T")],
        color=(60,90,160))
connect("User", "B", "Orders", "T", "1", "*",
        routing=[(anchor("User","B")[0]+40, anchor("User","B")[1]),
                 (anchor("User","B")[0]+40, 490),
                 (anchor("Orders","T")[0], 490),
                 anchor("Orders","T")],
        color=(170,60,60))
connect("User", "B", "tasks", "T", "1", "*",
        routing=[(anchor("User","B")[0]-40, anchor("User","B")[1]),
                 (anchor("User","B")[0]-40, 1410),
                 (anchor("tasks","T")[0], 1410),
                 anchor("tasks","T")],
        color=(60,90,160))
connect("User", "R", "delivery_trips", "T", "1", "*",
        routing=[anchor("User","R"),
                 (2890, anchor("User","R")[1]),
                 (2890, 950),
                 (anchor("delivery_trips","T")[0]+30, 950),
                 (anchor("delivery_trips","T")[0]+30, anchor("delivery_trips","T")[1])],
        color=(130,70,160))

# Catalog
connect("Categories", "R", "Products", "L", "1", "*",
        routing=[anchor("Categories","R"),
                 (anchor("Categories","R")[0]+30, anchor("Categories","R")[1]),
                 (anchor("Categories","R")[0]+30, anchor("Products","L")[1]),
                 anchor("Products","L")],
        color=(40,130,80))
connect("Products", "L", "Product_Images", "R", "1", "*", color=(40,130,80))
connect("Products", "R", "daily_stock", "L", "1", "*", color=(40,130,80))

# Cart
connect("Cart", "R", "cart_items", "L", "1", "*", color=(200,130,30))
connect("Products", "B", "cart_items", "T", "1", "*",
        routing=[anchor("Products","B"),
                 (anchor("Products","B")[0], 940),
                 (anchor("cart_items","T")[0], 940),
                 anchor("cart_items","T")],
        color=(40,130,80))

# Orders
connect("Orders", "R", "payments", "L", "1", "*", color=(170,60,60))
connect("Orders", "B", "order_items", "T", "1", "*",
        routing=[(anchor("Orders","B")[0]-30, anchor("Orders","B")[1]),
                 (anchor("Orders","B")[0]-30, 950),
                 (anchor("order_items","T")[0], 950),
                 anchor("order_items","T")],
        color=(170,60,60))
connect("Orders", "B", "shipping_addresses", "T", "1", "*", color=(170,60,60))
connect("Orders", "B", "delivery_trips", "T", "1", "*",
        routing=[(anchor("Orders","B")[0]+50, anchor("Orders","B")[1]),
                 (anchor("Orders","B")[0]+50, 950),
                 (anchor("delivery_trips","T")[0]-30, 950),
                 (anchor("delivery_trips","T")[0]-30, anchor("delivery_trips","T")[1])],
        color=(130,70,160))
connect("Products", "B", "order_items", "T", "1", "*",
        routing=[(anchor("Products","B")[0]+30, anchor("Products","B")[1]),
                 (anchor("Products","B")[0]+30, 920),
                 (anchor("order_items","T")[0]+30, 920),
                 (anchor("order_items","T")[0]+30, anchor("order_items","T")[1])],
        color=(40,130,80))

# Delivery
connect("delivery_trips", "B", "delivery_issues", "T", "1", "*", color=(130,70,160))

# Orders -> refund_requests
connect("Orders", "B", "refund_requests", "T", "1", "*",
        routing=[(anchor("Orders","B")[0]-80, anchor("Orders","B")[1]),
                 (anchor("Orders","B")[0]-80, 1430),
                 (anchor("refund_requests","T")[0], 1430),
                 anchor("refund_requests","T")],
        color=(170,60,60))

# ---------- Legend ----------
lx, ly = 2050, 1750
d.rectangle([lx, ly, lx + 1100, ly + 230], outline=(30,30,30), width=3, fill=(250,250,250))
d.text((lx + 18, ly + 12), "LEGEND", fill=(20,20,20), font=F_GROUP)
d.text((lx + 18, ly + 60), "PK = Primary Key    FK = Foreign Key", fill=(20,20,20), font=F_LEGEND)
d.text((lx + 18, ly + 92), "1 ── *   one-to-many relationship", fill=(20,20,20), font=F_LEGEND)
d.text((lx + 18, ly + 124), "Box colour = functional group", fill=(20,20,20), font=F_LEGEND)

# Group swatches (3 cols)
groups = [("Identity","auth"),("Catalog","catalog"),("Cart","cart"),
          ("Orders","order"),("Delivery","delivery"),("Operations","ops"),("Kitchen","kitchen")]
for i,(lab,key) in enumerate(groups):
    sx = lx + 18 + (i % 3) * 360
    sy = ly + 158 + (i // 3) * 32
    d.rectangle([sx, sy, sx + 28, sy + 24], fill=COLORS[key]["hdr"], outline=(30,30,30), width=2)
    d.text((sx + 38, sy + 1), lab, fill=(20,20,20), font=F_LEGEND)

out = r"d:\Sonika\DA udemy\JAVA\e-commerce application\er_diagram.png"
img.save(out, "PNG")
print("Saved:", out, "Size:", img.size)
