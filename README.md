# Calorie Counter (Android + On-device TFLite)

Offline-first Android MVP to estimate calories of Mauritian dishes:
- On-device TFLite int8 model
- Top-3 predictions with confirmation
- Portion presets + bias
- Local calories DB + food log

## Folders
- android/  → Android Studio project (Jetpack Compose)
- ml/       → Dataset prep, training, export (TFLite)

## Assets included
- android/app/src/main/assets/model/food_int8.tflite
- android/app/src/main/assets/labels.txt
- android/app/src/main/assets/calories.json

## Not included
- ml/data/raw, splits, and large training artifacts (see .gitignore).
