AI Voice Recorder & Transcriber 🎙️🤖

A modern, cloud-built Android application that records long-form audio and transcribes it with high accuracy using Google's Gemini 2.5 Flash AI model.

Built entirely with Kotlin and Jetpack Compose (Material 3), this project is designed to be built in the cloud using GitHub Actions—meaning no Android Studio is required to generate the installable .apk.

✨ Features

Long-Form Recording: Efficiently captures audio as compressed .m4a files directly on your device, allowing for 10+ minutes of recording without using massive amounts of storage or battery.

Gemini AI Transcription: Sends the compressed audio directly to the Gemini 2.5 Flash model for incredibly accurate, word-for-word transcriptions with natural spacing and speaker formatting.

Cloud Built: Uses a GitHub Actions workflow (build.yml) to automatically compile the app into an .apk file whenever you update the code.

Modern UI: Built with Jetpack Compose featuring smooth animations, a dark/light mode adaptable UI, and a simple one-tap copy-to-clipboard function.

🚀 How to Build & Install (No PC Required)

You do not need to download any developer tools to build this app. GitHub will do it for you!

Fork or Clone this Repository: Ensure all the files (including the .github/workflows/build.yml file) are in your own GitHub account.

Go to the "Actions" Tab: Click on the Actions tab at the top of your GitHub repository.

Run the Workflow: Select Build Android APK from the left menu. If it hasn't run automatically, click the Run workflow dropdown on the right and start it.

Download the App: Wait ~3-5 minutes for the build to turn green. Click on the completed build, scroll down to the Artifacts section, and download the VoiceTranscriber-APK.zip file.

Install: Unzip the downloaded file and send the app-debug.apk to your Android phone (via Google Drive, email, or USB). Tap it on your phone to install!

🔑 Usage & API Key Setup

To use the transcription feature, you will need a free Gemini API Key:

Go to Google AI Studio.

Click Create API Key and copy it.

Open the installed app on your Android phone.

Tap Configure API Key and paste your key into the app.

Tap the microphone icon to start recording (the app will ask for microphone permissions first). Tap it again to stop, and your transcription will generate automatically!

💰 Cost & Privacy

Cost: This app uses the Gemini 2.5 Flash model. A 10-minute audio recording uses roughly 15,000 tokens. The Google AI Studio free tier generously allows up to 1,000,000 tokens per minute and 1,500 requests per day, meaning everyday usage is completely free.

Privacy: Audio is recorded 100% offline and stored locally in your app's cache. It is only sent over the internet to Google's API when you stop recording to fetch the transcription.

🛠️ Tech Stack

Language: Kotlin

UI Framework: Jetpack Compose (Material 3)

AI SDK: com.google.ai.client.generativeai

CI/CD: GitHub Actions (Ubuntu / Java 17 / Gradle)
