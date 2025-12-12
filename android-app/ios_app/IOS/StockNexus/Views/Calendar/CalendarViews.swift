import SwiftUI

// MARK: - Calendar View
struct CalendarView: View {
    @State private var selectedDate = Date()
    @State private var events: [CalendarEvent] = CalendarEvent.sampleList
    @State private var showCreateEvent = false
    
    var eventsForSelectedDate: [CalendarEvent] {
        events.filter { Calendar.current.isDate($0.startDate, inSameDayAs: selectedDate) }
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Calendar Picker
                DatePicker(
                    "Select Date",
                    selection: $selectedDate,
                    displayedComponents: [.date]
                )
                .datePickerStyle(.graphical)
                .accentColor(.stockNexusRed)
                .padding()
                .background(Color(.systemBackground))
                
                Divider()
                
                // Events List
                if eventsForSelectedDate.isEmpty {
                    VStack(spacing: 16) {
                        Spacer()
                        Image(systemName: "calendar.badge.exclamationmark")
                            .font(.system(size: 50))
                            .foregroundColor(.secondary)
                        Text("No Events")
                            .font(.headline)
                        Text("No events scheduled for this date.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Button("Create Event") {
                            showCreateEvent = true
                        }
                        .foregroundColor(.stockNexusRed)
                        Spacer()
                    }
                } else {
                    List(eventsForSelectedDate) { event in
                        CalendarEventRow(event: event)
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Calendar")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showCreateEvent = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateEvent) {
                CreateCalendarEventView(selectedDate: selectedDate)
            }
        }
        .navigationViewStyle(.stack)
    }
}

// MARK: - Calendar Event Row
struct CalendarEventRow: View {
    let event: CalendarEvent
    
    var body: some View {
        HStack(spacing: 12) {
            // Time indicator
            RoundedRectangle(cornerRadius: 2)
                .fill(event.type.color)
                .frame(width: 4)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(event.title)
                    .font(.headline)
                
                HStack {
                    Image(systemName: "clock")
                        .font(.caption)
                    Text(event.startDate.formatted(date: .omitted, time: .shortened))
                    
                    if let endDate = event.endDate {
                        Text("-")
                        Text(endDate.formatted(date: .omitted, time: .shortened))
                    }
                }
                .font(.caption)
                .foregroundColor(.secondary)
                
                if let location = event.location {
                    HStack {
                        Image(systemName: "mappin")
                            .font(.caption)
                        Text(location)
                    }
                    .font(.caption)
                    .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            Text(event.type.rawValue.capitalized)
                .font(.caption)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(event.type.color.opacity(0.2))
                .foregroundColor(event.type.color)
                .cornerRadius(8)
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Create Calendar Event View
struct CreateCalendarEventView: View {
    let selectedDate: Date
    @Environment(\.dismiss) private var dismiss
    
    @State private var title = ""
    @State private var description = ""
    @State private var startTime = Date()
    @State private var endTime = Date()
    @State private var eventType: CalendarEvent.EventType = .meeting
    @State private var location = ""
    @State private var isAllDay = false
    
    var body: some View {
        NavigationView {
            Form {
                Section("Event Details") {
                    TextField("Title", text: $title)
                    
                    Picker("Type", selection: $eventType) {
                        ForEach(CalendarEvent.EventType.allCases, id: \.self) { type in
                            Text(type.rawValue.capitalized).tag(type)
                        }
                    }
                    
                    TextField("Location (optional)", text: $location)
                }
                
                Section("Time") {
                    Toggle("All Day", isOn: $isAllDay)
                    
                    if !isAllDay {
                        DatePicker("Start", selection: $startTime, displayedComponents: [.hourAndMinute])
                        DatePicker("End", selection: $endTime, displayedComponents: [.hourAndMinute])
                    }
                }
                
                Section("Description") {
                    TextEditor(text: $description)
                        .frame(minHeight: 80)
                }
            }
            .navigationTitle("New Event")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Create") {
                        // TODO: Create event
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(title.isEmpty)
                }
            }
        }
    }
}

// MARK: - Activity Log View
struct ActivityLogView: View {
    @State private var logs: [ActivityLog] = ActivityLog.sampleList
    @State private var selectedFilter: ActivityLog.ActionType? = nil
    @State private var searchText = ""
    
    var filteredLogs: [ActivityLog] {
        var result = logs
        
        if let filter = selectedFilter {
            result = result.filter { $0.action == filter }
        }
        
        if !searchText.isEmpty {
            result = result.filter {
                $0.description.localizedCaseInsensitiveContains(searchText) ||
                $0.userName.localizedCaseInsensitiveContains(searchText)
            }
        }
        
        return result.sorted { $0.timestamp > $1.timestamp }
    }
    
    var groupedLogs: [(String, [ActivityLog])] {
        let grouped = Dictionary(grouping: filteredLogs) { log in
            log.timestamp.formatted(date: .abbreviated, time: .omitted)
        }
        return grouped.sorted { $0.key > $1.key }
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("Search activity...", text: $searchText)
                }
                .padding()
                .background(Color.secondaryBackground)
                .cornerRadius(10)
                .padding()
                
                // Filter
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ActivityFilterChip(title: "All", isSelected: selectedFilter == nil) {
                            selectedFilter = nil
                        }
                        
                        ForEach(ActivityLog.ActionType.allCases, id: \.self) { type in
                            ActivityFilterChip(title: type.rawValue.capitalized, isSelected: selectedFilter == type) {
                                selectedFilter = type
                            }
                        }
                    }
                    .padding(.horizontal)
                }
                .padding(.bottom)
                
                // Activity List
                if filteredLogs.isEmpty {
                    VStack(spacing: 16) {
                        Spacer()
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.system(size: 50))
                            .foregroundColor(.secondary)
                        Text("No Activity")
                            .font(.headline)
                        Text("No activity logs found.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                } else {
                    List {
                        ForEach(groupedLogs, id: \.0) { date, logs in
                            Section(date) {
                                ForEach(logs) { log in
                                    ActivityLogRow(log: log)
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Activity Log")
        }
        .navigationViewStyle(.stack)
    }
}

// MARK: - Activity Filter Chip
struct ActivityFilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? Color.stockNexusRed : Color.secondaryBackground)
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(20)
        }
    }
}

// MARK: - Activity Log Row
struct ActivityLogRow: View {
    let log: ActivityLog
    
    var actionIcon: String {
        switch log.action {
        case .stockIn: return "arrow.down.circle.fill"
        case .stockOut: return "arrow.up.circle.fill"
        case .transfer: return "arrow.left.arrow.right.circle.fill"
        case .adjustment: return "slider.horizontal.3"
        case .create: return "plus.circle.fill"
        case .update: return "pencil.circle.fill"
        case .delete: return "trash.circle.fill"
        case .login: return "person.crop.circle.badge.checkmark"
        case .logout: return "person.crop.circle.badge.xmark"
        }
    }
    
    var actionColor: Color {
        switch log.action {
        case .stockIn, .create, .login: return .green
        case .stockOut, .delete, .logout: return .red
        case .transfer: return .blue
        case .adjustment, .update: return .orange
        }
    }
    
    var body: some View {
        HStack(spacing: 12) {
            // Action Icon
            Image(systemName: actionIcon)
                .font(.title2)
                .foregroundColor(actionColor)
                .frame(width: 32)
            
            // Details
            VStack(alignment: .leading, spacing: 4) {
                Text(log.description)
                    .font(.body)
                
                HStack {
                    Text(log.userName)
                        .fontWeight(.medium)
                    Text("•")
                    Text(log.timestamp.formatted(date: .omitted, time: .shortened))
                }
                .font(.caption)
                .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Notifications View
struct NotificationsView: View {
    @State private var notifications: [AppNotification] = AppNotification.sampleList
    
    var unreadCount: Int {
        notifications.filter { !$0.isRead }.count
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                if notifications.isEmpty {
                    VStack(spacing: 16) {
                        Spacer()
                        Image(systemName: "bell.slash")
                            .font(.system(size: 50))
                            .foregroundColor(.secondary)
                        Text("No Notifications")
                            .font(.headline)
                        Text("You're all caught up!")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                } else {
                    List {
                        ForEach(notifications) { notification in
                            NotificationRow(notification: notification)
                                .swipeActions(edge: .trailing) {
                                    Button("Delete", role: .destructive) {
                                        if let index = notifications.firstIndex(where: { $0.id == notification.id }) {
                                            notifications.remove(at: index)
                                        }
                                    }
                                }
                                .swipeActions(edge: .leading) {
                                    Button(notification.isRead ? "Unread" : "Read") {
                                        if let index = notifications.firstIndex(where: { $0.id == notification.id }) {
                                            notifications[index].isRead.toggle()
                                        }
                                    }
                                    .tint(.blue)
                                }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Notifications")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if unreadCount > 0 {
                        Button("Mark All Read") {
                            for i in notifications.indices {
                                notifications[i].isRead = true
                            }
                        }
                    }
                }
            }
        }
        .navigationViewStyle(.stack)
    }
}

// MARK: - Notification Row
struct NotificationRow: View {
    let notification: AppNotification
    
    var typeIcon: String {
        switch notification.type {
        case .lowStock: return "exclamationmark.triangle.fill"
        case .delivery: return "truck.box.fill"
        case .transfer: return "arrow.left.arrow.right"
        case .approval: return "checkmark.circle.fill"
        case .system: return "gear"
        case .alert: return "bell.fill"
        }
    }
    
    var typeColor: Color {
        switch notification.type {
        case .lowStock, .alert: return .orange
        case .delivery: return .blue
        case .transfer: return .purple
        case .approval: return .green
        case .system: return .gray
        }
    }
    
    var body: some View {
        HStack(spacing: 12) {
            // Type Icon
            ZStack {
                Circle()
                    .fill(typeColor.opacity(0.2))
                    .frame(width: 44, height: 44)
                
                Image(systemName: typeIcon)
                    .foregroundColor(typeColor)
            }
            
            // Content
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(notification.title)
                        .font(.headline)
                        .fontWeight(notification.isRead ? .regular : .semibold)
                    
                    if !notification.isRead {
                        Circle()
                            .fill(Color.stockNexusRed)
                            .frame(width: 8, height: 8)
                    }
                }
                
                Text(notification.message)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                
                Text(notification.createdAt.timeAgoDisplay())
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.vertical, 4)
        .opacity(notification.isRead ? 0.7 : 1.0)
    }
}

// MARK: - Preview Provider
struct CalendarViews_Previews: PreviewProvider {
    static var previews: some View {
        CalendarView()
    }
}
