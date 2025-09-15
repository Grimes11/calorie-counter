import random, pathlib, numpy as np, tensorflow as tf
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent
OUT = ROOT / "out"
FP32 = OUT / "food_fp32_keras.h5"
INT8 = OUT / "food_int8.tflite"

IMG_SIZE = (224,224)

# Collect ~300 random training images as representative data
def rep_images():
    train_root = ROOT / "data" / "train"
    paths = []
    exts = {".jpg",".jpeg",".png",".bmp",".webp"}
    for cls in train_root.iterdir():
        if cls.is_dir():
            paths += [p for p in cls.glob("*") if p.suffix.lower() in exts]
    random.shuffle(paths)
    return paths[:300]

def rep_dataset():
    for p in rep_images():
        img = Image.open(p).convert("RGB").resize(IMG_SIZE)
        arr = np.array(img, dtype=np.float32)
        # MobileNetV2 preprocess_input: scale to [-1,1]
        arr = (arr/127.5) - 1.0
        arr = np.expand_dims(arr, axis=0)
        yield [arr]

model = tf.keras.models.load_model(FP32)

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = rep_dataset
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.uint8
converter.inference_output_type = tf.uint8

int8_model = converter.convert()
INT8.write_bytes(int8_model)
print(f"[SAVED] {INT8}")
