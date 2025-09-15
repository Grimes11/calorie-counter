import os, math, pathlib, json
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.applications.mobilenet_v2 import MobileNetV2, preprocess_input

# === Paths ===
ROOT = pathlib.Path(__file__).resolve().parent
DATA = ROOT / "data"
TRAIN = DATA / "train"
VAL   = DATA / "val"
TEST  = DATA / "test"
LABELS = [
    "boulettes","dholl_puri","du_the","faratha","gato_piment",
    "jalebi","mine","pizza","riz_frite","samosa"
]
NUM_CLASSES = len(LABELS)
IMG_SIZE = (224, 224)
BATCH = 32
EPOCHS = 10  # start small; you can increase after a smoke test

# === Class weights (computed from your split) ===
# weight = total_train / (num_classes * class_count)
class_weight = {
    0: 1.137,  # boulettes (217)
    1: 3.521,  # dholl_puri (70)
    2: 0.927,  # du_the (266)
    3: 0.865,  # faratha (285)
    4: 3.625,  # gato_piment (68)
    5: 1.208,  # jalebi (204)
    6: 0.391,  # mine (630)
    7: 1.361,  # pizza (181)
    8: 0.674,  # riz_frite (366)
    9: 1.385   # samosa (178)
}

# === Datasets ===
def make_ds(dirpath, shuffle=True):
    return tf.keras.utils.image_dataset_from_directory(
        dirpath,
        labels="inferred",
        label_mode="int",
        class_names=LABELS,
        image_size=IMG_SIZE,
        batch_size=BATCH,
        shuffle=shuffle
    )

train_ds = make_ds(TRAIN, shuffle=True)
val_ds   = make_ds(VAL, shuffle=False)
test_ds  = make_ds(TEST, shuffle=False)

# Cache + prefetch for speed
AUTOTUNE = tf.data.AUTOTUNE
def prep(ds, augment=False):
    def _pp(img, y):
        return preprocess_input(tf.cast(img, tf.float32)), y
    if augment:
        aug = keras.Sequential([
            layers.RandomFlip("horizontal"),
            layers.RandomZoom(0.1),
            layers.RandomBrightness(factor=0.1)
        ])
        ds = ds.map(lambda x,y: (aug(x, training=True), y), num_parallel_calls=AUTOTUNE)
    ds = ds.map(_pp, num_parallel_calls=AUTOTUNE).cache().prefetch(AUTOTUNE)
    return ds

train_ds = prep(train_ds, augment=True)
val_ds   = prep(val_ds)
test_ds  = prep(test_ds)

# === Model ===
base = MobileNetV2(input_shape=IMG_SIZE+(3,), include_top=False, weights="imagenet")
base.trainable = False  # freeze backbone for warmup

inputs = keras.Input(shape=IMG_SIZE+(3,))
x = inputs
x = base(x, training=False)
x = layers.GlobalAveragePooling2D()(x)
x = layers.Dropout(0.2)(x)
outputs = layers.Dense(NUM_CLASSES, activation="softmax")(x)
model = keras.Model(inputs, outputs)

# top-3 accuracy metric
top3 = keras.metrics.SparseTopKCategoricalAccuracy(k=3, name="top3")

model.compile(
    optimizer=keras.optimizers.Adam(1e-3),
    loss=keras.losses.SparseCategoricalCrossentropy(),
    metrics=[keras.metrics.SparseCategoricalAccuracy(name="top1"), top3]
)

# === Train (warmup) ===
history = model.fit(
    train_ds,
    validation_data=val_ds,
    epochs=max(2, EPOCHS//3),
    class_weight=class_weight
)

# === Fine-tune: unfreeze last ~30% of the backbone ===
for layer in base.layers[int(len(base.layers)*0.7):]:
    layer.trainable = True

model.compile(
    optimizer=keras.optimizers.Adam(1e-4),
    loss=keras.losses.SparseCategoricalCrossentropy(),
    metrics=[keras.metrics.SparseCategoricalAccuracy(name="top1"), top3]
)

history_ft = model.fit(
    train_ds,
    validation_data=val_ds,
    epochs=EPOCHS,
    class_weight=class_weight
)

# === Evaluate on test set ===
test_metrics = model.evaluate(test_ds, return_dict=True)
print("TEST:", test_metrics)

# === Save Keras + FP32 TFLite ===
OUT = ROOT / "out"
OUT.mkdir(parents=True, exist_ok=True)
keras_path = OUT / "food_fp32_keras.h5"
tflite_fp32_path = OUT / "food_fp32.tflite"
labels_path = OUT / "labels.txt"

model.save(keras_path)
labels_path.write_text("\n".join(LABELS), encoding="utf-8")

converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
tflite_fp32_path.write_bytes(tflite_model)

print(f"[SAVED] {keras_path}")
print(f"[SAVED] {tflite_fp32_path}")
print(f"[SAVED] {labels_path}")
