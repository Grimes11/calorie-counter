import argparse, random, shutil
from pathlib import Path

def collect_images(folder):
    exts = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
    for p in Path(folder).glob("*"):
        if p.is_file() and p.suffix.lower() in exts:
            yield p

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw_dir", required=True)
    ap.add_argument("--out_root", required=True)
    ap.add_argument("--train", type=float, default=0.70)
    ap.add_argument("--val",   type=float, default=0.15)
    ap.add_argument("--test",  type=float, default=0.15)
    ap.add_argument("--seed", type=int, default=42)
    a = ap.parse_args()
    assert abs(a.train + a.val + a.test - 1.0) < 1e-6

    random.seed(a.seed)
    raw = Path(a.raw_dir)
    out = Path(a.out_root)
    for s in ("train","val","test"):
        (out / s).mkdir(parents=True, exist_ok=True)

    classes = sorted([d.name for d in raw.iterdir() if d.is_dir()])
    print("[CLASSES]", classes)

    for cls in classes:
        src = raw / cls
        imgs = list(collect_images(src))
        random.shuffle(imgs)
        n = len(imgs)
        n_train = int(n * a.train)
        n_val   = int(n * a.val)
        n_test  = n - n_train - n_val

        dst_train = out / "train" / cls; dst_train.mkdir(parents=True, exist_ok=True)
        dst_val   = out / "val"   / cls; dst_val.mkdir(parents=True, exist_ok=True)
        dst_test  = out / "test"  / cls; dst_test.mkdir(parents=True, exist_ok=True)

        for i, p in enumerate(imgs):
            if i < n_train: shutil.copy2(p, dst_train / p.name)
            elif i < n_train + n_val: shutil.copy2(p, dst_val / p.name)
            else: shutil.copy2(p, dst_test / p.name)
        print(f"[{cls}] total={n} train={n_train} val={n_val} test={n_test}")

    print("[DONE] Splitting complete.")
if __name__ == "__main__":
    main()
