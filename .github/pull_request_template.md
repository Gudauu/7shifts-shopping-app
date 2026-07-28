## What

<!-- One or two lines. What can a user do now that they could not before? -->

## Why

Closes #

## Screenshot

<!--
Drag a PNG in here for anything that changes the UI. Reviewers read the repo on
GitHub without building the app, so this is often the only time they see it work.

  adb exec-out screencap -p > shot.png
-->

## Acceptance evidence

| Acceptance criterion | Evidence type | Evidence |
| --- | --- | --- |
|  | Unit test / Compose test / Gradle or CI / document / manual emulator |  |

## Checklist

- [ ] Domain and state-holder logic has unit tests
- [ ] User-visible behaviour has automated evidence where valuable
- [ ] Every acceptance criterion above maps to appropriate evidence
- [ ] New decisions or deviations recorded without duplicating initial assumptions
- [ ] `./gradlew verify` green locally
- [ ] UI changes ran on a phone-sized emulator and were checked by eye
