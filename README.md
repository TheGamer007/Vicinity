# Vicinity

Vicinity makes it easier for you to find out if any of your friends are in a certain area. The original motivation behind it was to help with getting errands delegated more efficiently, but it can also be used for impromptu meetups, or other activities that involve location sharing.

## Usage Instructions

The app requires a Firebase project and a Google Maps API key for proper functioning. The corresponding config files have not been added to the repo, so you will need to set up your own.

First create a project on the [Firebase Console](https://console.firebase.google.com) and then download the `google-services.json` file. Similarly, create a new project on the [Google Could Platform](https://console.cloud.google.com/) dashboard and add the "Maps SDK for Android". Note the API key generated at this step. Both of these can and should be limited appropriately by adding your default or release signing keys, rather than leaving them open.

Clone this repository and copy the previously downloaded `google-services.json` file to the `app/` folder. Create a new resource file called `secrets.xml` in the `res/` folder and add a string resource named `maps_API_key` with the API key generated before.

Both the `google-services.json` and `secrets.xml` files have been added to this repo's `.gitignore`. If you don't intend to republish the code, then you can skip making a `secrets.xml` file, and add the Maps API Key either to `strings.xml`, or directly in the `AndroidManifest.xml` file.

## Implementation Overview

The app uses the Firebase Authentication and Realtime Database to keep track of users and their assorted details. Location tracking is performed using the latest set of Google Location and Maps APIs for the Android platform.

Both in order to reduce battery consumption and in order to reduce intrusiveness, the location is updated only when the Map screen of the app is visible to the user in the foreground. It is not synced when the app is closed, minimized, or if the screen is locked.

The Realtime Database has a well thought-out set of rules that complement its structure. It helps limit access to only those who have been authorized by the user, and reduces the risk of unwanted leakage.

> **Note:** Currently, locations are stored in the database directly as latitude,longitude pairs. I've tried looking into ways to add an additional layer of security, but none of them seemed appropriate. If you have any ideas that you think might work, please feel free to open an issue with the details.

## License

This app is licensed under the [MIT License](./LICENSE.md)
