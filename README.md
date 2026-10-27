<div align="center">

# ☀️ HavaCan

**English** · [Türkçe](README.tr.md)

**A talking, gamified personal weather assistant that plans your day around the weather**

Built during the **Kamp+** Android program by Turkcell Academy × the Turkish Ministry of Youth and Sports.

🥈 **Took 2nd place in the Kamp+ project competition.**

[![Android CI](https://github.com/bushushow/HavaCan/actions/workflows/android.yml/badge.svg)](https://github.com/bushushow/HavaCan/actions/workflows/android.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-3DDC84?logo=android&logoColor=white)
![Tests](https://img.shields.io/badge/unit%20tests-158-2ea44f)

[Features](#features) · [Architecture](#architecture) · [Running](#running) · [Tests](#tests) · [Data and privacy](#data-sources-and-privacy) · [Kamp+ assignments](#kamp-assignments)

</div>

---

HavaCan takes the real forecast from Open-Meteo and turns it into **decisions**: the best hour for a run, whether a plan collides with rain, a Plan B, a time that suits every friend you are meeting, and outfits built from the clothes actually in your wardrobe. The yellow mascot **HavaCan** lives on screen, chats with you and can walk you through the whole app by voice.

> **Real data only.** The app has no demo data. If the forecast does not arrive, no score or suggestion is made up; the screen shows the reason and a "Try again" button. Outfits are built only from clothes the user has photographed.

> The app's interface and voice commands are in Turkish.

<div align="center">

| Home | Dark theme | My Plans | My Wardrobe | HavaCan Assistant |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/ana-sayfa.png" width="170" alt="Home: sky header, 15 degrees and clear, HavaCan on an island and an hourly temperature chart"> | <img src="docs/screenshots/ana-sayfa-koyu.png" width="170" alt="Dark theme of the home screen: navy cards and a yellow selected tab"> | <img src="docs/screenshots/planlarim.png" width="170" alt="My Plans: weekly weather strip, a Weather Meet card and a running plan with weather tracking on"> | <img src="docs/screenshots/dolabim.png" width="170" alt="My Wardrobe: Shake for an Outfit card, add clothing button and photographed clothes"> | <img src="docs/screenshots/havacan-asistan.png" width="170" alt="HavaCan Assistant: the character, a greeting and two suggested questions"> |

<sub>Captured on the emulator with live Open-Meteo data (Gümüşhane, 26 October). The wardrobe items are drawn test photos.</sub>

</div>

## Features

| | Feature | What it does | Where the rules live |
|---|---|---|---|
| 🏝️ | **Home** | Animated island scene, a sky that changes with the weather and time of day, temperature/precipitation/wind chart, 16-day strip. Tap HavaCan for tips drawn from the real forecast | `domain/insight` |
| 🏃 | **Runner Mode** | A 0–100 running score for every hour (feels-like temperature, precipitation, wind, UV, daylight) and the best window | `RunningSuitabilityCalculator` |
| 📅 | **My Plans and Plan B** | Shows the risk at the plan's time (rain, storm, snow, wind, heat, cold) and suggests a dry hour within ±4 hours. Imports calendar events | `PlanAdvisor` |
| 🔔 | **Reminders and weather tracking** | Reminder before a plan; a notification if the weather turns bad or clears up (even while the app is closed) | WorkManager |
| 👟 | **Plan a Run** | Best start times for the chosen day, target distance, duration wheel, "how long before the run" reminder | `RunSlotPlanner` |
| 👥 | **Weather Meet** | Add friends and where they are coming from; the best time and place based on everyone's forecast, weather per person, Plan B; share the invite | `MeetPlanner` |
| 👕 | **My Wardrobe and Shake for an Outfit** | Add clothes from the gallery or camera; colour is detected on the phone and AI suggests tags. Shake the phone for a weather-appropriate outfit. If a piece is missing, it shows the missing category instead of a fake outfit | `OutfitEngine` |
| 💬 | **HavaCan Assistant** | Text and voice chat about weather, plans, outfits and running times; creates and edits plans (every change is read back and waits for confirmation) | `HavaCanAssistant` |
| 🎙️ | **Voice guide** | Navigate the whole app by voice; with TalkBack on, TalkBack reads the replies. Draggable microphone button | `VoiceCommandParser` |
| 📍 | **Choose location** | All 81 provinces and 973 districts of Turkey on the phone, searchable offline and insensitive to Turkish characters; Open-Meteo for places abroad; "my current location" finds the nearest real district | `PlaceDirectory` |
| 👤 | **Profile** | °C/°F, activity and style preferences, notification preferences, accessibility, privacy and confirmed data deletion | `ProfileStore` |
| 🌗 | **Theme** | Light and dark themes follow the phone setting; every header takes the colour of the sky | `ui/theme` |

### Accessibility

- Every touch target is at least 48 dp, icons have meaningful `contentDescription`s, and information is never conveyed by colour alone.
- **Large text**, **high contrast** and **reduce motion** in Profile; Android's animation setting is respected too.
- The voice guide and TalkBack work together: the microphone does not open until speech has finished.

## Architecture

Two Gradle modules, four layers. Details and rationale (in Turkish): **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**, decisions: **[docs/adr/](docs/adr/)**.

```
ui (Route → Screen)  →  ViewModel (StateFlow)  →  Repository (interface)  →  Retrofit · Room · file · SharedPreferences
        │                      │
        └──── domain rules (pure Kotlin, unit tested) ◄────┘
```

| Module | Contains | Rule |
|---|---|---|
| `:core:domain` | `model/` · `domain/` | Pure Kotlin/JVM. Android, Room or Compose imports **do not compile** |
| `:app` | `data/` · `di/` · `ui/` | Single wiring point `di/AppContainer`; ViewModels only see interfaces |

- **The UI never touches the network, ViewModels never draw, repositories never know about screens.**
- Every network request is exposed as a single `LoadState<T>` (Loading · Success · Empty · Error); errors are mapped to an `ErrorReason` in the ViewModel.
- Screens are split into **Route + Screen**; a Screen does not know its ViewModel.
- Navigation uses **Navigation Compose**; voice commands use the same routes.
- No global mutable state: the temperature unit and display preferences come from the profile flow through a `CompositionLocal`.

<details>
<summary><b>Package layout</b></summary>

```
Weather/
├── core/domain/                          :core:domain (pure Kotlin)
│   └── com.kampplus.havacan
│       ├── model/                        City, Forecast, Plan, ClothingItem, MeetPerson, TemperatureUnit…
│       └── domain/
│           ├── plans/                    PlanAdvisor: risk and Plan B
│           ├── running/                  RunningSuitabilityCalculator, RunSlotPlanner
│           ├── meet/                     MeetPlanner
│           ├── wardrobe/                 OutfitEngine, ClothingColors, ColorDetector
│           ├── insight/                  WeatherInsight
│           └── voice/                    VoiceCommandParser, HavaCanAssistant
└── app/                                  :app (Android)
    └── com.kampplus.havacan
        ├── di/AppContainer.kt            single wiring point
        ├── data/                         repositories, Retrofit, plan file, profile, calendar, location
        │   └── wardrobe/                 Room entities and DAO, photo store, AI client
        └── ui/
            ├── navigation/               HavaRoutes
            ├── common/                   LoadState, ErrorReason, sky header, day strip
            ├── theme/                    light/dark palette, typography, DisplaySettings
            └── home · plans · run · meet · wardrobe · chat · voice · city · profile
```

</details>

### Tech stack

| Area | Used |
|---|---|
| Language and UI | Kotlin 2.2, Jetpack Compose (BOM 2026.02), Material 3, scene and character drawn with Canvas |
| State | ViewModel + `StateFlow`, Coroutines, `SavedStateHandle` |
| Navigation | Navigation Compose |
| Network | Retrofit 3 + Gson, OkHttp |
| Persistence | Room (wardrobe), atomic JSON file (plans), SharedPreferences (preferences) |
| Background | WorkManager (reminders, weather check every 3 hours) |
| System | Photo Picker, camera, `SpeechRecognizer`, `TextToSpeech`, calendar, location, notifications |
| Backend | A small API on Vercel (keeps the AI key) |
| Quality | ktlint (Spotless), JUnit, kotlinx-coroutines-test, GitHub Actions |

## Running

**Requirements:** Android Studio (current), JDK 21 or newer, Android SDK 37. The app runs on Android 7.0 (API 24) and above.

```bash
git clone https://github.com/bushushow/HavaCan.git
cd HavaCan/Weather
./gradlew assembleDebug        # debug APK: app/build/outputs/apk/debug/
```

You can also open the `Weather/` folder in Android Studio and install with **Run**.

**AI features (optional).** Clothing tag suggestions and free-form chat need a backend URL. Without it the app still works fully: colour is detected on the phone, tags are entered by hand and the chat answers with rule-based replies.

```properties
# Weather/local.properties  (not committed)
HAVACAN_API_URL=https://<your-backend-url>
```

To set up your own backend: [backend/havacan-api/README.md](backend/havacan-api/README.md).

**Voice on the emulator.** For the voice guide, turn on Extended controls → Microphone → *Enable Host Microphone Access* and install the Turkish speech recognition pack (Settings → System → Languages → Speech).

## Tests

```bash
cd Weather
./gradlew spotlessCheck :core:domain:test testDebugUnitTest assembleDebug
```

**158 unit tests** (80 in `:core:domain`, 78 in `:app`) plus an on-device Room test:

| Area | Tests | What they protect |
|---|---|---|
| Rules | `PlanAdvisorTest`, `RunningSuitabilityCalculatorTest`, `RunSlotPlannerTest`, `MeetPlannerTest`, `OutfitEngineTest` | Risk thresholds, running score, time suggestions, meeting ranking, outfits only from real clothes |
| Assistant | `VoiceCommandParserTest`, `HavaCanAssistantTest` | Turkish commands, no change without confirmation, offline behaviour |
| Data | `ForecastMapperTest`, `CitySearchResponseTest`, `PlanStoreTest`, `PlaceDirectoryTest`, `TurkishProvincesTest`, `WardrobeDataTest`, `BackendAssistantTest` | JSON mapping, column→row conversion, atomic file writes, province/district search, validation of AI replies |
| ViewModel | `HomeViewModelTest`, `PlansViewModelTest`, `RunPlannerViewModelTest`, `LocationPickerViewModelTest`, `VoiceGuideControllerTest` | The four states, "Try again", offline saving, one save on a double tap, persistence |
| UI logic | `ForecastCardTest`, `HomeLogicTest`, `HavaCanTipsTest`, `HavaRoutesTest`, `UnitsTest` | Chart data, tips, route table, °C/°F |
| On device | `WardrobeDaoTest` | What is written to Room reads back unchanged |

Tests fake the network with stub `ForecastApi`/`CityApi`; shared helpers live under `app/src/test/.../testing/` (`MainDispatcherRule`, `FakeLocationStore`, `RecordingScheduler`).

## Quality gates

- **CI:** [`.github/workflows/android.yml`](.github/workflows/android.yml) on every push and PR: format check, tests for both modules, build and backend syntax check.
- **Code style:** ktlint (Spotless), same `.editorconfig` as the Kamp+ base repository.
- **PR template:** checklist for layer rules, states, accessibility and secrets ([`.github/pull_request_template.md`](.github/pull_request_template.md)).
- **Commits:** Conventional Commits; each commit says what was verified and how ([docs/GIT-HISTORY.md](docs/GIT-HISTORY.md)).

## Data sources and privacy

| Source | Used for | License |
|---|---|---|
| [Open-Meteo Forecast](https://open-meteo.com/) | Current, 16-day hourly and daily forecast (`timezone=auto`) | CC BY 4.0 |
| [Open-Meteo Geocoding](https://open-meteo.com/en/docs/geocoding-api) | Searching places abroad | CC BY 4.0 |
| [GeoNames](https://www.geonames.org/) | List of the 81 provinces and 973 districts, and missing coordinates | CC BY 4.0 |
| OpenRouter (through the backend) | Clothing photo tags, free-form chat | — |

Details and how to regenerate the data: [docs/data-sources.md](docs/data-sources.md).

**Privacy and security**

- **The API key is not in the APK or the Git history;** it lives only in the backend's environment secrets ([ADR-0003](docs/adr/0003-backend-for-secrets.md)).
- Clothing photos stay in the app's private folder. If a backend is configured, a copy scaled down to 512 pixels is sent to it for tag suggestions when a photo is added; without a backend, photos never leave the phone.
- Plans, clothes and preferences are stored only on the phone and can be deleted from Profile after confirmation.

| Permission | When it is requested | Why |
|---|---|---|
| Internet | At install | Weather forecast |
| Notifications | When the first plan is saved | Reminders and weather alerts |
| Calendar (read) | On tapping "Connect calendar" | Importing events as plans |
| Microphone | On the first voice command, after an explanation | Voice guide |
| Approximate location | On tapping "Use my current location" | Finding the nearest district |

## Kamp+ assignments

The submission point of each required assignment is tagged; evidence (screenshots, Logcat) is under `docs/kamp/`.

| Tag | Assignment | Evidence |
|---|---|---|
| `kamp-uygulama-2` | "Merhaba Kamp!", city cards, `LazyColumn`, click logging | [docs/kamp/uygulama-2](docs/kamp/uygulama-2/README.md) |
| `kamp-uygulama-3` | Search, `remember` / `rememberSaveable`, filter, empty state | [docs/kamp/uygulama-3](docs/kamp/uygulama-3/README.md) |
| `kamp-uygulama-4` | Open-Meteo search with Retrofit, ViewModel, the four states | [docs/kamp/uygulama-4](docs/kamp/uygulama-4/README.md) |

Other tags: `pre-architecture-final` (the last state before the architecture refactor).

## Repository layout

```
HavaCan/
├── Weather/            Android project (:app, :core:domain)
├── backend/havacan-api Small API that keeps the AI key (Vercel; a Cloudflare entry point too)
├── docs/               Architecture, ADRs, Git strategy, data sources, Kamp+ evidence, design drafts
├── tools/places/       Scripts that generate the province and district data from GeoNames
└── .github/            CI and PR template
```

## Known limitations

- WorkManager is not exact; in battery saver a reminder can be a few minutes late.
- The forecast cache is in memory only (10 min); with no internet an old forecast is not shown, an error is.
- In Weather Meet, participants do not vote inside the app (that would need accounts and a server); the suggestion is passed on through a shared invite.
- The HavaCan character and scenes are Canvas drawings; real 3D assets and an app icon do not exist yet ([docs/havacan-assets.md](docs/havacan-assets.md)).
- There is no full in-app attribution page for the data sources yet (Profile › About); attributions are in this README and in `docs/data-sources.md`.
- `applicationId` is still `com.example.weather` to keep the data on the demo device; the code package is `com.kampplus.havacan`.

## Licenses

Data source licenses are listed above. The Nunito font is used under the SIL Open Font License 1.1 ([docs/licenses/Nunito-OFL.txt](docs/licenses/Nunito-OFL.txt)).
