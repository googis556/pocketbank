# 갤럭시 홈화면 위젯 설치 가이드

## 파일 복사 위치

```
android/app/src/main/java/com/pocketbank/app/
  ├── CalendarWidget.kt        ← widget-files/CalendarWidget.kt
  └── SharedPrefsPlugin.kt     ← widget-files/SharedPrefsPlugin.kt

android/app/src/main/res/layout/
  └── calendar_widget.xml      ← widget-files/calendar_widget.xml

android/app/src/main/res/xml/
  └── calendar_widget_info.xml ← widget-files/calendar_widget_info.xml
```

## AndroidManifest.xml 에 추가

`</application>` 태그 바로 앞에 추가:

```xml
<receiver
    android:name=".CalendarWidget"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/calendar_widget_info" />
</receiver>
```

## MainActivity.kt 에 추가

```kotlin
class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        registerPlugin(SharedPrefsPlugin::class.java)  // ← 이 줄 추가
        super.onCreate(savedInstanceState)
    }
}
```

## app/build.gradle 의존성 확인

보통 Capacitor 프로젝트에 이미 있지만, 없으면 추가:

```gradle
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

## 빌드 후 테스트

1. Android Studio에서 빌드
2. 갤럭시 홈화면 빈 공간 길게 누르기 → 위젯
3. PocketBank 위젯 찾아서 추가
4. 앱 열어서 로그인 → 그룹 선택하면 위젯에 일정 표시됨
