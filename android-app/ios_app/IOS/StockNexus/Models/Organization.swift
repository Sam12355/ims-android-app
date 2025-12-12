import Foundation

// MARK: - Region
struct Region: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var createdAt: Date?
    var updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    static func == (lhs: Region, rhs: Region) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - District
struct District: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var regionId: String
    var regionName: String?
    var createdAt: Date?
    var updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case regionId = "region_id"
        case regionName = "region_name"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    static func == (lhs: District, rhs: District) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Branch
struct Branch: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var address: String?
    var phone: String?
    var email: String?
    var districtId: String
    var districtName: String?
    var regionId: String?
    var regionName: String?
    var managerId: String?
    var managerName: String?
    var latitude: Double?
    var longitude: Double?
    var createdAt: Date?
    var updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case address
        case phone
        case email
        case districtId = "district_id"
        case districtName = "district_name"
        case regionId = "region_id"
        case regionName = "region_name"
        case managerId = "manager_id"
        case managerName = "manager_name"
        case latitude
        case longitude
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    static func == (lhs: Branch, rhs: Branch) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Sample Data
extension Region {
    static let sample = Region(
        id: "region1",
        name: "Norrland",
        createdAt: Date(),
        updatedAt: Date()
    )
    
    static let samples: [Region] = [
        Region(id: "region1", name: "Norrland", createdAt: Date(), updatedAt: Date()),
        Region(id: "region2", name: "Svealand", createdAt: Date(), updatedAt: Date()),
        Region(id: "region3", name: "Götaland", createdAt: Date(), updatedAt: Date())
    ]
}

extension District {
    static let sample = District(
        id: "district1",
        name: "Västernorrland",
        regionId: "region1",
        regionName: "Norrland",
        createdAt: Date(),
        updatedAt: Date()
    )
    
    static let samples: [District] = [
        District(id: "district1", name: "Västernorrland", regionId: "region1", regionName: "Norrland", createdAt: Date(), updatedAt: Date()),
        District(id: "district2", name: "Västerbotten", regionId: "region1", regionName: "Norrland", createdAt: Date(), updatedAt: Date()),
        District(id: "district3", name: "Stockholm", regionId: "region2", regionName: "Svealand", createdAt: Date(), updatedAt: Date())
    ]
}

extension Branch {
    static let sample = Branch(
        id: "branch1",
        name: "ICA Maxi Örnsköldsvik",
        address: "Storgatan 1, 891 33 Örnsköldsvik",
        phone: "+46660123456",
        email: "maxi.ornskoldsvik@ica.se",
        districtId: "district1",
        districtName: "Västernorrland",
        regionId: "region1",
        regionName: "Norrland",
        managerId: "1",
        managerName: "John Smith",
        latitude: 63.2908,
        longitude: 18.7169,
        createdAt: Date(),
        updatedAt: Date()
    )
    
    static let samples: [Branch] = [
        Branch(id: "branch1", name: "ICA Maxi Örnsköldsvik", address: "Storgatan 1", phone: nil, email: nil, districtId: "district1", districtName: "Västernorrland", regionId: "region1", regionName: "Norrland", managerId: nil, managerName: nil, latitude: nil, longitude: nil, createdAt: Date(), updatedAt: Date()),
        Branch(id: "branch2", name: "ICA Kvantum Sundsvall", address: "Köpmangatan 5", phone: nil, email: nil, districtId: "district1", districtName: "Västernorrland", regionId: "region1", regionName: "Norrland", managerId: nil, managerName: nil, latitude: nil, longitude: nil, createdAt: Date(), updatedAt: Date()),
        Branch(id: "branch3", name: "ICA Supermarket Umeå", address: "Rådhusesplanaden 10", phone: nil, email: nil, districtId: "district2", districtName: "Västerbotten", regionId: "region1", regionName: "Norrland", managerId: nil, managerName: nil, latitude: nil, longitude: nil, createdAt: Date(), updatedAt: Date())
    ]
}
