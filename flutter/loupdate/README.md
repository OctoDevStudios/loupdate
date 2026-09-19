# Flutter plugin

```yaml
dependencies:
  loupdate:
    path: path/to/loupdate/flutter/loupdate
```

```dart
import 'package:loupdate/loupdate.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Loupdate.init(LoupdateConfig(
    endpoint: const String.fromEnvironment('LOUPDATE_ENDPOINT'),
    region: const String.fromEnvironment('LOUPDATE_REGION'),
    bucket: const String.fromEnvironment('LOUPDATE_BUCKET'),
    accessKeyId: const String.fromEnvironment('LOUPDATE_KEY_ID'),
    secretAccessKey: const String.fromEnvironment('LOUPDATE_KEY_SECRET'),
    appId: 'my-shop',
  ));
  runApp(const MyApp());
}

// After first frame / in home:
await Loupdate.check();
```

## Android host

Your app's `android/settings.gradle` must include the core module from this monorepo:

```gradle
include ':loupdate-core'
project(':loupdate-core').projectDir = new File(settingsDir, '../../../loupdate-core')
```

(Adjust the relative path if needed.)

iOS is not supported yet (Android OTA focus).
