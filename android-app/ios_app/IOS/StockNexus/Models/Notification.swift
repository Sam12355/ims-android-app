import Foundation

// MARK: - Notification Type
enum NotificationType: String, Codable, CaseIterable {
    case stockAlert = "stock_alert"
    case moveoutList = "moveout_list"
    case delivery = "delivery"
    case userUpdate = "user_update"
    case system = "system"
    
    var icon: String {
        switch self {
        case .stockAlert: return "exclamationmark.triangle.fill"
        case .moveoutList: return "list.bullet.rectangle.fill"
        case .delivery: return "shippingbox.fill"
        case .userUpdate: return "person.fill"
        case .system: return "gear"
        }
    }
}

// MARK: - App Notification
struct AppNotification: Identifiable, Codable, Equatable {
    let id: String
    var title: String
    var message: String
    var type: NotificationType
    var isRead: Bool
    var data: [String: String]?
    let userId: String
    let createdAt: Date
    var readAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case title
        case message
        case type
        case isRead = "is_read"
        case data
        case userId = "user_id"
        case createdAt = "created_at"
        case readAt = "read_at"
    }
    
    static func == (lhs: AppNotification, rhs: AppNotification) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Chat Message
struct ChatMessage: Identifiable, Codable, Equatable {
    let id: String
    var content: String
    var senderId: String
    var senderName: String
    var senderPhotoURL: String?
    var chatRoomId: String
    let createdAt: Date
    var isRead: Bool
    var readBy: [String]?
    
    enum CodingKeys: String, CodingKey {
        case id
        case content
        case senderId = "sender_id"
        case senderName = "sender_name"
        case senderPhotoURL = "sender_photo_url"
        case chatRoomId = "chat_room_id"
        case createdAt = "created_at"
        case isRead = "is_read"
        case readBy = "read_by"
    }
    
    static func == (lhs: ChatMessage, rhs: ChatMessage) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Chat Room
struct ChatRoom: Identifiable, Codable, Equatable {
    let id: String
    var name: String?
    var participants: [String]
    var participantNames: [String: String]?
    var lastMessage: String?
    var lastMessageAt: Date?
    var lastMessageSenderId: String?
    var isGroup: Bool
    var branchId: String?
    let createdAt: Date
    var updatedAt: Date?
    var unreadCount: Int
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case participants
        case participantNames = "participant_names"
        case lastMessage = "last_message"
        case lastMessageAt = "last_message_at"
        case lastMessageSenderId = "last_message_sender_id"
        case isGroup = "is_group"
        case branchId = "branch_id"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case unreadCount = "unread_count"
    }
    
    static func == (lhs: ChatRoom, rhs: ChatRoom) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Sample Data
extension AppNotification {
    static let sample = AppNotification(
        id: "notif1",
        title: "Low Stock Alert",
        message: "Coca Cola 33cl is running low (25 units remaining)",
        type: .stockAlert,
        isRead: false,
        data: ["itemId": "1"],
        userId: "1",
        createdAt: Date(),
        readAt: nil
    )
    
    static let samples: [AppNotification] = [
        AppNotification(id: "notif1", title: "Low Stock Alert", message: "Coca Cola 33cl is running low", type: .stockAlert, isRead: false, data: nil, userId: "1", createdAt: Date(), readAt: nil),
        AppNotification(id: "notif2", title: "New Moveout List", message: "John Manager created a new moveout list", type: .moveoutList, isRead: false, data: nil, userId: "1", createdAt: Calendar.current.date(byAdding: .hour, value: -1, to: Date())!, readAt: nil),
        AppNotification(id: "notif3", title: "Delivery Confirmed", message: "Your fish delivery has been confirmed", type: .delivery, isRead: true, data: nil, userId: "1", createdAt: Calendar.current.date(byAdding: .day, value: -1, to: Date())!, readAt: Date()),
        AppNotification(id: "notif4", title: "Account Activated", message: "Your account has been activated", type: .userUpdate, isRead: true, data: nil, userId: "1", createdAt: Calendar.current.date(byAdding: .day, value: -2, to: Date())!, readAt: Date())
    ]
}

extension ChatMessage {
    static let sample = ChatMessage(
        id: "msg1",
        content: "Hello, when is the next delivery?",
        senderId: "3",
        senderName: "Staff Member",
        senderPhotoURL: nil,
        chatRoomId: "room1",
        createdAt: Date(),
        isRead: false,
        readBy: nil
    )
    
    static let samples: [ChatMessage] = [
        ChatMessage(id: "msg1", content: "Hello, when is the next delivery?", senderId: "3", senderName: "Staff Member", senderPhotoURL: nil, chatRoomId: "room1", createdAt: Calendar.current.date(byAdding: .minute, value: -30, to: Date())!, isRead: true, readBy: nil),
        ChatMessage(id: "msg2", content: "Tomorrow morning at 8 AM", senderId: "1", senderName: "John Manager", senderPhotoURL: nil, chatRoomId: "room1", createdAt: Calendar.current.date(byAdding: .minute, value: -25, to: Date())!, isRead: true, readBy: nil),
        ChatMessage(id: "msg3", content: "Thanks!", senderId: "3", senderName: "Staff Member", senderPhotoURL: nil, chatRoomId: "room1", createdAt: Calendar.current.date(byAdding: .minute, value: -20, to: Date())!, isRead: false, readBy: nil)
    ]
}

extension ChatRoom {
    static let sample = ChatRoom(
        id: "room1",
        name: "Store Chat",
        participants: ["1", "3"],
        participantNames: ["1": "John Manager", "3": "Staff Member"],
        lastMessage: "Thanks!",
        lastMessageAt: Date(),
        lastMessageSenderId: "3",
        isGroup: true,
        branchId: "branch1",
        createdAt: Date(),
        updatedAt: Date(),
        unreadCount: 1
    )
    
    static let samples: [ChatRoom] = [
        ChatRoom(id: "room1", name: "Store Chat", participants: ["1", "3"], participantNames: nil, lastMessage: "Thanks!", lastMessageAt: Date(), lastMessageSenderId: "3", isGroup: true, branchId: "branch1", createdAt: Date(), updatedAt: nil, unreadCount: 1),
        ChatRoom(id: "room2", name: nil, participants: ["1", "2"], participantNames: ["2": "Admin User"], lastMessage: "Please check the inventory", lastMessageAt: Calendar.current.date(byAdding: .hour, value: -2, to: Date())!, lastMessageSenderId: "2", isGroup: false, branchId: nil, createdAt: Date(), updatedAt: nil, unreadCount: 0)
    ]
}
