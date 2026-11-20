# Deployment Guide

This document describes the available deployment paths and their current behavior.

> **Quick Start:** For a condensed setup guide, see [.github/DEPLOYMENT_QUICKSTART.md](../.github/DEPLOYMENT_QUICKSTART.md)

## Table of Contents

- [Overview](#overview)
- [GitHub Actions Workflow](#github-actions-workflow)
- [Required GitHub Secrets](#required-github-secrets)
- [Deployment Process](#deployment-process)
- [Deployment Targets](#deployment-targets)
- [Undeploying](#undeploying)
- [Follow-up Issues](#follow-up-issues)

## Overview

The project deploys Lambda functions to AWS using SAM and CloudFormation. GitHub Actions
deployment is manual; there is no deploy-on-push or deploy-on-release automation.

The workflow offers `both`, `authentication`, and `skill` targets. Their actual behavior and
limitations are described in [Deployment Targets](#deployment-targets).

## GitHub Actions Workflow

The deployment workflow is defined in `.github/workflows/deploy.yml` and is triggered manually
from the GitHub Actions interface. It runs only in the original repository and only when the
selected ref is `main` or a version tag beginning with `v`. Selecting another branch or a
fork will not deploy.

### Workflow Features

- **Manual trigger** - Initiated on-demand from the GitHub Actions UI
- **Targeted SAM builds** - The dropdown selects which function(s) to build; see [Deployment Targets](#deployment-targets) for stack behavior
- **Automated builds** - Uses Gradle to build the Java/Kotlin application
- **SAM deployment** - Leverages AWS SAM CLI for Lambda deployment via CloudFormation
- **Secure authentication** - Uses AWS OIDC for secure, credential-less authentication
- **Parameter management** - All sensitive configuration is stored in GitHub Secrets

## Required GitHub Secrets

To enable automated deployments, you must configure the following secrets in your GitHub repository:

### Navigation to Secrets

1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** to add each secret

### AWS Authentication Secrets

#### `AWS_ROLE_ARN` (Required)
The ARN of the IAM role that GitHub Actions will assume for deployment.

**Format:** `arn:aws:iam::ACCOUNT_ID:role/ROLE_NAME`

**Example:** `arn:aws:iam::123456789012:role/GitHubActionsDeploymentRole`

**Setup Instructions:**

**Step 1: Create GitHub OIDC Identity Provider in AWS**

First, you need to add GitHub as an identity provider in AWS IAM (only needed once per AWS account):

1. Go to AWS IAM Console → **Identity providers** → **Add provider**
2. Select **OpenID Connect**
3. Configure:
   - **Provider URL**: `https://token.actions.githubusercontent.com`
   - **Audience**: `sts.amazonaws.com`
4. Click **Add provider**

**Step 2: Create IAM Role for GitHub Actions**

1. Go to AWS IAM Console → **Roles** → **Create role**
2. Select **Web identity** as the trusted entity type
3. Choose:
   - **Identity provider**: `token.actions.githubusercontent.com`
   - **Audience**: `sts.amazonaws.com`
4. Click **Next** and attach the following managed policies:
   - `AWSCloudFormationFullAccess` - For CloudFormation stack management
   - `IAMFullAccess` - For creating Lambda execution roles
   - `AWSLambda_FullAccess` - For Lambda function management
   - `AmazonS3FullAccess` - For deployment artifact storage
   
   **Note:** In production, you should create a custom policy with least-privilege permissions instead of using full access policies.

5. Name your role (e.g., `GitHubActionsDeploymentRole`) and create it

**Step 3: Update Trust Policy**

After creating the role, update its trust policy to restrict access to your specific repository:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:YOUR_GITHUB_OWNER/YOUR_REPO_NAME:*"
        }
      }
    }
  ]
}
```

Replace:
- `ACCOUNT_ID` with your AWS account ID
- `YOUR_GITHUB_OWNER` with your GitHub username or organization name
- `YOUR_REPO_NAME` with your repository name (e.g., `golf-canada-alexa`)

**Step 4: Copy the Role ARN**

After creating the role, copy its ARN (e.g., `arn:aws:iam::123456789012:role/GitHubActionsDeploymentRole`) to use as the `AWS_ROLE_ARN` secret in GitHub.

For more details, see: [Configuring OpenID Connect in Amazon Web Services](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)

#### `AWS_REGION` (Required)
The AWS region where the Lambda functions will be deployed.

**Example:** `us-east-1`

**Recommendation:** Use `us-east-1` for Alexa skills as it's the primary region for Alexa services.

### Deployment Configuration Secrets

#### `STACK_NAME` (Required)
The name of the CloudFormation stack that will be created or updated. The GitHub Actions
workflow uses this secret directly; the `samconfig.toml` default is for local SAM commands.

**Example:** `golf-canada-alexa`

### Application Configuration Secrets

#### `GOLF_CANADA_CLIENT_ID` (Required)
The OAuth Client ID for Golf Canada authentication.

**Description:** This is obtained from Golf Canada's OAuth provider configuration and is used by the authentication Lambda function to facilitate account linking.

**Security:** This value is sensitive and should not be committed to source code.

#### `GOLF_CANADA_CLIENT_SECRET` (Required)
The OAuth Client Secret for Golf Canada authentication.

**Description:** The secret key paired with the Client ID for OAuth authentication with Golf Canada.

**Security:** This is highly sensitive and must be kept secure. Never commit this to source code or logs.

#### `ALEXA_SKILL_ID` (Required)
The Alexa Skill ID from the Amazon Alexa Developer Console.

**Format:** `amzn1.ask.skill.XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX`

**Example:** `amzn1.ask.skill.12345678-1234-1234-1234-123456789012`

**Description:** This identifies your specific Alexa skill in the Amazon ecosystem and is used to validate requests to the skill Lambda function.

**How to find it:**
1. Go to [Alexa Developer Console](https://developer.amazon.com/alexa/console/ask)
2. Select your skill
3. The Skill ID is displayed at the top of the skill dashboard

### Optional Personal Deployment Secrets

`GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` are optional and must be provided together
to enable static credentials mode for a personal, single-user deployment. Leave both unset
for standard Alexa Account Linking. Static credentials are passed to the skill Lambda as
environment variables; do not use this mode to handle other users' credentials. See the
[single-user deployment notes](PRIVATE_SKILL_SPIKE.md) for security considerations.

The workflow passes both values to SAM even when they are not configured, and the template
defaults them to empty strings.

### Testing Secrets (Optional)

These secrets are used by the test workflow (`.github/workflows/test.yml`) and are not required for deployment:

#### `TEST_USERNAME` (Optional)
Your Golf Canada username for integration testing.

#### `TEST_PASSWORD` (Optional)
Your Golf Canada password for integration testing.

**Note:** If these are not configured, integration tests will be skipped, and only unit tests will run.

## Deployment Process

### Step 1: Configure Secrets

Ensure all required secrets listed above are configured in your GitHub repository settings.

### Step 2: Trigger Deployment

The deployment workflow is triggered manually from the GitHub Actions interface.

1. Navigate to your repository on GitHub
2. Click on the **Actions** tab
3. Select the **Deploy to AWS Lambda** workflow from the left sidebar
4. Click the **Run workflow** button (on the right side)
5. Choose the deployment target from the dropdown:
   - **both** - Deploy both Lambda functions (default)
   - **authentication** - Builds the authentication function; see [Deployment Targets](#deployment-targets)
   - **skill** - Builds the skill function and removes the authentication backend from the stack
6. Select `main` or a version tag beginning with `v`. The workflow will not run from `master`,
   another branch, or a fork.
7. Click **Run workflow** to start the deployment

The workflow builds with Java 21 and Gradle, builds the selected SAM function(s), assumes the
configured AWS role through GitHub OIDC, and deploys a CloudFormation stack using the
`STACK_NAME` and `AWS_REGION` secrets.

### Step 3: Monitor Deployment

1. Go to the **Actions** tab in your GitHub repository
2. Click on the workflow run that you triggered
3. Monitor the deployment progress in real-time
4. Check for any errors in the workflow logs

### Step 4: Verify Deployment

After successful deployment, verify that your functions are working:

1. **Check CloudFormation outputs** in the AWS Console, or run:
   ```shell
   aws cloudformation describe-stacks \
       --stack-name "$STACK_NAME" \
       --region "$AWS_REGION" \
       --query 'Stacks[0].Outputs'
   ```
   The workflow log does not print the Lambda Function URL or skill ARN.

2. **Test Authentication Function:**
   - If the authentication function is included in the stack, use its Function URL output
   - Test the `/login` endpoint to ensure the authentication flow works

3. **Test Alexa Skill:**
   - Use the Alexa Developer Console test simulator
   - Or test with an Alexa-enabled device linked to your developer account

## Troubleshooting

### Deployment Fails with Authentication Error

**Problem:** Workflow fails with "Unable to assume role"

**Solution:**
1. Verify `AWS_ROLE_ARN` secret is correct
2. Ensure the IAM role trust policy includes your repository
3. Confirm the OIDC identity provider is configured in AWS

### Deployment Succeeds but Functions Don't Work

**Problem:** Deployment completes but Lambda functions fail at runtime

**Solution:**
1. Check Lambda function logs in CloudWatch
2. Verify all secrets (`GOLF_CANADA_CLIENT_ID`, `GOLF_CANADA_CLIENT_SECRET`, `ALEXA_SKILL_ID`) are correct
3. Ensure the SSL certificate layer is properly deployed
4. Verify function permissions and environment variables

### SAM Build Fails

**Problem:** `sam build` command fails during deployment

**Solution:**
1. Verify `template.yaml` is valid
2. Check that Gradle build completed successfully
3. Ensure all dependencies are properly packaged

## Deployment Targets

The SAM template always includes `GolfCanadaAlexaSkillFunction`. The authentication Lambda,
its Function URL, and the certificate layer are controlled by the `DeployAuthentication`
parameter.

- **`both`** builds the full SAM application and deploys with authentication enabled.
- **`authentication`** builds only the authentication function but still deploys the SAM
  template with authentication enabled. Because the skill function remains in the template,
  this does not guarantee an authentication-only stack update; see the follow-up issue below.
- **`skill`** builds the skill function and deploys with authentication disabled. CloudFormation
  removes the authentication function, its Function URL, and the certificate layer if they
  already exist in the stack. This target is destructive to the account-linking backend and is
  intended for a static, personal deployment.

`ClientId`, `ClientSecret`, and `SkillId` remain SAM parameters and are passed by the workflow
for every target. With `skill`, set both optional static credential secrets to enable personal
static mode; without them, the skill still expects Account Linking, whose authentication
backend this target removes.

## Alexa Skill Configuration

After deployment, retrieve the stack outputs from CloudFormation. The GitHub Actions workflow
does not print them.

1. In the Alexa Developer Console, set the skill endpoint to the
   `GolfCanadaAlexaSkillFunctionArn` output.
2. When deploying with the authentication function, configure Account Linking:
   - **Authorization URI**: `{GolfCanadaAuthenticationFunctionUrl}/login`
   - **Access Token URI**: `{GolfCanadaAuthenticationFunctionUrl}/authToken`
   - **Client ID / Client Secret**: the values used for `ClientId` and `ClientSecret`
   - **Client Authentication Scheme**: HTTP Basic
   - **Scope**: `address email offline_access openid phone profile roles`
3. Upload the interaction model from `model/model.json`.

For static personal deployments, configure no Account Linking; the skill uses the configured
Golf Canada credentials directly. Do not use static mode for a shared skill.

## Undeploying

The separate `.github/workflows/undeploy.yml` workflow is also manually triggered and restricted
to the original repository's `main` branch or a `v*` tag. It offers:

- **`authentication`** — removes the authentication Lambda, Function URL, and certificate layer
  while leaving the skill Lambda in the stack.
- **`stack`** — deletes the entire CloudFormation stack and all its resources.

Both actions use the AWS OIDC role and the `AWS_REGION` and `STACK_NAME` secrets.

## Follow-up Issues

The following discrepancies are present in the current workflow or template and need follow-up:

- [ ] Fix or clarify the `authentication` deployment target: its SAM build targets one function,
  but deployment still includes the skill function in the template.
- [ ] Make the destructive behavior of the `skill` target explicit in the workflow and confirm
  that disabling Account Linking's backend is the intended deployment behavior.
- [ ] Fix the authentication certificate layer's `ContentUri` in `template.yaml`: it points to
  `./GolfCanadaAuthenticationCertLayer`, which is not present in the repository checkout (the
  certificate assets are under `layers/`). Verify a full SAM build and deployment after fixing.
- [ ] Include the deployed Function URL and skill Lambda ARN in the GitHub Actions output, or
  otherwise make the workflow's deployment outputs discoverable without a separate AWS query.

## Manual Deployment (Alternative)

If you prefer to deploy manually or need to deploy from your local machine:

1. **Configure AWS CLI:**
   ```bash
   aws configure
   ```

2. **Build and deploy:**
   ```bash
   ./gradlew packageJar
   sam build
   sam deploy --guided
   ```

3. **Follow the prompts** to configure deployment parameters

For architecture and component details, see the [architecture guide](ARCHITECTURE.md).

## Security Best Practices

1. **Use OIDC Authentication:** The workflow uses AWS OIDC instead of long-lived access keys
2. **Least Privilege:** Create custom IAM policies with minimum required permissions
3. **Secret Rotation:** Regularly rotate OAuth client secrets and update GitHub secrets
4. **Branch Protection:** Protect the main branch and require PR reviews before merging
5. **Tag Protection:** Consider protecting version tags to prevent accidental deletions
6. **Audit Logs:** Monitor AWS CloudTrail for deployment activities

## Additional Resources

- [AWS SAM Documentation](https://docs.aws.amazon.com/serverless-application-model/)
- [GitHub Actions - AWS Integration](https://github.com/aws-actions)
- [Configuring OIDC in AWS](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- [Amazon Alexa Documentation](https://developer.amazon.com/en-US/docs/alexa/custom-skills/host-a-custom-skill-as-an-aws-lambda-function.html)
