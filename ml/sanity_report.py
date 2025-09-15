import argparse
from pathlib import Path

EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}

def count_all(folder):
    return sum(1 for p in Path(folder).rglob("*") if p.is_file() and p.suffix.lower() in EXTS)

def per_class_counts(split_dir):
    counts = {}
    for d in sorted([p for p in Path(split_dir).iterdir() if p.is_dir()]):
        counts[d.name] = sum(1 for p in d.glob("*") if p.is_file() and p.suffix.lower() in EXTS)
    return counts

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data_root", required=True)
    a = ap.parse_args()
    root = Path(a.data_root)

    for s in ("raw","train","val","test"):
        d = root / s
        if not d.exists():
            print(f"\n=== {s.upper()} === (missing)"); continue
        print(f"\n=== {s.upper()} ===")
        print(f"Total images: {count_all(d)}")
        if s != "raw":
            for k, v in per_class_counts(d).items():
                print(f"{k}: {v}")
if __name__ == "__main__":
    main()
