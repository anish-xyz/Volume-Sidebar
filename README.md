# Volume Sidebar Overlay for Android

A lightweight Android accessibility application that places a floating, gesture-based volume handle on the side of your screen. It allows you to trigger the system volume panel from anywhere with a simple inward swipe gesture—preventing accidental screen touches underneath.

---

## Features

* **Custom Touch Interception Zone:** Prevents swipe gestures from bleeding through to underlying apps or the home screen.
* **Orientation Awareness:** Independently saves separate positions for **Portrait** and **Landscape** modes.
* **Notch & Cutout Support:** Extends flush to physical screen edges without getting restricted by status bars or camera punch-holes.
* **Gesture Priority:** Designed to prevent Android system back-gestures from interfering with volume triggers (especially in landscape mode).
* **Drag-to-Position:** Press and hold the sidebar to freely drag and position it anywhere on the left or right screen edge.
* **Full Customization Controls:**
  * Adjust touch buffer width and handle dimensions (height/width).
  * Customize RGB colors and transparency (Alpha) for both the handle and border.
  * Fine-tune border thickness and long-press hold duration.

---

## Installation & Setup

1. **Clone or Download** this repository into Android Studio.
2. Build and install the application onto your Android device (`Android 8.0 / API 26` or higher recommended).
3. **Grant Overlay Permission:** When prompted, grant the app permission to **Display over other apps** (System Alert Window).
4. Tap **Enable / Apply Settings** to start the foreground service.

---

## How to Use

* **Adjust Volume:** Swipe inward from the sidebar handle toward the center of your screen to bring up the device volume slider.
* **Move Sidebar:** Press and hold the sidebar for the configured duration (default: 2 seconds) until it dims, then drag it up or down. Drag it past the center of the screen to snap it to the opposite edge.
* **Customize Appearance:** Open the main app interface to adjust dimensions, border thickness, colors, and touch buffer zones using the sliders, then tap **Enable / Apply Settings** to apply your changes live.

---

## Built With

* **Language:** Kotlin
* **Framework:** Android SDK (WindowManager, Service, SharedPreferences)
* **Minimum SDK:** API 23 (Android 6.0)
