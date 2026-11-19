# Golf Canada Alexa Skill (Unofficial)

An Amazon Alexa skill for accessing Golf Canada membership and handicap information.

> This is an entirely unofficial application and may be shut down at any time because of
> how it interacts with Golf Canada. For information, please email
> **ken.j.davidson@live.ca**.

## Overview

The project is a Kotlin/JVM Alexa skill deployed to AWS Lambda with AWS SAM. It includes a
skill handler and an optional OAuth authentication function for Alexa Account Linking.
Personal deployments can instead use static Golf Canada credentials; see the
[single-user deployment notes](docs/PRIVATE_SKILL_SPIKE.md) before enabling that mode.

Currently available features include handicap lookup (including friends), membership status,
and favorite courses. Score history and score posting are planned. See the
[complete feature list](docs/AVAILABLE_FEATURES.md).

## Documentation

- [Contribution guide](CONTRIBUTION.md) — development setup, testing, and pull requests
- [Deployment guide](docs/DEPLOYMENT.md) — AWS and GitHub Actions deployment
- [Architecture](docs/ARCHITECTURE.md) — system components and request flows
- [Available features](docs/AVAILABLE_FEATURES.md)
- [Private, single-user deployment](docs/PRIVATE_SKILL_SPIKE.md)
- [Privacy policy](PRIVACY_POLICY.md)
- [Terms of service](TERMS_OF_SERVICE.md)
