# Et2Brutus

![Alt text](app/src/main/res/drawable/et2brutus_banner.webp?raw=true "Et2Brutus")

This app allows you to automate repetitive tasks on your screen using a floating controller. It uses Android's Accessibility Service to programmatically click on the screen.

## Setup

Before you can use the app, you need to grant two permissions:

1.  **Draw Over Other Apps:** This permission is required to show the floating controller on top of other apps.
2.  **Accessibility Service:** This permission allows the app to perform clicks and other actions on your behalf.

To grant these permissions:

1.  Open the Screen Bruteforcer app.
2.  Click the "Request" button for "Draw Over Other Apps" and grant the permission in the settings screen that appears.
3.  Click the "Go To Settings" button for "Accessibility Service" and enable the "Screen Bruteforcer" service in the accessibility settings.
4.  Click the "Re-Check Permissions" button in the app to confirm that the permissions have been granted.

## Usage

There are two ways to start the floating controller:

### Manual Start

1.  Open the Screen Bruteforcer app.
2.  Ensure both required permissions are granted.
3.  Click the "Start Service" button.

A floating controller will appear on your screen.

### Keyboard Shortcut

If you have a physical keyboard connected to your device, you can use the following shortcut to start the floating controller at any time:

**`Ctrl + G`**

This shortcut works even when the app is not in the foreground.

## Using the Floating Controller

The floating controller allows you to record and replay a sequence of taps.

*(Note: The exact functionality of the floating controller is inferred from the code. This section may need to be updated after testing.)*

1.  **Record:** Press the record button on the floating controller to start recording your taps.
2.  **Perform Taps:** Tap on the screen in the sequence you want to automate.
3.  **Stop Recording:** Press the stop button on the controller to finish recording.
4.  **Playback:** Press the play button to have the app automatically replay the sequence of taps you recorded.

To stop the service and remove the floating controller, open the app and click the "Stop Service" button.
