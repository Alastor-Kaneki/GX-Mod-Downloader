# Security Policy

## Reporting a vulnerability

Do not open a public issue for a vulnerability that could expose users to malicious package execution, unsafe redirects, path traversal, or arbitrary file writes. Contact the repository owner privately through the security-reporting options available on the GitHub profile or repository.

Include the affected commit, platform, reproduction steps, and the least-sensitive proof needed to demonstrate the issue.

## Design constraints

GX Mod Downloader must remain a download-only client:

- Never automatically install or activate a GX Mod.
- Never execute package code, CSS, shaders, or scripts.
- Never weaken the official GX host allowlist to make a failing download work.
- Never commit Android signing keys, passwords, API tokens, or release credentials.
- Treat changes to GX Store response parsing and CRX handling as security-sensitive.
