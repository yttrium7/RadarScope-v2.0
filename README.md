

# RadarScope

##### ![version](https://img.shields.io/badge/modified-v2.0-green.svg)  Location-Chat-based application

##### team project - original repository access:    [RadarOpal-Service](https://github.com/yttrium7/RadarOpal-Service)


## What is RadarScope?
RadarScope is a location-based Android application that mainly helps older people to find the way. Using the combination of map navigation and voice communication.
Older people can use this app to locate and navigate to their destination with or without help provider. This app also has text and voice chat system that allows people to communicate with each other.
<img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/radarscope_icon.png" height=100 alt="app-logo">

## Features:
- Real-time location based navigation
- VoIP, talk to assistance provider in realtime
- Send the safe messages about current movement and location in duration of 10mins
- Add contacts
- 1 to 1 text chat system
- Sharing real-time location with others
- User sign-up/sign-in

## Setup API and Service
- [Firebase](https://firebase.google.com/) for storing main data
- [Google Maps](https://console.developers.google.com) for calculating geofence and route points to positioning clients on the set time.
- [Mapbox for Android](https://www.mapbox.com/) for navigation.
- [Agora](https://www.agora.io/en/) for voice chat in realtime.

## Device Requirements
Android device running ![SDK](https://img.shields.io/badge/SDK-27-orange.svg)

## Download Guidance
1. Simply Clone this rep and open folder of `mapHelper` in Android Studio.
2. To try this app in Android Studio, Run the app using an Android smartphone or an emulator (which can be chosen in through Android Studio).
3. Give the permission to **Internet connection**, **GPS** and **Microphone**.


## User Guidance
| | |
|--|--|
| sign up or login easily | <img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/rs-creat-account.gif" align=left height=400 alt="create-account"> |

|You could also update your personal information in the 'Account' tab| <img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/rs-4fragment-edit-account.gif" align=left height=400 alt="edit-account">  |

|Add friends|<img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/rs-send-friend-request-log-out.gif" align=left height=400 alt="add-friend"> <img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/rs-accept-request.gif" align=left height=400 alt="accept-request">
|Unfriend if in any need|<img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/rs-unfriend.gif" align=left height=400 alt="unfriend">|
|You could send the message to your contacts via the Message.|<img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/rs-first-chat.gif" align=left height=400 alt="chat">|

- You could choose either `ASK FOR HELP` or `FIND YOURSELF` options

|  |  |
|--|--|
|After choosing `ASK FOR HELP`, you need choose a contact to invite him to help you. He could set the destination and route for you and talk to you by voice in realtime.|<img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/rs-ask-for-help-and-accept-help.gif" align=left height=400 alt="ask-for-help"> <img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/rs-help-someone-to-find-way.gif" align=left height=400 alt="accept-help">|
|After choosing `HELP YOURSELF`, you need to set the destination and route by yourself. You could also choose a contact to send the safe message on your way.| |
|What's more, if you do not know what to do after choosing the `FIND YOURSELF`, you could simply click the red button right corner '?' to look for the help in this duration.|<img src="https://github.com/yttrium7/RadarScope-v2.0/blob/master/rs-clips/rs-need-help.gif" align=left height=400 alt="need-help">|
|After setting the destination and route, you could simply follow the navigation to find the way.| |


## Test cases
1. Test cases stores in the `Test` and `AndroidTest` folders.
2. Test folder stores the unit tests of `LoginActivity` and `SignUpActivity`.
3. `TestPresenter` and `Viewer` Interface in the `main.controller` is the supplement class for the unit tests.
4. `AndroidTest` folder stores the instrumentation tests of `LoginActivity` and `WelcomeActivity`.
5. It uses `espresso` and `Junit4` to test the UI and Activity of the projects.
