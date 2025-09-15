import argparse
from pathlib import Path
from PIL import Image, ImageStat

def average_hash(img, hash_size=8):
    g = img.convert("L").resize((hash_size, hash_size), Image.BILINEAR)
    pixels = list(g.getdata())
    avg = sum(pixels) / len(pixels)
    bits = ''.join('1' if p >= avg else '0' for p in pixels)
    return hex(int(bits, 2))[2:].rjust(hash_size*hash_size//4, '0')

def is_too_small(img, min_size=128):
    return img.width < min_size or img.height < min_size

def is_too_dark_or_bright(img, dark=5, bright=250):
    m = ImageStat.Stat(img.convert("L")).mean[0]
    return m <= dark or m >= bright

def collect_images(folder):
    exts = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
    for p in Path(folder).rglob("*"):
        if p.suffix.lower() in exts:
            yield p

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw_dir", required=True)
    ap.add_argument("--min_size", type=int, default=128)
    ap.add_argument("--out_bad", default="bad_images.txt")
    args = ap.parse_args()

    raw = Path(args.raw_dir)
    seen = {}
    bad = []
    removed_dupes = removed_bad = scanned = 0

    for cls_dir in sorted([d for d in raw.iterdir() if d.is_dir()]):
        print(f"[CLASS] {cls_dir.name}")
        for img_path in collect_images(cls_dir):
            scanned += 1
            try:
                with Image.open(img_path) as im:
                    im.load()
                    if is_too_small(im, args.min_size) or is_too_dark_or_bright(im):
                        bad.append(f"QUALITY\t{img_path}")
                        img_path.unlink(missing_ok=True); removed_bad += 1; continue
                    ah = average_hash(im, 8)
                    key = (cls_dir.name, ah)
                    if key in seen:
                        bad.append(f"DUPLICATE\t{img_path}\tDUPE_OF\t{seen[key]}")
                        img_path.unlink(missing_ok=True); removed_dupes += 1
                    else:
                        seen[key] = str(img_path)
            except Exception as e:
                bad.append(f"ERROR\t{img_path}\t{e}")
                try: img_path.unlink(missing_ok=True)
                except: pass
                removed_bad += 1

    Path(args.out_bad).write_text("\n".join(bad), encoding="utf-8")
    print(f"[DONE] Scanned: {scanned} | Removed bad: {removed_bad} | Removed dupes: {removed_dupes}")
    print(f"[LOG] bad details: {Path(args.out_bad).resolve()}")
if __name__ == "__main__":
    main()
