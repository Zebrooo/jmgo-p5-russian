# JMGO P5 fast Russian voice recognition

## Goal

Replace the JMGO-specific build's slow Whisper inference with offline Vosk Russian recognition while preserving the existing FUTO activity, IME, microphone-button flow, and Whisper fallback.

## Architecture

Only application ID `org.futo.voiceinput.jmgo` may use Vosk. A pure policy selects Vosk when the official model directory `files/vosk-model-small-ru-0.22` exists. A process-wide engine loads the model once, creates a recognizer per utterance, accepts 16 kHz float PCM, and parses Vosk's final JSON result. The existing Whisper path remains unchanged for every other flavor and is used if Vosk is absent or throws.

## Data and lifecycle

- Input is the same mono 16 kHz float buffer already captured by `AudioRecognizer`.
- `JmgoVoskEngine.prepare()` runs while audio is being recorded so first-use model loading overlaps speech.
- `JmgoVoskEngine.transcribe()` is serialized because the global model is shared.
- The model remains resident until Android kills the process; a recognizer is closed after every utterance.
- Recognized text is returned through the existing `finished(String)` callback and is never logged.

## Dependencies and device data

- Maven dependency: `com.alphacephei:vosk-android:0.3.75`.
- Model: official `vosk-model-small-ru-0.22.zip` from `alphacephei.com`.
- Model is copied to the custom app's private files directory after APK installation; it is not bundled into other variants.
- Android 13 and `armeabi-v7a` must remain supported.

## Failure handling

If model selection, loading, native initialization, JSON parsing, or transcription fails, `AudioRecognizer` loads and runs the existing Whisper model. A blank but valid Vosk result remains blank; it does not trigger a second transcription.

## Verification

- Unit-test package/model selection and JSON extraction.
- Run the complete Gradle unit-test suite and assemble the `playStoreDebug` APK.
- Install without clearing data, copy the official model, select the custom IME, and grant microphone permission.
- Verify Russian speech from an active search field and measure time from microphone release to returned text.

