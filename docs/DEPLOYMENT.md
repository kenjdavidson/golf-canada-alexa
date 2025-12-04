# Deployment Guide

This document provides detailed instructions for deploying the Golf Canada Alexa skill Lambda functions using GitHub Actions.

> **Quick Start:** For a condensed setup guide, see [.github/DEPLOYMENT_QUICKSTART.md](../.github/DEPLOYMENT_QUICKSTART.md)

## Table of Contents

- [Overview](#overview)
- [GitHub Actions Workflow](#github-actions-workflow)
- [Required GitHub Secrets](#required-github-secrets)
- [Deployment Process](#deployment-process)
- [Tag-Based Deployment Strategy](#tag-based-deployment-strategy)

## Overview

The project uses GitHub Actions to automatically deploy Lambda functions to AWS when tags are pushed to the repository. 

**Important Note:** Due to the nature of AWS SAM and CloudFormation, the deployment always updates the entire stack, which includes both Lambda functions. However, the tag-based naming strategy helps with:

1. **Version tracking** - Identify which component triggered the deployment
2. **Release management** - Organize releases by functional area
3. **Change tracking** - Understand deployment history at a glance

### Tag-Based Deployment Triggers

The workflow supports three tag patterns:

1. **General release tags** (e.g., `v1.0.0`) - For releases affecting both functions or major updates
2. **Authentication-focused tags** (e.g., `auth-v1.0.0`) - For releases primarily affecting the authentication function
3. **Skill-focused tags** (e.g., `skill-v1.0.0`) - For releases primarily affecting the Alexa skill function

## GitHub Actions Workflow

The deployment workflow is defined in `.github/workflows/deploy.yml` and is triggered when tags matching specific patterns are pushed to the repository.

### Workflow Features

- **Automated builds** - Uses Gradle to build the Java/Kotlin application
- **SAM deployment** - Leverages AWS SAM CLI for Lambda deployment via CloudFormation
- **Secure authentication** - Uses AWS OIDC for secure, credential-less authentication
- **Tag-based versioning** - Organizes releases by component using tag prefixes
- **Parameter management** - All sensitive configuration is stored in GitHub Secrets
- **Full stack deployment** - Updates both Lambda functions with each deployment to maintain consistency

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
The name of the CloudFormation stack that will be created or updated.

**Example:** `golf-canada-alexa`

**Note:** This should match the stack name in your `samconfig.toml` if you're also doing manual deployments.

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

### Step 2: Create and Push a Tag

The deployment workflow is triggered by pushing tags to the repository. The tag name determines which functions will be deployed.

#### General Release (Both Functions)

For general releases, major updates, or changes affecting both functions:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Any tag starting with `v` (e.g., `v1.0.0`, `v1.1.0`, `v2.0.0-beta`) will trigger a deployment.

#### Authentication-Focused Release

For releases primarily related to authentication function changes:

```bash
git tag auth-v1.0.0
git push origin auth-v1.0.0
```

Tags starting with `auth-v` indicate authentication-focused deployments (both functions are still deployed).

#### Skill-Focused Release

For releases primarily related to Alexa skill function changes:

```bash
git tag skill-v1.0.0
git push origin skill-v1.0.0
```

Tags starting with `skill-v` indicate skill-focused deployments (both functions are still deployed).

**Note:** Regardless of the tag pattern used, AWS SAM deploys the entire CloudFormation stack, which includes both Lambda functions. The tag naming convention helps with release organization and tracking which component was the primary focus of the release.

### Step 3: Monitor Deployment

1. Go to the **Actions** tab in your GitHub repository
2. Click on the workflow run that was triggered by your tag
3. Monitor the deployment progress in real-time
4. Check for any errors in the workflow logs

### Step 4: Verify Deployment

After successful deployment, verify that your functions are working:

1. **Check AWS Console:**
   - Navigate to AWS Lambda in your configured region
   - Verify that your functions are updated with the latest code

2. **Test Authentication Function:**
   - The deployment outputs will show the Function URL
   - Test the `/login` endpoint to ensure the authentication flow works

3. **Test Alexa Skill:**
   - Use the Alexa Developer Console test simulator
   - Or test with an Alexa-enabled device linked to your developer account

## Tag-Based Deployment Strategy

The deployment workflow uses semantic versioning with prefixes to organize releases by their primary focus:

| Tag Pattern | Example | Primary Focus | Actual Deployment |
|-------------|---------|---------------|-------------------|
| `v*` | `v1.0.0`, `v2.1.3` | General release | Both functions |
| `auth-v*` | `auth-v1.0.0` | Authentication changes | Both functions |
| `skill-v*` | `skill-v1.0.0` | Skill changes | Both functions |

**Important:** All tag patterns deploy the complete stack (both Lambda functions). The tag prefix is organizational and helps track which component motivated the release.

### Recommended Versioning Strategy

1. **Major releases** (breaking changes): `v2.0.0`
2. **Minor releases** (new features): `v1.1.0`
3. **Patch releases** (bug fixes): `v1.0.1`
4. **Function-specific hotfixes**: `auth-v1.0.1`, `skill-v1.0.1`

### Example Deployment Scenarios

**Scenario 1: Initial Production Release**
```bash
git tag v1.0.0
git push origin v1.0.0
# Deploys complete stack - both authentication and skill functions
```

**Scenario 2: Authentication Security Patch**
```bash
git tag auth-v1.0.1
git push origin auth-v1.0.1
# Deploys complete stack - focus is on authentication fixes
# Both functions are updated to maintain stack consistency
```

**Scenario 3: New Skill Feature**
```bash
git tag skill-v1.1.0
git push origin skill-v1.1.0
# Deploys complete stack - focus is on skill enhancements
# Both functions are updated to maintain stack consistency
```

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

For more details, see the [AWS Deployment](../README.md#aws-deployment) section in the main README.

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
