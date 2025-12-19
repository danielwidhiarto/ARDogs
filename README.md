# ARDogs 🐕

An Android application for real-time dog breed detection using AI/ML with augmented reality features. This app uses a custom-trained YOLO11n model to identify 15 popular dog breeds through your device's camera.

## 📱 Features

- **Real-time Detection**: Instant dog breed recognition using your camera
- **15 Breed Support**: Detects popular breeds including Golden Retriever, German Shepherd, Siberian Husky, and more
- **AR Overlay**: Visual bounding boxes showing detected dogs with confidence scores
- **Fun Facts**: Educational information about each detected breed that rotates every 5 seconds
- **High Performance**: Optimized ONNX Runtime implementation for fast inference
- **Modern UI**: Clean Material Design interface with dark mode support

## 🐶 Supported Dog Breeds

The model is trained to recognize the following 15 breeds:

1. Beagle
2. Chihuahua
3. Doberman
4. French Bulldog
5. German Shepherd
6. Golden Retriever
7. Labrador Retriever
8. Maltese Dog
9. Pomeranian
10. Pug
11. Rottweiler
12. Samoyed
13. Shih-Tzu
14. Siberian Husky
15. Standard Poodle

## 🚀 Technologies Used

- **Language**: Kotlin
- **ML Framework**: ONNX Runtime (v1.17.0)
- **Model**: YOLOv11n (custom-trained on dog breeds)
- **Camera**: AndroidX CameraX API
- **UI**: Material Design Components, ViewBinding
- **Async**: Kotlin Coroutines
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 36)

## 📦 Project Structure

```
app/
├── src/main/
│   ├── assets/
│   │   └── yolo11n_best.onnx        # Pre-trained YOLO model
│   ├── java/com/example/ardogs/
│   │   ├── MainActivity.kt           # Main app logic & camera handling
│   │   ├── DogDetector.kt           # ONNX model inference engine
│   │   ├── DogBreedInfo.kt          # Breed information & fun facts
│   │   ├── BoundingBoxOverlay.kt    # AR overlay for detection visualization
│   │   └── ImageUtils.kt            # Image preprocessing utilities
│   └── res/
│       └── layout/
│           └── activity_main.xml     # Main screen layout
```

## 🛠️ Installation & Setup

### Prerequisites

- Android Studio (latest version recommended)
- Android device or emulator with camera support
- Minimum Android 7.0 (API 24)

### Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/ARDogs.git
   cd ARDogs
   ```

2. **Open in Android Studio**

   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Build the project**

   - Let Gradle sync and download dependencies
   - Build > Make Project

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click Run (or press Shift+F10)
   - Grant camera permissions when prompted

## 🎯 How It Works

1. **Camera Feed**: The app uses CameraX API to capture real-time video frames
2. **Preprocessing**: Each frame is resized and normalized for model input (320x320)
3. **Inference**: The YOLO11n model processes the frame and outputs detection results
4. **Post-processing**: Non-Maximum Suppression (NMS) filters overlapping detections
5. **Visualization**: Bounding boxes and breed labels are drawn on the camera preview
6. **Info Display**: Fun facts about the detected breed are shown and rotate every 5 seconds

## ⚙️ Key Components

### DogDetector

- Loads and runs the ONNX model
- Handles image preprocessing (resize, normalization)
- Implements NMS algorithm for filtering detections
- Confidence threshold: 35%
- IoU threshold: 45%

### MainActivity

- Manages camera lifecycle
- Processes frames asynchronously using Coroutines
- Updates UI with detection results
- Handles camera permissions

### BoundingBoxOverlay

- Custom View for drawing AR overlays
- Displays bounding boxes, breed names, and confidence scores
- Scales coordinates to match screen dimensions

### DogBreedInfo

- Contains detailed information for each breed
- Provides fun facts with rotation mechanism
- Includes breed characteristics and origin

## 📊 Performance

- **Inference Time**: ~50-100ms per frame (device-dependent)
- **FPS**: ~10-20 frames per second
- **Model Size**: ~6MB (YOLO11n-nano variant)
- **Memory Usage**: Optimized for mobile devices

## 🔒 Permissions

The app requires the following permission:

- **CAMERA**: For real-time dog breed detection

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 Future Improvements

- [ ] Add more dog breeds to the model
- [ ] Implement breed history and detailed information pages
- [ ] Add ability to save and share detection results
- [ ] Support for photo gallery detection (not just camera)
- [ ] Multi-dog detection in single frame
- [ ] Add sound effects for detection
- [ ] Implement model quantization for better performance

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Author

Made with ❤️ for dog lovers everywhere

## 🙏 Acknowledgments

- YOLO (You Only Look Once) object detection framework
- ONNX Runtime for mobile ML inference
- AndroidX CameraX for modern camera implementation
- Material Design Components for beautiful UI

## 📞 Support

If you encounter any issues or have questions:

- Open an issue on GitHub
- Check existing issues for solutions
- Contact: [your-email@example.com]

---

**Note**: This app is for educational and entertainment purposes. Detection accuracy may vary based on lighting conditions, camera angle, and dog positioning.
