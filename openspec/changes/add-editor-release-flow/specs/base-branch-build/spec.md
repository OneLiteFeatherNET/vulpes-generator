# Spec Delta

## Purpose

Verifies that a project's snapshot works with a real `vulpes-base` branch before the generator hands out any content. Every local build and every release is generated and compiled against the branch the caller names, so that a broken snapshot is refused with its compiler diagnostics instead of being published.

## ADDED Requirements

### Requirement: Base branch selection
The system SHALL accept a `baseBranch` for every local build and every release. When the caller omits it, the system SHALL use `develop`. The system SHALL accept only a branch that exists in the `vulpes-base` repository; a name that does not exist SHALL be refused with `422`. When the branch list cannot be read, the system SHALL answer `502` and SHALL NOT assume that the branch exists.

#### Scenario: Default branch
- **WHEN** a local build is requested without `baseBranch`
- **THEN** the system builds against the branch `develop`

#### Scenario: Unknown branch
- **WHEN** a release names a branch that `vulpes-base` does not have
- **THEN** the response status is `422` and nothing is cloned, built or published

#### Scenario: Branch list unavailable
- **WHEN** the list of branches cannot be read from GitHub
- **THEN** the response status is `502` and the message names the repository

### Requirement: Shallow clone and full commit SHA
The system SHALL clone the selected branch with a depth of one into a temporary directory. The system SHALL resolve the commit the branch points to at clone time and SHALL use that commit for everything that follows. The commit SHALL always be the full 40-character lower-case hexadecimal SHA-1; abbreviated SHAs SHALL NOT appear in a response, in `release.json`, in a zip or as a cache key. When the clone fails, the system SHALL answer `502` and build nothing.

#### Scenario: Commit recorded
- **WHEN** the clone of the branch succeeds
- **THEN** the system knows one commit SHA and returns the same full 40-character SHA in the response of a release

#### Scenario: Clone fails
- **WHEN** the repository cannot be cloned
- **THEN** the response status is `502` and nothing is published

### Requirement: Code generation from the snapshot
The system SHALL generate the Java sources of the project's snapshot with the code generator of the Vulpes Gradle plugin, consumed as the published library `vulpes-codegen`. The generated sources SHALL be written only into the temporary directory of the request. A snapshot that the generator refuses SHALL be answered with `422` and the generator's message.

#### Scenario: Snapshot generates
- **WHEN** the snapshot of a project is generated
- **THEN** the generated sources are compiled against the cached base of the selected branch

#### Scenario: Generator refuses the snapshot
- **WHEN** the generator rejects the snapshot content
- **THEN** the response status is `422`, the message names the refused content, and nothing is compiled or published

### Requirement: Compilation gate
The system SHALL compile the generated sources in-process against the cached classes and the cached runtime classpath of the selected commit before it builds any output. When the compilation succeeds, the system SHALL continue with the request. When it fails, the system SHALL answer `422` and SHALL publish nothing.

#### Scenario: Compilation fails
- **WHEN** the generated sources do not compile against the branch
- **THEN** the response status is `422` and nothing is published

#### Scenario: Compilation succeeds
- **WHEN** the generated sources compile against the branch
- **THEN** the request continues to the output step

### Requirement: Compilation diagnostics
The system SHALL report every compile error of a refused request as a diagnostic with the file, the line and the message reported by the compiler. The list SHALL be capped at 100 diagnostics, and the response SHALL state when the list was cut.

#### Scenario: Diagnostics listed
- **WHEN** the compiler reports an error at line 12 of `items/Sword.java`
- **THEN** the body lists an entry with that file, that line and the compiler message

#### Scenario: Too many diagnostics
- **WHEN** the compiler reports more than 100 errors
- **THEN** the response lists the first 100 and states that the list was cut

### Requirement: Base build cache per commit
The system SHALL build the `vulpes-base` checkout of a commit once, with the Gradle wrapper of that checkout, and SHALL cache the built `vulpes-base` jar and the runtime dependency jars under the full commit SHA. A later request for the same commit SHALL reuse the cache and SHALL NOT run the Gradle build again. The cache SHALL hold no clone and no generated source.

#### Scenario: Same commit twice
- **WHEN** two requests resolve to the same commit
- **THEN** the base is built once and the second request reuses the cache

#### Scenario: No sources in the cache
- **WHEN** a commit's cache entry exists
- **THEN** the entry contains no `.git` entry and no `.java` file

### Requirement: Base build cache eviction
The system SHALL keep the configured number of most recently used commits in the base build cache and SHALL evict the least recently used commit beyond that number.

#### Scenario: Eviction
- **WHEN** the cache holds the configured number of commits and a new commit is stored
- **THEN** the least recently used commit's cache entry is removed and the next request for it builds the base again

### Requirement: Base build environment
The system SHALL run the Gradle build of a `vulpes-base` checkout with an environment that holds no generator secret. The environment SHALL NOT contain `ONELITEFEATHER_MAVEN_USERNAME`, `ONELITEFEATHER_MAVEN_PASSWORD`, `GITHUB_PASSWORD`, or any variable whose name starts with `VULPES_`.

#### Scenario: Secrets not passed
- **WHEN** the base build of a commit runs
- **THEN** the environment of the Gradle process contains none of the generator secrets or `VULPES_` variables

### Requirement: Read-only repository credential for the base build
The system SHALL pass a dedicated read-only credential, configured as `vulpes.base.maven.username` and `vulpes.base.maven.password`, to the Gradle process as `ORG_GRADLE_PROJECT_OneLiteFeatherRepositoryUsername` and `ORG_GRADLE_PROJECT_OneLiteFeatherRepositoryPassword`. It SHALL NOT pass the publish credential `vulpes.content.maven.*` in its place. When the read-only credential is not configured, the system SHALL pass neither variable.

#### Scenario: Read-only credential passed
- **WHEN** the read-only credential is configured and the base build of a commit runs
- **THEN** the Gradle process receives it under the two `ORG_GRADLE_PROJECT_` names and receives no publish credential

#### Scenario: No read-only credential configured
- **WHEN** no read-only credential is configured and the base build of a commit runs
- **THEN** the environment of the Gradle process contains no `ORG_GRADLE_PROJECT_OneLiteFeatherRepository` variable

### Requirement: Base build time limit
The system SHALL stop the Gradle build of a `vulpes-base` checkout when it runs longer than the configured build time limit, and SHALL answer `502` with a message that names the time limit. The system SHALL run at most one base build at a time in one generator instance. The first build of a new commit is slower and needs more memory than a request that hits the cache; the deployment SHALL size the generator for it (`design.md`, section 9).

#### Scenario: Build too slow
- **WHEN** the base build exceeds the time limit
- **THEN** the response status is `502`, nothing is published and no cache entry is left for that commit

#### Scenario: Two builds at once
- **WHEN** two requests need a base build of different commits at the same time
- **THEN** the second build starts only after the first one has finished

### Requirement: Workspace cleanup
The system SHALL delete the temporary clone and build directories of a request after the request has finished, whether it succeeded or failed. The system MUST NOT keep any clone directory after a response has been sent.

#### Scenario: Successful request
- **WHEN** a local build succeeds
- **THEN** no clone or build directory of the request exists afterwards

#### Scenario: Failed compilation
- **WHEN** the compilation gate refuses a request
- **THEN** no clone or build directory of the request exists afterwards
