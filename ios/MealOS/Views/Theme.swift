import SwiftUI

enum MealTheme {
    static let enamel = Color(red: 0.031, green: 0.196, blue: 0.141)
    static let cream = Color(red: 0.953, green: 0.941, blue: 0.910)
    static let yellow = Color(red: 0.898, green: 0.780, blue: 0.420)
    static let action = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 1, green: 0.70, blue: 0.55, alpha: 1) : UIColor(red: 0.69, green: 0.23, blue: 0.035, alpha: 1)
    })
    static let background = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(red: 0.024, green: 0.110, blue: 0.078, alpha: 1) : UIColor(red: 0.953, green: 0.941, blue: 0.910, alpha: 1)
    })
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
