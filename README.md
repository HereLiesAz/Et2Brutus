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

The floating controller allows you to automate a brute-force attack on a text field.

1.  **Configure:** Use the controller to identify the input field, submit button, and any popup that may appear.
2.  **Set Parameters:** Configure the bruteforce parameters, such as character set, length, and attempt pace.
3.  **Start:** Press the play button to begin the bruteforce process.
4.  **Stop:** Press the stop button to halt the process.

To stop the service and remove the floating controller, open the app and click the "Stop Service" button.
