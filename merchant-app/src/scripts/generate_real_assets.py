import os
import glob
import json
import shutil
import pypdf
from PIL import Image
import io

D4D_DIR = r"c:\Reji\Android Apps\Deal Finder\D4D"
ASSETS_DIR = r"c:\Reji\Android Apps\Deal Finder\user-app\app\src\main\assets"
PRODUCTS_ASSET_DIR = os.path.join(ASSETS_DIR, "products")
FLYERS_ASSET_DIR = os.path.join(ASSETS_DIR, "flyers")
LOGOS_ASSET_DIR = os.path.join(ASSETS_DIR, "logos")

os.makedirs(PRODUCTS_ASSET_DIR, exist_ok=True)
os.makedirs(FLYERS_ASSET_DIR, exist_ok=True)
os.makedirs(LOGOS_ASSET_DIR, exist_ok=True)

# 1. Copy Logos
print("=== Step 1: Copying Logos ===")
shop_dirs = [d for d in os.listdir(D4D_DIR) if os.path.isdir(os.path.join(D4D_DIR, d))]
logo_count = 0
for s in shop_dirs:
    s_path = os.path.join(D4D_DIR, s)
    logo_file = None
    for ext in [".png", ".jpg", ".jpeg", ".webp"]:
        f = os.path.join(s_path, f"logo{ext}")
        if os.path.exists(f):
            logo_file = f
            break
    if logo_file:
        dest = os.path.join(LOGOS_ASSET_DIR, f"{s}.png")
        try:
            with Image.open(logo_file) as im:
                im.convert("RGBA").save(dest, "PNG")
            logo_count += 1
        except Exception as e:
            print(f"Error converting logo for {s}: {e}")

print(f"Successfully processed {logo_count} retailer logos into {LOGOS_ASSET_DIR}")

# 2. Extract Flyer Cover Pages & Product Crops from PDFs
print("\n=== Step 2: Extracting Flyer Cover Pages & Product Images from PDFs ===")
with open(os.path.join(D4D_DIR, "catalogues.json"), "r", encoding="utf-8") as f:
    catalogues = json.load(f)

print(f"Total catalogues to inspect: {len(catalogues)}")

# Helper to crop product panels from a page image
# Typical flyer page layout has 2 columns x 2 rows or 2 columns x 3 rows of products
def extract_product_crops(img, prefix, max_crops=6):
    w, h = img.size
    crops = []
    # Grid 2 cols x 3 rows (ignoring header banner ~12% top, footer ~8% bottom)
    top_margin = int(h * 0.12)
    bot_margin = int(h * 0.92)
    eff_h = bot_margin - top_margin
    eff_w = w
    
    rows = 3
    cols = 2
    row_h = eff_h // rows
    col_w = eff_w // cols
    
    crop_idx = 0
    for r in range(rows):
        for c in range(cols):
            if crop_idx >= max_crops:
                break
            box = (
                c * col_w + int(col_w * 0.05),
                top_margin + r * row_h + int(row_h * 0.05),
                (c + 1) * col_w - int(col_w * 0.05),
                top_margin + (r + 1) * row_h - int(row_h * 0.05)
            )
            cropped = img.crop(box)
            crops.append((f"{prefix}_crop{crop_idx}.jpg", cropped))
            crop_idx += 1
    return crops

flyer_extracted = 0
product_crops_saved = 0
retailer_crops_map = {}  # retailer_id -> list of crop file paths

for cat in catalogues:
    flyer_id = str(cat.get("flyer_id"))
    retailer_id = cat.get("shop_id")
    pdf_rel = cat.get("pdf_path")
    if not pdf_rel:
        continue
    pdf_path = os.path.join(D4D_DIR, pdf_rel)
    if not os.path.exists(pdf_path):
        continue
    
    try:
        reader = pypdf.PdfReader(pdf_path)
        if len(reader.pages) == 0:
            continue
        
        # Extract page 0 image as flyer cover
        page0 = reader.pages[0]
        if len(page0.images) > 0:
            img_data = page0.images[0].data
            with Image.open(io.BytesIO(img_data)) as cover_img:
                cover_dest = os.path.join(FLYERS_ASSET_DIR, f"{flyer_id}.jpg")
                cover_img.convert("RGB").save(cover_dest, "JPEG", quality=85)
                flyer_extracted += 1
                
                # Extract product crops from page 0 and page 1 if available
                crops = extract_product_crops(cover_img, f"{retailer_id}_{flyer_id}_p0", max_crops=4)
                for crop_name, crop_img in crops:
                    crop_dest = os.path.join(PRODUCTS_ASSET_DIR, crop_name)
                    crop_img.convert("RGB").save(crop_dest, "JPEG", quality=85)
                    product_crops_saved += 1
                    retailer_crops_map.setdefault(retailer_id, []).append(crop_dest)
        
        if len(reader.pages) > 1:
            page1 = reader.pages[1]
            if len(page1.images) > 0:
                img_data = page1.images[0].data
                with Image.open(io.BytesIO(img_data)) as p1_img:
                    crops = extract_product_crops(p1_img, f"{retailer_id}_{flyer_id}_p1", max_crops=4)
                    for crop_name, crop_img in crops:
                        crop_dest = os.path.join(PRODUCTS_ASSET_DIR, crop_name)
                        crop_img.convert("RGB").save(crop_dest, "JPEG", quality=85)
                        product_crops_saved += 1
                        retailer_crops_map.setdefault(retailer_id, []).append(crop_dest)

    except Exception as e:
        # Ignore individual pdf parse issues
        pass

print(f"Extracted {flyer_extracted} flyer cover images into {FLYERS_ASSET_DIR}")
print(f"Extracted {product_crops_saved} product deal crops into {PRODUCTS_ASSET_DIR}")

# 3. Create Specific Master Product Images for the 12 master products
print("\n=== Step 3: Preparing Master Product Images ===")
# List of 12 master products
master_products = [
    "master-coke-6x330ml",
    "master-almarai-milk-2l",
    "master-alrawdah-chicken-1kg",
    "master-sunwhite-rice-5kg",
    "master-tide-powder-5kg",
    "master-nutella-750g",
    "master-dettol-liquid-2x1l",
    "master-lipton-tea-100s",
    "master-fresh-eggs-30s",
    "master-iphone-15-128gb",
    "master-samsung-tv-55-4k",
    "master-noor-oil-15l"
]

# For each master product, find best matching real flyer crops from relevant retailers
all_crops = glob.glob(os.path.join(PRODUCTS_ASSET_DIR, "*.jpg"))
print(f"Available crops pool: {len(all_crops)}")

# Create default fallback deal image
if all_crops:
    default_deal_dest = os.path.join(PRODUCTS_ASSET_DIR, "default_deal.jpg")
    shutil.copy(all_crops[0], default_deal_dest)
    print("Created default_deal.jpg fallback")

# For each master product and retailer combination, assign a real crop
for idx, m_id in enumerate(master_products):
    # Base master image
    m_dest = os.path.join(PRODUCTS_ASSET_DIR, f"{m_id}.jpg")
    crop_source = all_crops[idx % len(all_crops)]
    shutil.copy(crop_source, m_dest)

print("Master product images generated.")
