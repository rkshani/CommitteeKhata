# Committee Khata (BC Manager)

Offline Android app to run committees (BC): members, monthly payments, whose turn is next, and who has not paid.

**Package:** `com.rk.committee` · **Stack:** Kotlin, Jetpack Compose (Material 3), MVVM, Hilt, Room, Navigation Compose · minSdk 26, targetSdk 35

## Features
- Many committees in one app
- Members with fixed turn order (set by admin, move up/down; locked after their pot is given)
- Monthly payments: tap to mark paid/unpaid, "Mark all paid"
- Give pot to the member whose turn it is, with undo
- Overview: current month, collected vs pot, unpaid list, next turn, pending months per member
- PDF report (summary, members table, payment grid) shared via any app (WhatsApp etc.)
- English or Urdu (RTL). Picked once on first launch; one language at a time; can change in Settings
- 100% offline (Room database)

## How it works
- Round 1 = start month. Member #1 gets the pot in round 1, #2 in round 2, and so on.
- Number of rounds = number of members. Pot = amount × members.

## Open & run
1. Open the `CommitteeKhata` folder in Android Studio (Ladybug or newer).
2. Let Gradle sync (AGP 8.7.3, Kotlin 2.1.0, Gradle 8.11.1).
3. Run on a device/emulator.

## Structure
```
app/src/main/java/com/rk/committee/
  data/local       Room entities, DAOs, database
  data/repository  CommitteeRepository
  data/prefs       First-launch flag + AppLanguage (per-app locale)
  domain           CommitteeDetails (all business rules)
  report           PDF report generator + share
  ui/screens       language, home, edit, detail (overview/members/payments), settings
  ui/components    shared UI pieces
res/values         English strings
res/values-ur      Urdu strings
```
