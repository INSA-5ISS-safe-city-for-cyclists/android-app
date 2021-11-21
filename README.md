# Safe city for cyclists - Android app

This app uses [MapLibre](https://maplibre.org/) as map engine, and [Geoapify](https://geoapify.com) for the map data and routing engine.

## Installation

Download android studio then clone the project
```shell
git clone git@github.com:INSA-5ISS-safe-city-for-cyclists/android-app.git
```

Open the project on Android Studio and sync gradle.

Geoapify maps needs an api key to work which is not synced in git.

- If you are a repo admin
  - You can access the github secrets to retrieve the contents of the file `secrets.xml`. Copy the contents and put it in `app/src/main/res/values/secrets.xml`.
- If you are **not** a repo admin
    - Create a new file in `app/src/main/res/values/secrets.xml` with the following structure And replace `YOUR_TOKEN` with your own Geoapify token.
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <string name="geoapify_access_token">YOUR_TOKEN</string>
</resources>
```