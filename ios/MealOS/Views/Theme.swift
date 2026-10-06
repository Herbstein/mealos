import SwiftUI

enum MealTheme {
    // Fixed brand colours, paired explicitly on filled actions and dark feature panels.
    static let charcoal = Color(uiColor: rgb(0x191918))
    static let ivory = Color(uiColor: rgb(0xF7F3EA))
    static let peach = Color(uiColor: rgb(0xF3AD88))
    static let yellow = Color(uiColor: rgb(0xE7CE70))

    // Standalone controls need a deeper peach in light mode; filled actions keep peach.
    static let action = adaptive(light: 0x984C31, dark: 0xF3AD88)
    static let background = adaptive(light: 0xF7F3EA, dark: 0x191918)
    static let surface = adaptive(light: 0xFFFCF6, dark: 0x272725)
    static let text = adaptive(light: 0x242321, dark: 0xF7F3EA)

    private static func rgb(_ hex: UInt32) -> UIColor {
        UIColor(red: CGFloat((hex >> 16) & 255) / 255,
                green: CGFloat((hex >> 8) & 255) / 255,
                blue: CGFloat(hex & 255) / 255, alpha: 1)
    }
    private static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            rgb(traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
    static func display(_ size: CGFloat = 38) -> Font { .custom("BarlowCondensed-SemiBold", size: size, relativeTo: .largeTitle) }
}
struct RecipeRow: View {
    let recipe: Recipe
    var favorite = false
    var body: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                Text(recipe.shortName).font(.headline)
                Text("\(recipe.totalMinutes) min · \(recipe.effortMinutes) min hands-on").font(.subheadline).foregroundStyle(.secondary)
                Text(recipe.equipment.joined(separator: " · ")).font(.caption).foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
            if favorite { Image(systemName: "heart.fill").foregroundStyle(MealTheme.action).accessibilityLabel("Favorite") }
        }.padding(.vertical, 8)
    }
}
struct EmptyMealView: View {
    let title: String
    let symbol: String
    let detail: String
    var body: some View { ContentUnavailableView(title, systemImage: symbol, description: Text(detail)) }
}
