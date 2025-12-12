# Stock Nexus iOS Application

A comprehensive inventory management system for restaurant and retail operations built with SwiftUI and MVVM architecture.

## Features

- **Authentication**: Secure login/signup with JWT token-based authentication
- **Dashboard**: Real-time inventory overview with key metrics
- **Stock Management**: Stock in/out operations with barcode scanning support
- **Moveout Lists**: Create and manage moveout lists for inventory transfers
- **ICA Deliveries**: Track and receive deliveries from ICA
- **Calendar**: Schedule and manage events
- **Reports**: Visual analytics with charts (iOS 16+)
- **User Management**: Admin features for managing staff and roles
- **Branch/Outlet Management**: Multi-location support
- **Notifications**: Push notification support
- **Settings**: Customizable app settings with biometric authentication

## Technical Stack

- **Platform**: iOS 15.0+, iPadOS 15.0+
- **Language**: Swift 5.9+
- **UI Framework**: SwiftUI
- **Architecture**: MVVM (Model-View-ViewModel)
- **Reactive Programming**: Combine
- **Secure Storage**: Keychain
- **Charts**: Swift Charts (iOS 16+)

## Project Structure

```
StockNexus/
├── App/
│   ├── StockNexusApp.swift      # App entry point
│   └── ContentView.swift         # Root view with navigation logic
├── Models/
│   ├── User.swift                # User and authentication models
│   ├── Item.swift                # Inventory item models
│   ├── Organization.swift        # Organization/Branch/Outlet models
│   ├── MoveoutList.swift         # Moveout list models
│   ├── ICADelivery.swift         # ICA delivery models
│   ├── CalendarEvent.swift       # Calendar event models
│   ├── ActivityLog.swift         # Activity logging models
│   └── Notification.swift        # Notification models
├── Views/
│   ├── Auth/                     # Authentication views
│   ├── Main/                     # Main tab and dashboard
│   ├── Inventory/                # Stock in/out views
│   ├── Moveout/                  # Moveout list views
│   ├── More/                     # More menu and management views
│   ├── Components/               # Reusable UI components
│   ├── ICA/                      # ICA delivery views
│   ├── Calendar/                 # Calendar and activity views
│   ├── Reports/                  # Reports and analytics views
│   └── Settings/                 # Settings views
├── ViewModels/
│   ├── AuthViewModel.swift       # Authentication state management
│   ├── DashboardViewModel.swift  # Dashboard data management
│   ├── InventoryViewModel.swift  # Inventory operations
│   └── NavigationManager.swift   # App navigation state
├── Services/
│   ├── NetworkManager.swift      # API networking layer
│   ├── AuthService.swift         # Authentication API
│   ├── InventoryService.swift    # Inventory API
│   ├── UserService.swift         # User management API
│   ├── OrganizationService.swift # Organization API
│   ├── MoveoutListService.swift  # Moveout list API
│   └── ICADeliveryService.swift  # ICA delivery API
├── Utilities/
│   ├── Extensions.swift          # Swift extensions
│   ├── Constants.swift           # App constants
│   └── KeychainManager.swift     # Secure storage manager
└── Resources/
    ├── Assets.xcassets/          # Images and colors
    └── Info.plist                # App configuration
```

## User Roles

1. **Admin (Level 4)** - Full system access
2. **Manager (Level 3)** - Full branch operations
3. **Assistant Manager (Level 2)** - Most branch operations
4. **Stock Controller** - Inventory-focused access
5. **Staff (Level 1)** - Limited front-line access

## Getting Started

### Prerequisites

- Xcode 15.0 or later
- iOS 15.0+ device or simulator
- macOS Sonoma or later (recommended)

### Installation

1. Clone the repository
2. Open `StockNexus.xcodeproj` in Xcode
3. Select your development team in Signing & Capabilities
4. Build and run on simulator or device

### Configuration

1. Update the `AppConstants.swift` file with your API base URL:
   ```swift
   static let baseURL = "https://your-api-url.com/api"
   ```

2. Configure your bundle identifier in the project settings

## API Integration

The app expects a REST API with the following endpoints:

- `POST /auth/login` - User authentication
- `POST /auth/register` - User registration
- `GET /inventory/items` - Fetch inventory items
- `POST /inventory/stock-in` - Record stock in
- `POST /inventory/stock-out` - Record stock out
- And more...

## Design Guidelines

- **Primary Color**: Stock Nexus Red (#E6002A)
- **UI Framework**: Follow iOS Human Interface Guidelines
- **Accessibility**: Support for Dynamic Type and VoiceOver

## Development Guidelines

- Follow SwiftUI best practices
- Use async/await for network calls
- Implement proper error handling
- Use environment objects for shared state
- Write unit tests for ViewModels and Services

## Building for Production

1. Update version and build numbers in Info.plist
2. Configure your App Store Connect account
3. Archive and upload using Xcode

## License

Copyright © 2024 Stock Nexus. All rights reserved.
