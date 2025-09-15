from pathlib import Path

EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
labels = [
    "boulettes","dholl_puri","du_the","faratha","gato_piment",
    "jalebi","mine","pizza","riz_frite","samosa"
]

train = Path("data/train")
counts = []
for cls in labels:
    n = sum(1 for p in (train/cls).glob("*") if p.is_file() and p.suffix.lower() in EXTS)
    counts.append(n)

total = sum(counts)
C = len(labels)
weights = {i: round((total/(C*counts[i])), 3) for i in range(C)}
print("counts:", dict(zip(labels, counts)))
print("class_weight (index->weight):", weights)
