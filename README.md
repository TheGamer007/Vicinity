# Vicinity

Vicinity makes it easier for you to find out if any of your friends are in a certain area. The original idea behind it was to help with getting errands delegated more efficiently, in the times when they can't be handled personally.

## Implementation Overview

The app uses the Firebase Authentication and Realtime Database to keep track of users and their assorted details. Location tracking is performed using the latest set of Google Location and Maps APIs for the Android platform.

Both in order to reduce battery consumption and in order to reduce intrusiveness, the location service is not carried out constantly in the background and updates only when the Map screen of the app is visible to the user in the foreground.

The Realtime Database has a well thought-out set of rules that complement its structure. It helps limit access to only those who have been authorised by the user, and reduces the risk of unwanted leakage.

> **Note:** The current behaviour wherein the locations are being stored directly is not desirable due to a lot of reasons. Though I've looked into a number of potential solutions, the majority seem unsuited to the app's usecase. However, if you think you have an idea that might work, please feel free to open an issue with the details.
