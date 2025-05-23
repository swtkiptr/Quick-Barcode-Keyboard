# Quick Barcode Keyboard

This Android application serves as a dual-purpose input method that combines a barcode scanner with a traditional keyboard. It allows you to scan barcodes and QR codes directly into any text field on your device.

## Features

- **Barcode & QR Code Scanning**: Quickly scan 1D and 2D barcodes (including QR, DataMatrix, etc.) directly into any text field
- **Integrated Keyboard**: Switch between scanner and traditional keyboard modes with a single tap
- **Privacy-Focused**: No internet permission required, ensuring your scanned data never leaves your device
- **Audio & Haptic Feedback**: Customizable beep sounds and vibration for successful scans
- **Flashlight Control**: Toggle the device flashlight for scanning in low-light conditions
- **Multiple Keyboard Layouts**: QWERTY and symbol layouts for versatile text input
- **Easy Setup**: Guided step-by-step setup process for first-time users

## How to Use

1. Install the app and follow the setup wizard to enable the keyboard and grant necessary permissions
2. Open any text field in any application
3. Select "Quick Barcode Keyboard" from your input method options
4. Scan a barcode or QR code, and the content will be automatically inserted
5. Switch to keyboard mode anytime by tapping the keyboard button

## Requirements

- Android 9.0 (API level 28) or higher
- Camera permission for barcode scanning
- Vibration permission for haptic feedback

## Technical Implementation

### Architecture
- Based on Android's `InputMethodService` to implement a custom keyboard
- Uses ZXing library for barcode scanning capabilities
- Implements a guided setup process using `ViewPager2` for onboarding

### Key Components
- **BarcodeInputService**: Main service that handles barcode scanning and keyboard functionality
- **CustomKeyboardView**: Implements the keyboard UI and input handling
- **ScannerIndicatorView**: Provides visual feedback during scanning
- **MainActivity**: Guides users through the setup process
- **SettingsActivity**: Allows users to configure scanning options

### Dependencies
- ZXing Android Embedded (v4.3.0) for barcode scanning
- AndroidX libraries for modern UI components
- Kotlin Coroutines for asynchronous operations

## Development

### Building the Project
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run on a device or emulator with API level 28+

### Project Structure
- `app/src/main/java/id/swtkiptr/keyboardscanner/` - Main source code
  - `keyboard/` - Custom keyboard implementation
  - `scanner/` - Barcode scanner implementation
- `app/src/main/res/` - Resource files
  - `layout/` - UI layouts
  - `xml/` - Input method and preferences configuration

## Privacy

This application:
- Does not request internet permission
- Keeps all scanned data on your device
- Requires only necessary permissions (camera and vibration)

## License

Copyright 2020 Raphael Michel. Apache License 2.0.
Based on dm77/barcodescanner-view and ZXing by Google (both Apache License 2.0).

## Updated and Enhanced by

Updated with additional features and modern UI design. Original source code available at https://github.com/raphaelm/android-barcode-keyboard
