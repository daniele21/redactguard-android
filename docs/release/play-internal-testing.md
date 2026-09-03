# Google Play internal testing release

Status: release-signing runbook for RedactGuard

## Signing invariant

RedactGuard and the Android Local LLM Harness are separate Play applications, but the Harness same-publisher authorization requires the APKs installed by Google Play to carry the same accepted app-signing certificate.

Two signing identities must not be confused:

- **Upload key**: signs the `.aab` before it is uploaded to Play Console.
- **Play app-signing key**: signs the APKs Google Play actually delivers to devices.

RedactGuard intentionally reuses the existing Harness **upload key** locally and in protected CI. In Play Console, RedactGuard must also be configured to **use the same Play app-signing key as the Harness application**. Reusing only the upload key is not sufficient for same-publisher Binder authorization.

Do not use Internal App Sharing as evidence for this architecture. Use the normal **Internal testing** track so the installed packages use the configured Play app-signing identity.

## Package identities

Release packages:

- Harness Host: `io.github.daniele21.localllm.phonetest`
- RedactGuard: `io.github.daniele21.redactguard`

Debug packages are separate and must not be uploaded to Play:

- Harness Host debug: `io.github.daniele21.localllm.phonetest.debug`
- RedactGuard debug: `io.github.daniele21.redactguard.debug`

## Shared upload-key configuration

RedactGuard does not contain signing material in the repository. `.jks` and `.keystore` files are ignored by Git.

The release build consumes exactly these environment variables:

```text
REDACTGUARD_ANDROID_UPLOAD_STORE_FILE
REDACTGUARD_ANDROID_UPLOAD_STORE_PASSWORD
REDACTGUARD_ANDROID_UPLOAD_KEY_ALIAS
REDACTGUARD_ANDROID_UPLOAD_KEY_PASSWORD
```

The helper `scripts/build-redactguard-release.sh` defaults to the existing Harness upload identity:

```text
~/.keystore/local-llm-phone-test-upload.jks
alias: local-llm-phone-test-upload
```

On macOS it reads the password from the same Keychain item already used by the Harness release helper:

```text
service: io.github.daniele21.localllm.phonetest.android-upload
account: local-llm-phone-test-upload
```

This deliberately avoids creating or committing a second copy of the upload private key.

## Local preflight

From the RedactGuard repository:

```bash
bash scripts/build-redactguard-release.sh check
```

The command must resolve the shared Harness keystore, alias, password source and Android SDK without printing the secret.

If the shared Harness Keychain password has not been configured yet, run the Harness setup helper once from the Harness checkout:

```bash
bash scripts/build-phone-test-release.sh setup
```

## Build the Play bundle locally

Run:

```bash
bash scripts/build-redactguard-release.sh build
```

The helper exports the RedactGuard-specific signing variables only for the build process and runs:

```bash
./gradlew :app:bundleRelease
```

The expected output is:

```text
app/build/outputs/bundle/release/app-release.aab
```

The helper verifies the JAR signature, promotes the identity-bearing artifact and prints the upload certificate SHA-256 embedded in the bundle.

Direct `bundleRelease` or `assembleRelease` is fail-closed when the full signing configuration is absent. `REDACTGUARD_ALLOW_UNSIGNED_RELEASE=true` exists only for an explicit non-distributable CI artifact and must never be used for a Play upload.

`app/version.properties` remains the repository baseline for normal local builds. Protected Play CI supplies a positive `PLAY_VERSION_CODE` environment override so it can build the exact next Play version without modifying or committing `version.properties`.

## Automated GitHub -> Play Internal Testing

`.github/workflows/play-internal.yml` is the canonical automated publishing path.

Automatic publication is deliberately disabled until repository variable `PLAY_INTERNAL_ENABLED=true` is configured. After activation, an app-relevant push to `dev` starts the release workflow. Before accessing signing material or Google Play, the job resolves its exact checked-out SHA and requires the existing `Validate` workflow for the same SHA and `push` event to complete successfully.

The release job then:

1. authenticates to Google through GitHub OIDC and Workload Identity Federation;
2. creates a temporary Android Publisher edit and lists the current APK/AAB version codes;
3. selects `max(versionCode) + 1`, or `1` when the Play application has no uploaded artifacts;
4. reconstructs the PKCS12 upload keystore only inside the GitHub runner;
5. calls the canonical `bash scripts/build-android-aab.sh build` entrypoint with signing variables and the resolved `PLAY_VERSION_CODE`;
6. verifies the signed AAB with `jarsigner`;
7. refreshes its short-lived Google access token;
8. uploads the AAB, updates track `internal` to a completed release and commits the Play edit;
9. stores the released AAB as a seven-day GitHub Actions evidence artifact.

Publishing is serialized per RedactGuard application so two concurrent pushes cannot independently choose the same next version code. Failed publication deletes the uncommitted Play edit when possible.

The workflow also supports `workflow_dispatch`, but the selected candidate must already have a successful `Validate` **push** run for the exact same commit. Manual dispatch therefore cannot bypass repository validation.

### GitHub configuration

Create a GitHub Environment named:

```text
play-internal
```

Store the following Environment secrets:

```text
ANDROID_UPLOAD_KEYSTORE_B64
ANDROID_UPLOAD_STORE_PASSWORD
ANDROID_UPLOAD_KEY_PASSWORD
```

`ANDROID_UPLOAD_KEY_PASSWORD` may contain the same value as the store password when the PKCS12 key uses one shared password.

Store these non-secret variables in the `play-internal` Environment:

```text
GCP_WORKLOAD_IDENTITY_PROVIDER
GCP_PLAY_SERVICE_ACCOUNT
ANDROID_UPLOAD_KEY_ALIAS
```

When the existing alias is unchanged, `ANDROID_UPLOAD_KEY_ALIAS` may be omitted because the workflow defaults to:

```text
local-llm-phone-test-upload
```

Finally create this **repository variable** only after the Google/Play configuration is ready:

```text
PLAY_INTERNAL_ENABLED=true
```

Keeping the enable flag at repository scope lets the workflow decide whether an automatic `push` release job should start before Environment variables are loaded.

## Google identity and Play permissions

The workflow does not store a long-lived Google service-account JSON key. GitHub obtains a short-lived OIDC identity and impersonates the service account configured by `GCP_PLAY_SERVICE_ACCOUNT` through the provider configured by `GCP_WORKLOAD_IDENTITY_PROVIDER`.

The Google Cloud project must have the Google Play Android Developer API enabled. The Workload Identity provider must trust this GitHub repository and allow it to impersonate the service account. The same service-account email must be invited in Google Play Console with permission to release the RedactGuard application to testing tracks. Production-release permission is not required for this workflow.

## Register RedactGuard in Play Console

Create the application in the same Google Play developer account as the Harness application.

Use:

```text
Name: RedactGuard
Package: io.github.daniele21.redactguard
Track: Testing > Internal testing
```

Before relying on the application for Binder integration evidence, configure Play App Signing so RedactGuard uses the **same app-signing key as the Harness app**. Do not allow a new independent Play app-signing key to become the RedactGuard identity.

After enrollment, compare in Play Console:

```text
Harness      > App integrity > App signing key certificate > SHA-256
RedactGuard  > App integrity > App signing key certificate > SHA-256
```

The two app-signing SHA-256 fingerprints must be identical.

Separately verify that the upload certificate shown for RedactGuard matches the upload key reconstructed by CI/local release tooling. The upload certificate and app-signing certificate are different concepts and may be different certificates.

## Internal-testing release

For the first application bootstrap, complete the minimum Play Console application setup and ensure the package/upload/app-signing identities are accepted. After the automated path is enabled, normal validated `dev` changes no longer require manually creating the internal release.

Maintain the tester list and opt-in flow in Play Console. Install both Harness and RedactGuard from their normal Play Internal testing flows on the physical ARM64 device; do not mix a Play release APK with an ADB-installed debug build for final same-publisher evidence.

Every uploaded release uses a strictly increasing Play version code. CI resolves that value from current Play state; it does not rely on GitHub run numbers or manually increment the repository file.

## Device evidence

After both applications are delivered by Play, continue with `docs/evidence/physical-two-apk.md`.

For release-like evidence, record at minimum:

- Harness source commit and Play release/version;
- RedactGuard source commit and Play release/version;
- RedactGuard AAB SHA-256;
- Consumer SDK version;
- model/config identity selected by the Harness;
- device model and Android version;
- installed release package identities;
- Play app-signing certificate SHA-256 for both applications;
- pass/fail for Host absent, reconnect, analysis, review, cancellation, Host death, export and process-death privacy.

Only after the physical two-APK gate passes with the Play-delivered applications may the legacy in-Harness OMBRA consumer be removed.
