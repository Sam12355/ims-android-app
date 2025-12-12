# Stock Nexus iOS Application

## Project Overview
Stock Nexus is an inventory management system for restaurant/retail operations built with SwiftUI and MVVM architecture.

## Technical Stack
- **Platform:** iOS 15.0+, iPadOS 15.0+
- **Language:** Swift 5.9+
- **UI Framework:** SwiftUI
- **Architecture:** MVVM (Model-View-ViewModel)
- **Reactive Programming:** Combine
- **Secure Storage:** Keychain

## Primary Color
- Stock Nexus Red: #E6002A

## User Roles
1. Admin (Level 4) - Full system access
2. Manager (Level 3) - Full branch operations
3. Assistant Manager (Level 2) - Most branch operations
4. Staff (Level 1) - Limited front-line access

## Project Structure
```
StockNexus/
├── App/
│   └── StockNexusApp.swift
├── Models/
├── Views/
├── ViewModels/
├── Services/
├── Utilities/
└── Resources/
```

## Development Guidelines
- Follow SwiftUI best practices
- Use async/await for network calls
- Implement proper error handling
- Use environment objects for shared state
- Follow iOS Human Interface Guidelines
