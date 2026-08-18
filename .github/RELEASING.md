# Publishing to Maven Central

Releases use the Sonatype Central Portal through `.github/workflows/publish.yml`. Maven Central versions are immutable: never reuse a version after a successful deployment.

## One-Time Setup

1. Sign in to [Central Portal](https://central.sonatype.com/) and verify the `io.github.ingrowthly` namespace. The GitHub account or organization must control the matching repository namespace.
2. In Central Portal, open **Account > Generate User Token**. Save the generated username and password; these are not the normal account credentials.
3. Create a password-protected OpenPGP signing key if one is not already available:

   ```bash
   gpg --full-generate-key
   gpg --list-secret-keys --keyid-format=long
   gpg --armor --export-secret-keys <KEY_ID>
   ```

4. Publish the public key so Maven Central can verify signatures:

   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```

5. In GitHub, open **Settings > Secrets and variables > Actions** and create these repository secrets:

   | Secret | Value |
   | --- | --- |
   | `MAVEN_CENTRAL_USERNAME` | Central Portal user-token username |
   | `MAVEN_CENTRAL_PASSWORD` | Central Portal user-token password |
   | `SIGNING_KEY` | Complete ASCII-armored private key, including BEGIN/END lines |
   | `SIGNING_PASSWORD` | Private-key passphrase |

Never store credentials, private keys, or exported key files in the repository.

## Release Procedure

1. Run `./gradlew clean build` and confirm CI is green.
2. Move entries from `Unreleased` in `CHANGELOG.md` to a dated version heading, then add a new empty `Unreleased` section.
3. Commit and push the release preparation.
4. Create and push a version tag matching `vMAJOR.MINOR.PATCH`:

   ```bash
   git tag -s v3.0.0 -m "Release 3.0.0"
   git push origin v3.0.0
   ```

The tag starts the publish workflow. It removes the leading `v`, builds with `-PreleaseVersion=3.0.0`, signs every artifact, uploads the deployment, and requests automatic release on Maven Central.

For a controlled retry before a successful Central deployment, run **Publish to Maven Central** from the GitHub Actions page and enter the version manually. Do not retry a version that Central Portal already accepted; increment the patch version instead. Portal validation and search-index propagation can take several minutes after the workflow succeeds.

## Local Checks

Generate the publication and inspect its POM without credentials:

```bash
./gradlew generatePomFileForMavenPublication
./gradlew publishToMavenLocal
```

The release upload itself should run only in GitHub Actions, where credentials remain isolated in repository secrets.
