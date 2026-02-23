# Private Skill Spike: Static Individual-User Deployment

**Date:** 2026-02-23  
**Issue:** [Look into making this static for an individual user](https://github.com/kenjdavidson/golf-canada-alexa/issues)  
**Status:** ✅ Implemented

## Overview

This document captures the research and decisions made when investigating a "static" deployment mode that allows an individual to fork and deploy this skill for personal use — without requiring the OAuth authentication Lambda (`GolfCanadaAuthenticationFunction`).

---

## Table of Contents

- [Alexa Private Skills: Research](#alexa-private-skills-research)
- [Static Authentication Approach](#static-authentication-approach)
- [Security Risks and Mitigations](#security-risks-and-mitigations)
- [Implementation Summary](#implementation-summary)
- [Setup Guide for Individual Users](#setup-guide-for-individual-users)

---

## Alexa Private Skills: Research

### Are all Alexa skills required to go public?

No. Amazon supports multiple distribution models for Alexa skills:

1. **Development Skills (Personal Use)** — While a skill is in "Development" status in the [Alexa Developer Console](https://developer.amazon.com/alexa/console/ask), it is only accessible to the Amazon account used to create it and any explicitly invited Beta testers. Skills do **not** need to be submitted for certification. This is the recommended approach for this project.

2. **Private Skills (Alexa for Business / Enterprise)** — Amazon offered a [Private Skills](https://developer.amazon.com/en-US/docs/alexa/alexa-for-business/create-and-publish-private-skills.html) distribution model through Alexa for Business (now [Alexa Smart Properties](https://developer.amazon.com/en-US/alexa/alexa-smart-properties)). This allows organizations to deploy skills only to their own devices. This requires an enterprise account and is overkill for personal use.

3. **Beta Testing** — You can invite up to 500 Amazon accounts to test a skill in development via the [Alexa Developer Console Beta Test feature](https://developer.amazon.com/en-US/docs/alexa/devconsole/test-your-skill.html#beta-test). This is useful if you want to share with a small group.

4. **Published Skills** — Skills submitted for certification and published to the Alexa Skill Store are available to all Alexa users.

### Recommendation

For personal use of this skill, keeping the skill in **Development** status is the simplest and most secure option. The skill will be available to the Alexa account registered in your developer account and will not require certification. This is a well-documented and supported approach.

**References:**
- [Alexa Skill Lifecycle](https://developer.amazon.com/en-US/docs/alexa/devconsole/about-the-developer-console.html)
- [Test Your Skill](https://developer.amazon.com/en-US/docs/alexa/devconsole/test-your-skill.html)
- [Private Skills (Alexa for Business)](https://developer.amazon.com/en-US/docs/alexa/alexa-for-business/create-and-publish-private-skills.html)

---

## Static Authentication Approach

### The Problem

The current architecture requires two Lambda functions:
1. **`GolfCanadaAuthenticationFunction`** — An OAuth wrapper that handles Golf Canada login and issues tokens to Alexa Account Linking.
2. **`GolfCanadaAlexaSkillFunction`** — The skill handler.

The `GolfCanadaAuthenticationFunction` is necessary because Golf Canada doesn't implement a standard OAuth Authorization Code flow. The auth Lambda acts as a middleman, presenting a login form and proxying authentication to Golf Canada.

For a single-user personal deployment, maintaining this OAuth wrapper adds complexity. The user's credentials could be provided directly to the Skill Lambda instead.

### The Solution: Static Credentials Mode

When the `GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` environment variables are set on the Skill Lambda, the skill bypasses Alexa Account Linking entirely. Instead:

1. The `StaticCredentialInterceptor` authenticates with Golf Canada using the stored credentials.
2. The resulting access token is cached in-memory for the token's TTL (typically 1 hour).
3. The token is stored in request attributes and used by all handlers just like an Account Linking token.

This eliminates the need for the `GolfCanadaAuthenticationFunction` and its associated Lambda Function URL when deploying for a single user.

### Architecture Comparison

**Standard Mode (Multi-user, Account Linking):**
```
Alexa App → /login → GolfCanadaAuthenticationFunction → Golf Canada login form
User submits → /code → Authenticate → Store token → Redirect with code
Alexa → /authToken → Exchange code for tokens
Alexa Skill Request → GolfCanadaAlexaSkillFunction (uses Account Linking token)
```

**Static Mode (Single-user, no OAuth Lambda needed):**
```
Alexa Skill Request → GolfCanadaAlexaSkillFunction
  └── StaticCredentialInterceptor authenticates using GOLF_CANADA_USERNAME / GOLF_CANADA_PASSWORD
  └── Token cached in-memory for subsequent requests
  └── Handlers use static token (no Account Linking required)
```

---

## Security Risks and Mitigations

### Storing Credentials in GitHub Secrets

GitHub Secrets are encrypted at rest and are not exposed in workflow logs. They are the standard mechanism for managing secrets in GitHub Actions.

**Risks:**

| Risk | Severity | Mitigation |
|------|----------|------------|
| Credentials exposed if GitHub account is compromised | High | Use a strong password and 2FA on your GitHub account. Consider a dedicated Golf Canada account for this purpose. |
| Credentials visible in Lambda environment variables to any IAM user with `GetFunctionConfiguration` permission | Medium | Apply least-privilege IAM policies; restrict who can view Lambda configuration. |
| Credentials exposed in Lambda logs if accidentally logged | Medium | The code is careful not to log credentials; maintain this practice. |
| Token rotation — if Golf Canada password changes, Lambda stops working | Low | Update `GOLF_CANADA_PASSWORD` GitHub Secret and re-deploy. |
| A malicious workflow in a fork could exfiltrate secrets | High | Only run workflows from trusted branches; avoid running workflows from fork PRs against secrets. |
| Credentials in Lambda environment are not re-encrypted | Low | Lambda encrypts environment variables using the AWS-managed key by default. Use a customer-managed KMS key for higher assurance. |

**Versus Account Linking:**

Account Linking avoids storing credentials entirely — the user authenticates through the Alexa app, and Alexa manages the OAuth tokens. The static approach is a deliberate trade-off of security for simplicity in single-user personal deployments.

### Recommendations

1. **Use a dedicated Golf Canada account** for the personal Alexa skill deployment, not your primary account. This limits exposure if credentials are compromised.
2. **Never commit credentials** to source control. Always use GitHub Secrets.
3. **Restrict Lambda IAM access** so only authorized users can view the Lambda configuration.
4. **Enable AWS CloudTrail** to audit who accesses the Lambda configuration.
5. **Keep the skill in Development mode** and do not publish it. A published skill would be available to all Alexa users but would not have valid credentials for their accounts.
6. **Rotate credentials periodically** — update the GitHub Secret and redeploy.

---

## Implementation Summary

The following changes were made to support static individual-user deployment:

### New: `StaticCredentialInterceptor`

**File:** `src/main/kotlin/kjd/golfcanada/alexa/interceptor/StaticCredentialInterceptor.kt`

A new request interceptor that:
- Checks for `GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` environment variables.
- Authenticates with Golf Canada using the Password grant type.
- Caches the token in-memory with TTL-based expiration.
- Stores the token in request attributes for downstream use.

### Modified: `AuthenticationRequestInterceptor`

**File:** `src/main/kotlin/kjd/golfcanada/alexa/interceptor/AuthenticationRequestInterceptor.kt`

Updated `isAuthenticated()` to also check request attributes for a static token, so that requests with a static token are treated as authenticated.

### Modified: `UserProfileInterceptor`

**File:** `src/main/kotlin/kjd/golfcanada/alexa/interceptor/UserProfileInterceptor.kt`

Updated to fall back to the static token from request attributes when no Alexa account-linking token is present.

### Modified: `ApiClientProviderExt`

**File:** `src/main/kotlin/kjd/golfcanada/client/provider/ApiClientProviderExt.kt`

Updated `withAuthenticatedClient()` to fall back to the static token from request attributes.

### Modified: `GolfCanadaAlexaSkill`

**File:** `src/main/kotlin/kjd/golfcanada/alexa/GolfCanadaAlexaSkill.kt`

Added `StaticCredentialInterceptor.fromEnvironment()` as the first request interceptor so it runs before authentication checking.

### Modified: `template.yaml`

Added optional `GolfCanadaUsername` and `GolfCanadaPassword` parameters (default to empty string). When provided, they are passed to the Skill Lambda as `GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` environment variables.

### Modified: `.github/workflows/deploy.yml`

The deployment workflow now passes `GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` secrets as SAM parameters. These are optional — if the secrets are not set, the parameters default to empty strings and static mode is disabled.

---

## Setup Guide for Individual Users

Follow these steps to deploy a personal copy of the Golf Canada Alexa skill:

### 1. Fork the Repository

Fork this repository to your own GitHub account.

### 2. Create an Alexa Skill (Development Only)

1. Log in to the [Alexa Developer Console](https://developer.amazon.com/alexa/console/ask)
2. Create a new custom skill
3. Upload the interaction model from `model/model.json`
4. Note your **Skill ID** (e.g., `amzn1.ask.skill.xxxx...`)
5. Do **not** enable Account Linking (static mode handles auth directly)
6. Set the skill endpoint to the Lambda ARN (configured after deployment)

### 3. Configure GitHub Secrets

In your forked repository, go to **Settings → Secrets and variables → Actions** and add:

| Secret | Description |
|--------|-------------|
| `AWS_ROLE_ARN` | IAM role ARN for GitHub Actions OIDC deployment |
| `AWS_REGION` | AWS region (e.g., `us-east-1`) |
| `STACK_NAME` | CloudFormation stack name (e.g., `golf-canada-alexa-personal`) |
| `ALEXA_SKILL_ID` | Your Alexa Skill ID from step 2 |
| `GOLF_CANADA_USERNAME` | Your Golf Canada login username (email) |
| `GOLF_CANADA_PASSWORD` | Your Golf Canada login password |

> ⚠️ **Security Note:** `GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` will be stored as Lambda environment variables. See [Security Risks and Mitigations](#security-risks-and-mitigations) above.

The `GOLF_CANADA_CLIENT_ID` and `GOLF_CANADA_CLIENT_SECRET` secrets are still required by the SAM template for the authentication Lambda, but you can skip deploying the auth function using the `skill` deployment target in the workflow.

### 4. Deploy Only the Skill Function

In your forked repository:
1. Go to **Actions → Deploy to AWS Lambda**
2. Click **Run workflow**
3. Select **skill** as the deployment target
4. Click **Run workflow**

This deploys only the `GolfCanadaAlexaSkillFunction` — the OAuth Lambda is not needed in static mode.

### 5. Configure the Alexa Skill Endpoint

After deployment, the workflow output will show the Lambda ARN. In the Alexa Developer Console:
1. Navigate to your skill → **Endpoint**
2. Select **AWS Lambda ARN**
3. Enter the `GolfCanadaAlexaSkillFunctionArn` from the deployment output
4. Save and build the skill

### 6. Test

Use the Alexa Developer Console test simulator or your Echo device to test the skill. Since the skill is in Development mode, it's only available to your developer Amazon account.

---

## Related Documentation

- [Alexa Account Linking](https://developer.amazon.com/en-US/docs/alexa/account-linking/add-account-linking.html)
- [Alexa Skill Lifecycle](https://developer.amazon.com/en-US/docs/alexa/devconsole/about-the-developer-console.html)
- [Test Your Skill](https://developer.amazon.com/en-US/docs/alexa/devconsole/test-your-skill.html)
- [Private Skills (Alexa for Business)](https://developer.amazon.com/en-US/docs/alexa/alexa-for-business/create-and-publish-private-skills.html)
- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [AWS Lambda Environment Variable Encryption](https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html#configuration-envvars-encryption)
- [Architecture Documentation](ARCHITECTURE.md)
- [Deployment Guide](DEPLOYMENT.md)
