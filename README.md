# Stock Nexus Android App

A comprehensive Android native application for inventory management, replicating the Stock Nexus web application with identical look and feel.

## 📱 Overview

Stock Nexus Android is a modern native Android application built with Kotlin and Jetpack Compose. It provides a complete inventory management system with features for tracking items, managing stock levels, user management, analytics, and multi-branch operations.

## ✨ Key Features

### 📊 Dashboard

- Real-time analytics and statistics
- Quick action buttons for common tasks
- Recent activity feed
- Key performance indicators (KPIs)

### 📦 Inventory Management

- Complete item catalog with search and filtering
- Stock level tracking and alerts
- Stock in/out operations
- Category management
- Barcode scanning support (planned)

### 👥 User Management

- User roles and permissions
- Staff management
- Authentication and authorization
- Profile management

### 🏢 Multi-branch Operations

- Region and district management
- Branch-specific inventory tracking
- Cross-branch analytics

### 📈 Analytics & Reports

- Interactive charts and graphs
- Inventory value trends
- Stock movement analysis
- Low stock alerts

### 🎨 Modern UI/UX

- Material Design 3 implementation
- Dark/Light theme support
- Responsive design for phones and tablets
- Custom Stock Nexus branding (#E6002A primary color)

## 🛠 Technical Stack

### Core Technologies

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Navigation**: Navigation Component

### Key Dependencies

- **Jetpack Compose BOM**: 2023.10.01
- **Material Design 3**: Latest stable
- **Room Database**: 2.6.0
- **Retrofit**: 2.9.0
- **Hilt**: 2.48
- **Coroutines**: 1.7.3
- **WorkManager**: 2.8.1
- **Coil**: 2.5.0 (Image loading)
- **MPAndroidChart**: 3.1.0 (Charts)

## 🏗 Project Structure

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/stocknexus/android/
│   │   ├── StockNexusApplication.kt
│   │   └── presentation/
│   │       ├── MainActivity.kt
│   │       ├── components/
│   │       │   ├── NavigationDrawerContent.kt
│   │       │   └── TopAppBar.kt
│   │       ├── theme/
│   │       │   ├── Color.kt
│   │       │   ├── Theme.kt
│   │       │   └── Type.kt
│   │       ├── navigation/
│   │       │   └── StockNexusNavigation.kt
│   │       ├── dashboard/
│   │       │   └── DashboardScreen.kt
│   │       ├── inventory/
│   │       │   └── InventoryScreen.kt
│   │       ├── users/
│   │       │   └── UsersScreen.kt
│   │       ├── analytics/
│   │       │   └── AnalyticsScreen.kt
│   │       ├── branches/
│   │       │   └── BranchesScreen.kt
│   │       └── profile/
│   │           └── ProfileScreen.kt
│   └── res/
│       ├── values/
│       │   ├── strings.xml
│       │   ├── colors.xml
│       │   ├── themes.xml
│       │   ├── dimens.xml
│       │   └── arrays.xml
│       └── xml/
└── build.gradle
```

## 🚀 Getting Started

### Prerequisites

- Android Studio Arctic Fox (2020.3.1) or newer
- Android SDK API Level 24 or higher
- Kotlin 1.9.10 or newer
- Gradle 8.1.2 or newer

### Installation

1. **Clone the repository**

   ```bash
   git clone <repository-url>
   cd stock-nexus-android
   ```

2. **Build the project**

   ```bash
   ./gradlew build
   ```

3. **Run on device/emulator**
   ```bash
   ./gradlew installDebug
   ```

### VS Code Setup

The project is configured for VS Code development:

1. **Install required extensions**:

   - Kotlin Language Support
   - Android iOS Emulator
   - Gradle for Java

2. **Build using VS Code tasks**:

   - `Ctrl/Cmd + Shift + P` → "Tasks: Run Task" → "Build Android App"

3. **Development workflow**:
   - Use the integrated terminal for Gradle commands
   - Leverage IntelliSense for Kotlin development
   - Use the Android emulator extension for testing

## 🎨 Design System

### Colors

The app uses a cohesive color system matching the web application:

- **Primary**: #E6002A (Stock Nexus Red)
- **Secondary**: #6C757D (Gray)
- **Success**: #00C851 (Green)
- **Warning**: #FF8800 (Orange)
- **Error**: #BA1A1A (Red)

### Typography

- **Font Family**: Inter (system fallback for mobile)
- **Scales**: Material Design 3 typography scale
- **Weight**: Regular, Medium, SemiBold, Bold

### Components

- **Cards**: Elevated surfaces with 8dp corner radius
- **Buttons**: Material Design 3 specifications
- **Icons**: Material Icons Extended
- **Navigation**: Modal Navigation Drawer + Bottom Navigation

## 📐 Architecture

### MVVM Pattern

- **View**: Composable functions
- **ViewModel**: Business logic and state management
- **Model**: Data classes and repositories

### Clean Architecture Layers

1. **Presentation Layer**: UI components and ViewModels
2. **Domain Layer**: Business logic and use cases (planned)
3. **Data Layer**: Repositories and data sources (planned)

### Dependency Injection

- Hilt modules for dependency management
- Repository pattern for data access
- Use case pattern for business logic

## 🧪 Testing Strategy

### Unit Tests

- ViewModel testing with MockK
- Repository testing
- Use case testing

### Integration Tests

- Room database testing
- API integration testing

### UI Tests

- Compose UI testing
- Navigation testing
- User interaction testing

## 📱 Responsive Design

### Phone Support

- Optimized for phones (5" - 7")
- Single-pane navigation
- Compact layouts

### Tablet Support

- Large screen optimization
- Two-pane layouts where appropriate
- Extended functionality

## 🔐 Security Features

### Authentication

- JWT token-based authentication
- Biometric authentication support (planned)
- Session management

### Data Protection

- Local database encryption
- Secure API communication
- Permission-based access control

## 🌐 API Integration

### Endpoints

The app is designed to work with the Stock Nexus backend API:

- **Authentication**: `/auth/login`, `/auth/refresh`
- **Inventory**: `/api/inventory/*`
- **Users**: `/api/users/*`
- **Analytics**: `/api/analytics/*`
- **Branches**: `/api/branches/*`

### Data Synchronization

- Real-time updates via WebSocket (planned)
- Offline-first architecture with Room
- Conflict resolution strategies

## 🔄 Development Workflow

### Branch Strategy

- `main`: Production-ready code
- `develop`: Development branch
- `feature/*`: Feature branches

### Code Standards

- Kotlin coding conventions
- Compose best practices
- Material Design 3 guidelines
- Clean architecture principles

### CI/CD Pipeline

- Automated testing on PR
- Code quality checks (ktlint, detekt)
- Automated builds and releases

## 📦 Build Variants

### Debug

- Development configuration
- Debugging tools enabled
- Test data included

### Release

- Production configuration
- Obfuscation enabled
- Optimized for performance

## 🎯 Roadmap

### Version 1.0 (Current)

- ✅ Basic UI structure and navigation
- ✅ Dashboard with sample data
- ✅ Inventory management interface
- ✅ User management interface
- ✅ Theme system matching web app

### Version 1.1 (Planned)

- [ ] Backend API integration
- [ ] Real data implementation
- [ ] Authentication system
- [ ] Offline data synchronization

### Version 1.2 (Planned)

- [ ] Push notifications
- [ ] Barcode scanning
- [ ] Advanced analytics
- [ ] Export functionality

### Version 2.0 (Future)

- [ ] Multi-language support
- [ ] Advanced reporting
- [ ] Integration with external systems
- [ ] AI-powered insights

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Code Style

- Follow Kotlin conventions
- Use meaningful commit messages
- Add tests for new features
- Update documentation

## 📄 License

This project is proprietary software developed for Stock Nexus inventory management system.

## 📞 Support

For support and questions:

- Create an issue in the repository
- Contact the development team
- Check the documentation wiki

## 🙏 Acknowledgments

- Material Design 3 team for design system
- Jetpack Compose team for modern Android UI
- Stock Nexus web team for design consistency
- Open source community for libraries and tools

---

**Built with ❤️ using Kotlin and Jetpack Compose**
