# Security Policy

## Supported Versions

Security fixes are released for the current minor release line. Older releases do not receive security backports.

| Version | Supported |
| ------- | --------- |
| 0.2.x   | Yes       |
| < 0.2.0 | No        |

## Reporting a Vulnerability

Please do **not** report undisclosed security vulnerabilities through public GitHub issues, pull requests, or discussions.

Use GitHub's [private vulnerability reporting](https://github.com/tinywind/BABEL-CODEGEN/security/advisories/new) for this repository to submit a report confidentially. If you cannot use GitHub Security Advisories, contact the maintainer directly at `tinywind0@gmail.com`.

When reporting an issue, please include as much of the following as possible:

- Affected module (`babel-codegen`, `babel-codegen-maven`, or `babel-codegen-gradle-plugin`) and version
- Java, Maven, or Gradle version used
- A minimal reproduction or proof of concept
- Description of the potential impact and any suggested mitigation

## Response Process

- We aim to acknowledge receipt of a vulnerability report within 7 days.
- Validated issues are tracked privately through a GitHub Security Advisory while a fix is prepared and verified.
- Once a patched release is available, we will publish the advisory and credit reporters who wish to be acknowledged.

## Security Model Notes

`babel-codegen` compiles JavaScript and JSX at build time using `@babel/standalone` embedded in GraalJS. GraalJS contexts deny Java host access and host class lookup, and each context is closed after execution. Reports involving GraalJS sandbox escapes, unsafe file path handling, configuration XML parsing issues, or vulnerable bundled and transitive dependencies are in scope.
