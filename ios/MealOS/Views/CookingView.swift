import SwiftUI

struct CookingView: View {
    let recipe: Recipe
    let portion: Portion
    @EnvironmentObject private var store: MealStore
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.dynamicTypeSize) private var typeSize
    @State private var index = 0
    @State private var finished = false
    private var step: RecipeStep { recipe.steps[min(index, recipe.steps.count - 1)] }
    var body: some View {
        GeometryReader { geometry in
            let wide = geometry.size.width >= 720 && !typeSize.isAccessibilitySize
            Group {
                if wide {
                    HStack(alignment: .top, spacing: 24) {
                        ScrollView { stepPanel }.frame(maxWidth: .infinity)
                        ScrollView { workspace }.frame(maxWidth: .infinity)
                    }
                } else {
                    ScrollView { VStack(spacing: 24) { stepPanel; workspace } }
                }
            }.padding(wide ? 28 : 16)
                .background(MealTheme.enamel.ignoresSafeArea())
        }
        .toolbar(.hidden, for: .tabBar)
        .navigationTitle(recipe.shortName).navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(MealTheme.enamel, for: .navigationBar).toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear { index = min(store.snapshot.cookingSteps[recipe.id] ?? 0, max(0, recipe.steps.count - 1)); UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .onChange(of: scenePhase) { _, phase in UIApplication.shared.isIdleTimerDisabled = phase == .active }
        .onChange(of: index) { _, value in store.setStep(value, recipe: recipe.id) }
        .alert("Dinner is ready", isPresented: $finished) {
            Button("Done", role: .cancel) {}
        } message: { Text(recipe.leftoverNote.isEmpty ? "Enjoy your meal. Any running timers will continue on this device." : recipe.leftoverNote + " Any running timers will continue on this device.") }
    }
    private var stepPanel: some View {
        VStack(alignment: .leading, spacing: 24) {
            HStack { Text("Step \(index + 1) of \(recipe.steps.count)").font(.headline); Spacer(); Text(step.equipment).font(.subheadline).multilineTextAlignment(.trailing) }
            Text(step.title).font(MealTheme.display(48)).fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text(step.instruction).font(.title2).fixedSize(horizontal: false, vertical: true)
            if let tip = step.tip, !tip.isEmpty { Label(tip, systemImage: "lightbulb").font(.body) }
            if let duration = step.timerSeconds {
                TimerControl(id: step.timerID(recipeID: recipe.id, index: index), label: step.title, duration: duration)
            }
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 16) { previousButton; nextButton }
                VStack(spacing: 12) { nextButton; previousButton }
            }
            if index + 1 < recipe.steps.count {
                Divider()
                Text("Up next: \(recipe.steps[index + 1].title)").font(.headline)
                Text(recipe.steps[index + 1].equipment).font(.subheadline)
            }
        }.padding(24).frame(maxWidth: .infinity, alignment: .leading)
            .foregroundStyle(MealTheme.enamel).background(MealTheme.cream, in: RoundedRectangle(cornerRadius: 24))
    }
    private var previousButton: some View {
        Button { index = max(0, index - 1) } label: { Label("Previous", systemImage: "chevron.left").frame(minHeight: 44) }
            .buttonStyle(.bordered).tint(MealTheme.enamel).disabled(index == 0)
    }
    private var nextButton: some View {
        Button { if index + 1 < recipe.steps.count { index += 1 } else { finished = true } } label: {
            Label(index + 1 < recipe.steps.count ? "Next step" : "Finish cooking", systemImage: index + 1 < recipe.steps.count ? "chevron.right" : "checkmark")
                .font(.headline).frame(maxWidth: .infinity, minHeight: 44)
        }.buttonStyle(.borderedProminent).tint(Color(red: 0.69, green: 0.23, blue: 0.035)).foregroundStyle(.white)
    }
    private var workspace: some View {
        VStack(alignment: .leading, spacing: 24) {
            VStack(alignment: .leading, spacing: 16) {
                Text("At your station").font(MealTheme.display(34))
                Text(portion.label).font(.subheadline)
                IngredientList(recipe: recipe, portion: portion)
            }.padding(24).foregroundStyle(MealTheme.enamel).background(MealTheme.cream, in: RoundedRectangle(cornerRadius: 24))
            let timers = store.snapshot.timers.filter { $0.endAt != nil || $0.remaining < $0.duration }
            if !timers.isEmpty {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Timers on this device").font(.title2.weight(.semibold)).foregroundStyle(MealTheme.cream)
                    ForEach(timers) { timer in TimerControl(id: timer.id, label: timer.label, duration: Int(timer.duration)) }
                }
            }
            if let notice = store.timerNotice {
                VStack(alignment: .leading, spacing: 12) {
                    Text(notice).foregroundStyle(MealTheme.cream)
                    if let url = URL(string: UIApplication.openSettingsURLString) { Link("Open notification settings", destination: url).tint(MealTheme.yellow) }
                }
            }
            if !recipe.leftoverNote.isEmpty {
                VStack(alignment: .leading, spacing: 12) { Text("Lunch tomorrow").font(.headline); Text(recipe.leftoverNote) }.foregroundStyle(MealTheme.cream)
            }
        }
    }
}
struct TimerControl: View {
    let id: String
    let label: String
    let duration: Int
    @EnvironmentObject private var store: MealStore
    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let timer = store.timer(id: id)
            let seconds = timer?.seconds(at: context.date) ?? duration
            let running = timer?.endAt != nil && seconds > 0
            VStack(alignment: .leading, spacing: 12) {
                Text(label).font(.headline)
                Text(seconds == 0 ? "Ready" : String(format: "%02d:%02d", seconds / 60, seconds % 60))
                    .font(.system(.largeTitle, design: .rounded).monospacedDigit()).foregroundStyle(MealTheme.yellow)
                    .accessibilityLabel(seconds == 0 ? "Timer finished" : "\(seconds / 60) minutes, \(seconds % 60) seconds")
                HStack(spacing: 16) {
                    Button {
                        if running { store.pauseTimer(id) }
                        else { Task { await store.startTimer(id: id, label: label, duration: duration) } }
                    } label: { Label(running ? "Pause" : seconds == 0 ? "Restart" : "Start", systemImage: running ? "pause.fill" : "play.fill").frame(minHeight: 44) }
                    Button("Reset") { store.resetTimer(id) }.frame(minWidth: 44, minHeight: 44)
                }.buttonStyle(.bordered).tint(MealTheme.cream)
            }.padding(18).frame(maxWidth: .infinity, alignment: .leading)
                .foregroundStyle(MealTheme.cream).background(MealTheme.enamel, in: RoundedRectangle(cornerRadius: 16))
        }
    }
}
