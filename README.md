# 🚨 Silent Guardian - Emergency SOS App

<div align="center">

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Android Studio](https://img.shields.io/badge/Android_Studio-3DDC84?style=for-the-badge&logo=android-studio&logoColor=white)](https://developer.android.com/studio)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen?style=for-the-badge)](https://developer.android.com/about/versions/android-5.0)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/yourusername/silent-guardian-sos-app?style=for-the-badge&logo=github)](https://github.com/yourusername/silent-guardian-sos-app/stargazers)
[![GitHub Issues](https://img.shields.io/github/issues/sajid-android-dev/silent-guardian-sos-app?style=for-the-badge&logo=github)](https://github.com/yourusername/silent-guardian-sos-app/issues)

**A Privacy-First Emergency Response System for Android**  
*Discreet. Reliable. Life-Saving.*

</div>

## ✨ Features

### 🚨 Core Features
- **One-Tap Emergency**: Configurable delay with visual countdown
- **Stealth Mode**: Discreet interface resembling system utility
- **Volume Button Trigger**: Press volume down 3 times quickly
- **Shake Detection**: Motion-based emergency activation
- **Real-time GPS**: Accurate location sharing with contacts
- **Bulk SMS Alerts**: Concurrent emergency message dispatch

### 🔒 Privacy & Security
- **No Cloud Dependency**: All data stored locally
- **Permission Transparency**: Granular control with explanations
- **Secure Storage**: Encrypted local data storage

## 🏗️ Architecture

### Technical Stack
- **Language**: Java
- **Min SDK**: API 21 (Android 5.0)
- **Target SDK**: API 34 (Android 14)
- **Architecture**: MVC with Service-Oriented Components
- **Key Services**: Foreground Services, Accessibility Service, Broadcast Receivers

### Core Components
- `MainActivity` - Primary UI and emergency initiation
- `EmergencyService` - Orchestrates emergency response
- `AccessibilityButtonService` - Volume button detection
- `ShakeDetectionService` - Motion pattern recognition
- `LocationService` - GPS and network location
- `SMSService` - Bulk SMS dispatch
- `DataManager` - Local data storage

## 📦 Installation

### Prerequisites
- Android Studio 2023.1.1+
- Java Development Kit (JDK) 11+
- Android SDK API 21-34
- Git for version control

### Build & Run
```bash
# Clone repository
git clone https://github.com/yourusername/silent-guardian-sos-app.git
cd silent-guardian-sos-app

# Open in Android Studio
# File → Open → Select project

# Build project
./gradlew build

# Install on device
./gradlew installDebug
