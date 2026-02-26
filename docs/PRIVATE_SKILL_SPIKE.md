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
  - [Credential Storage Alternatives](#credential-storage-alternatives)
  - [IAM Single-User Configuration](#iam-single-user-configuration)
  - [Recommendations](#recommendations)
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

> ⚠️ **Legal and Data Liability Warning:** This approach stores your own Golf Canada credentials inside a Lambda environment variable. You are personally responsible for the security of those credentials. If your GitHub account, AWS account, or Lambda function is compromised and credentials leak, you bear the liability for any resulting harm. **This must only ever be used with your own personal credentials.** Deploying a version of this skill that asks other users to enter their Golf Canada login credentials would create unacceptable legal and data-liability exposure — you would be handling third-party credentials without a formal data processing agreement with Golf Canada or your users, and without any consent mechanism. Golf Canada's own terms of service likely prohibit such use of their platform.

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

| Risk | Severity | Mitigation | Completed |
|------|----------|------------|-----------|
| Credentials exposed if GitHub account is compromised | High → Medium | Use a strong password and 2FA on your GitHub account. Consider a dedicated Golf Canada account for this purpose. | ✅ 2FA enabled — reduces effective severity to Medium |
| Credentials visible in Lambda environment variables to any IAM user with `GetFunctionConfiguration` permission | Medium | Apply least-privilege IAM policies; restrict who can view Lambda configuration. Single-user accounts should avoid creating additional IAM users. See [IAM Single-User Configuration](#iam-single-user-configuration). | ⬜ See setup guide |
| Credentials exposed in Lambda logs if accidentally logged | Medium | The code is careful not to log credentials; maintain this practice. | ✅ Code review confirms no credential logging |
| Token rotation — if Golf Canada password changes, Lambda stops working | Low | Update `GOLF_CANADA_PASSWORD` GitHub Secret and re-deploy. | ✅ Re-deploy process documented |
| A malicious workflow in a fork could exfiltrate secrets | High | Only run workflows from protected branches in the original repository. GitHub Actions secrets are scoped per-repository — a fork's workflow uses only the fork's own secrets. `workflow_dispatch` cannot be triggered by external contributors. The `deploy.yml` workflow enforces a branch restriction (`main` only). | ✅ Branch restriction added to workflow |
| Credentials in Lambda environment are not re-encrypted by default | Low | Lambda encrypts environment variables using the AWS-managed key by default. Use a customer-managed KMS key for higher assurance. See [Credential Storage Alternatives](#credential-storage-alternatives). | ⬜ Optional — see alternatives |

### Credential Storage Alternatives

The table above shows the baseline approach (credentials in Lambda env vars via GitHub Secrets). The following alternatives offer stronger security at the cost of additional complexity:

#### Option 1: Customer-Managed KMS Key for Lambda Environment Variables

AWS Lambda supports encrypting environment variables using a **customer-managed KMS key** (CMK) instead of the default AWS-managed key. This adds an extra layer of encryption:

1. Create a KMS key in AWS Key Management Service
2. In Lambda → Configuration → Environment variables, click **Encryption configuration** and select your CMK
3. Update the IAM role to allow `kms:Decrypt` for the Lambda execution role

This means an attacker who can view the Lambda configuration still cannot read the plaintext credentials without also having KMS key access.

**Reference:** [Lambda environment variable encryption](https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html#configuration-envvars-encryption)

#### Option 2: AWS Secrets Manager

Instead of passing credentials as environment variables, store them in [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html). The Lambda function retrieves them at runtime:

1. Create a secret in AWS Secrets Manager:
   ```bash
   aws secretsmanager create-secret \
     --name "golf-canada-alexa/credentials" \
     --secret-string '{"username":"your@email.com","password":"yourpassword"}'
   ```
2. Add `secretsmanager:GetSecretValue` permission to the Lambda execution role
3. Modify `StaticCredentialInterceptor` to call `GetSecretValue` on startup instead of reading env vars

**Pros:** Credentials are not visible in Lambda configuration at all; supports automatic rotation; full audit trail  
**Cons:** Additional AWS cost; adds latency on cold start; more complex setup

**Reference:** [AWS Secrets Manager User Guide](https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html)

#### Option 3: Application-Level Credential Encoding

The `GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` could be stored obfuscated in environment variables, with the application decoding them at runtime using a key that is only embedded in the compiled JAR. This approach is called "security through obscurity":

- An attacker who gets the env var value sees only encoded bytes
- To recover credentials they must also obtain the Lambda JAR, reverse-engineer it, and find the decoding key

This provides **marginal** additional protection — it raises the bar for a casual attacker, but a determined attacker with access to both the Lambda configuration and the JAR can always recover the credentials. **AWS Secrets Manager is preferred.** This option is documented here for completeness only.

### IAM Single-User Configuration

For a personal AWS account with a single user, the key concern is ensuring that no additional IAM user (created by mistake, or in future) can read the Lambda environment variables.

**Step 1: Identify your IAM user or root account**

For a personal account, you likely operate as either:
- The root account (not recommended for day-to-day use) — secure with MFA
- A single IAM administrator user — ensure MFA is enabled

**Step 2: Verify no unnecessary IAM users exist**

In the AWS IAM Console → Users, confirm only your own user is listed.

**Step 3: Restrict the GitHub Actions deployment role**

The IAM role used by GitHub Actions for deployment (`AWS_ROLE_ARN`) should have **only the permissions needed for deployment** — it does not need `lambda:GetFunctionConfiguration`. The trust policy should already restrict this role to your specific repository and branch (see [docs/DEPLOYMENT.md](DEPLOYMENT.md)).

If you want to explicitly deny `GetFunctionConfiguration` from the deployment role:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Deny",
      "Action": [
        "lambda:GetFunctionConfiguration"
      ],
      "Resource": "*"
    }
  ]
}
```

Add this as an inline policy on the GitHub Actions role. This means even the deployment workflow cannot read back the credentials it just deployed.

**Step 4: Enable CloudTrail**

Enable [AWS CloudTrail](https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-user-guide.html) to log all calls to `GetFunctionConfiguration`. If credentials are read by an unexpected actor, you will have an audit trail.

**Reference:** [AWS IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)

**Versus Account Linking:**

Account Linking avoids storing credentials entirely — the user authenticates through the Alexa app, and Alexa manages the OAuth tokens. However, publishing this skill publicly in Account Linking mode also introduces significant data and legal concerns:

- This skill acts as an OAuth *wrapper* around Golf Canada's authentication — it is not a first-party integration. Publishing it publicly would mean collecting Golf Canada credentials from users on your infrastructure, creating liability without a formal agreement with Golf Canada.
- Maintaining a public version of the skill also means ongoing AWS service costs that are harder to control, as any Alexa user could enable the skill.
- Golf Canada could terminate access to their API at any time, making a public skill non-functional.

The static approach is a deliberate trade-off of security for simplicity in **single-user personal deployments only**.

### Recommendations

1. **Use a dedicated Golf Canada account** for the personal Alexa skill deployment, not your primary account. This limits exposure if credentials are compromised.
   > ⚠️ **Beta testing is not feasible for other Golf Canada users.** Inviting other users to test the skill in standard Account Linking mode would require them to enter their credentials into an OAuth wrapper you control — this creates the same legal and data-liability risks described above. The skill should remain personal.
2. **Never commit credentials** to source control. Always use GitHub Secrets.
3. **Restrict Lambda IAM access** so only authorized users can view the Lambda configuration. See [IAM Single-User Configuration](#iam-single-user-configuration) above for step-by-step instructions.
4. **Enable AWS CloudTrail** to audit who accesses the Lambda configuration.
5. **Keep the skill in Development mode** and do not publish it. A published skill would be available to all Alexa users but would not have valid credentials for their accounts — and would create the legal/liability problems noted above.
6. **Rotate credentials periodically** — update the GitHub Secret and redeploy.
7. **Consider AWS Secrets Manager** for stronger credential protection. See [Credential Storage Alternatives](#credential-storage-alternatives) above.

---

## Implementation Summary

The following changes were made to support static individual-user deployment:

> **Static mode is a developer/personal-use mode only.** When `GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` are **not** set (the default), the `StaticCredentialInterceptor` is a complete no-op and the skill falls back to the standard Alexa Account Linking flow. This means the same deployment can evolve: you can use static mode for personal testing today, and later add the full OAuth Lambda for a proper Account Linking flow without removing any code.

### New: `StaticCredentialInterceptor`

**File:** `src/main/kotlin/kjd/golfcanada/alexa/interceptor/StaticCredentialInterceptor.kt`

A new request interceptor that:
- Checks for `GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` environment variables.
- **If credentials are absent (default):** is a no-op, preserving the normal OAuth Account Linking flow.
- **If credentials are present:** authenticates with Golf Canada using the Password grant type, caches the token in-memory with TTL-based expiration, and stores the token in request attributes for downstream use.

### Modified: `AuthenticationRequestInterceptor`

**File:** `src/main/kotlin/kjd/golfcanada/alexa/interceptor/AuthenticationRequestInterceptor.kt`

Updated `isAuthenticated()` to also check request attributes for a static token, so that requests with a static token are treated as authenticated. When no static token is present and no Alexa account-linking token is present, the normal unauthenticated flow applies.

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

The deployment workflow now:
- Passes `GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` secrets as SAM parameters (these are optional — if the secrets are not set, the parameters default to empty strings and static mode is disabled).
- Restricts deployment to the `main` branch only via a job-level `if` condition, preventing deployment from topic branches and protecting against secret exfiltration from PRs.

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
