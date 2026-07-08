# Offline Voice Input — Live Streaming Preview fork

A fork of [notune/android_transcribe_app](https://github.com/notune/android_transcribe_app)
that adds an **optional live streaming preview** to the voice keyboard (IME), so you can
see your speech being transcribed *while you talk* instead of only after you tap stop.

Everything else — 100% offline on-device transcription with NVIDIA's Parakeet TDT model,
the Rust backend, live subtitles, and the three speech-to-text integration paths — is
unchanged from upstream. See the [upstream README](https://github.com/notune/android_transcribe_app)
for the full feature list, usage, and background.

## What this fork changes

The upstream voice keyboard is **batch**: it records the whole utterance, runs a single
transcription when you tap stop, and inserts the result all at once. There's no feedback
while you speak, so you can't tell if your words are being picked up correctly until it's
done.

This fork adds a **cosmetic live preview**: while you dictate, a timer re-transcribes the
recent audio and shows it in a single italic line inside the keyboard. It's a confidence
monitor — you can watch words land and stop early if something's clearly wrong.

Key properties by design:

- **Opt-in.** Off by default. Enable it under **Settings → Live streaming preview**. When
  off, behaviour is identical to upstream — no extra computation happens.
- **The preview never commits text.** It only draws to an italic strip in the keyboard UI;
  the input field is untouched until the final commit. This deliberately avoids all the
  cursor / composing-region edge cases that in-field partial text would introduce.
- **The committed text is still authoritative.** On stop, the app runs the same single
  full-buffer transcription as upstream and commits that. The preview cannot truncate or
  corrupt the committed result.
- **Bounded cost.** Each preview pass transcribes only a trailing ~15s window and refreshes
  roughly every 0.7s. The worker is single-in-flight, so it self-throttles on slower
  devices (a slow pass just means a less frequent refresh); the final commit is never
  affected. Because it runs extra inference, the feature is opt-in.

## How it works

| Layer | File | Change |
|---|---|---|
| Preview engine | `src/voice_session.rs` | A `preview` flag on `start_recording` spawns a ticker thread that re-transcribes the trailing audio window (~15s, ~700ms cadence) and pushes each hypothesis via a new `onPartialText` JNI callback. Single-in-flight; exits when the session ends. |
| IME binding | `src/ime.rs` | `startRecording` takes the preview flag and forwards it. |
| Popup path | `src/recognize.rs` | Signature-compat only (the recognize popup passes `false`). |
| Keyboard UI | `RustInputMethodService.java` | Reads the opt-in setting, receives `onPartialText`, and renders it in the italic strip. The final commit path is unchanged. |
| Layout | `res/layout/ime_layout.xml` | A single-line italic `ime_preview_text` strip (start-ellipsized so the most recent words stay visible), hidden unless a preview arrives. |
| Setting | `MainActivity.java`, `res/layout/activity_main.xml`, `res/values/strings.xml` | The opt-in "Live streaming preview" toggle (marker-file backed, like the other settings). |

The Parakeet model and the `transcribe-rs` inference API are untouched — the stateless
`transcribe_samples` call serves both the preview passes and the final commit.

## Building

Identical to upstream (JDK 17+, Android SDK, NDK, Rust with the `aarch64-linux-android`
target, and `cargo-ndk`). See the
[upstream build instructions](https://github.com/notune/android_transcribe_app#building).

```bash
./gradlew assembleRelease   # -> app/build/outputs/apk/release/app-release.apk
```

The build targets `arm64-v8a` only and downloads the ~670 MB Parakeet model on first
build.

## Credits & license

All credit for the app, the Rust backend, and the on-device model integration goes to
[notune/android_transcribe_app](https://github.com/notune/android_transcribe_app) and the
projects it builds on (Parakeet TDT by NVIDIA, `transcribe-rs` by CJ Pais). This fork only
adds the streaming-preview feature described above.

Licensed under [MIT](LICENSE), same as upstream.
