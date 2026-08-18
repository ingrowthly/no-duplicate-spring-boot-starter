# Repository Guidelines

## Project Structure & Module Organization

This Java 17/Spring Boot 4 starter prevents duplicate invocations through an AOP interceptor and an atomic storage contract. Production code is under `src/main/java/io/github/ingrowthly/noduplicate`: `annotation` contains the public API, `component` owns interception, `store` provides the extension contract and Redis adapter, `util` generates fingerprints, and `config` contains auto-configuration and properties. Auto-configuration registration lives in `src/main/resources/META-INF/spring/`.

Tests mirror production packages under `src/test/java`. Keep public extension points small and avoid leaking Redis implementation details into the aspect.

## Build, Test, and Development Commands

Always use the checked-in Gradle wrapper:

- `./gradlew clean build` compiles, tests, checks coverage, and creates library artifacts.
- `./gradlew test` runs the fast, service-free JUnit suite.
- `./gradlew test --tests '*NoDuplicateAspectTest'` runs one test class.
- `./gradlew jacocoTestReport` creates HTML and XML coverage reports.
- `./gradlew publishToMavenLocal` publishes a local development artifact.
- `./gradlew generatePomFileForMavenPublication` generates the Central Portal POM for inspection.

Tests must not require Redis, Docker, network access, or timing sleeps.

## Coding Style & Naming Conventions

Use four-space indentation, same-line braces, UTF-8, and one public type per file. Types use `UpperCamelCase`; methods and fields use `lowerCamelCase`; tests end in `Test`. Compilation enables `-parameters`, all practical javac warnings, and `-Werror`. Prefer JDK and Spring APIs over adding utility dependencies. Keep components immutable where practical and document public APIs with concise Javadoc.

## Testing Guidelines

JUnit 5, AssertJ, and Mockito are available through Spring Boot Test. Test behavior at component boundaries: deterministic fingerprints, duplicate rejection, TTL conversion, safe token release, and conditional auto-configuration. Use in-memory implementations for aspect tests and mocks only at external adapters. `check` enforces at least 80% line coverage.

## Commit & Pull Request Guidelines

Use concise Conventional Commit-style subjects, matching history: `feat(*)`, `fix(*)`, `test(*)`, or `docs(*)`. Pull requests should explain behavior and compatibility changes, call out Redis key or configuration impacts, link issues, and report the exact Gradle verification command. Update `README.md` whenever public annotations, properties, requirements, or extension contracts change, and record user-visible changes under `Unreleased` in `CHANGELOG.md`. Follow `.github/RELEASING.md` for releases; never commit Central Portal or signing credentials.
