# Deployment Quick Start Guide

This guide provides a quick reference for setting up automated deployments with GitHub Actions.

## Prerequisites

- AWS Account with administrative access
- GitHub repository with Actions enabled
- Golf Canada OAuth credentials
- Alexa Skill ID
- ASK CLI refresh token and vendor ID for skill model updates

## Quick Setup (5 Steps)

### 1. Configure AWS OIDC Provider (One-time setup)

In AWS IAM Console:
1. Navigate to **Identity providers** → **Add provider**
2. Select **OpenID Connect**
3. Set **Provider URL**: `https://token.actions.githubusercontent.com`
4. Set **Audience**: `sts.amazonaws.com`
5. Click **Add provider**

### 2. Create IAM Role for GitHub Actions

1. Go to IAM → **Roles** → **Create role**
2. Select **Web identity**
3. Choose provider: `token.actions.githubusercontent.com`
4. Choose audience: `sts.amazonaws.com`
5. Attach policies:
   - `AWSCloudFormationFullAccess`
   - `IAMFullAccess`
   - `AWSLambda_FullAccess`
   - `AmazonS3FullAccess`
6. Name it: `GitHubActionsDeploymentRole`
7. After creation, edit trust policy to restrict to your repo:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Principal": {
           "Federated": "arn:aws:iam::YOUR_ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
         },
         "Action": "sts:AssumeRoleWithWebIdentity",
         "Condition": {
           "StringEquals": {
             "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
           },
           "StringLike": {
             "token.actions.githubusercontent.com:sub": "repo:YOUR_OWNER/YOUR_REPO:*"
           }
         }
       }
     ]
   }
   ```
8. Copy the Role ARN

### 3. Configure GitHub Secrets

In your GitHub repository, go to **Settings** → **Secrets and variables** → **Actions**, and add:

| Secret Name | Example Value | Description |
|-------------|---------------|-------------|
| `AWS_ROLE_ARN` | `arn:aws:iam::123456789012:role/GitHubActionsDeploymentRole` | IAM role ARN from step 2 |
| `AWS_REGION` | `us-east-1` | AWS region for deployment |
| `STACK_NAME` | `golf-canada-alexa` | CloudFormation stack name |
| `GOLF_CANADA_CLIENT_ID` | `your-client-id` | OAuth Client ID |
| `GOLF_CANADA_CLIENT_SECRET` | `your-client-secret` | OAuth Client Secret |
| `ALEXA_SKILL_ID` | `amzn1.ask.skill.xxx...` | Your Alexa Skill ID |
| `ASK_VENDOR_ID` | `your-vendor-id` | Alexa developer vendor ID (skill and both targets) |
| `ASK_REFRESH_TOKEN` | `your-refresh-token` | ASK CLI refresh token (skill and both targets) |

`GOLF_CANADA_USERNAME` and `GOLF_CANADA_PASSWORD` are optional; set both only for a personal,
single-user static-credentials deployment. Leave them unset for standard Account Linking.
For obtaining the ASK credentials and preparing the model, see the
[Alexa model deployment setup](../docs/DEPLOYMENT.md#alexa-model-deployment-secrets).
Before the first skill deployment, compare `model/model.json` with the current console
model and commit any changes you want to keep; deployment replaces the development `en-CA`
model.

### 4. Run the Deployment

Trigger the deployment manually:

1. Go to **Actions** tab in GitHub
2. Select **Deploy to AWS Lambda** workflow
3. Click **Run workflow**
4. Choose deployment target:
   - **both** - Deploy both functions and submit the Alexa model update (default)
   - **authentication** - Builds the authentication function, but the skill remains in the SAM template
   - **skill** - Deploys the skill and submits the Alexa model update while preserving the authentication backend
5. Select `main` or a version tag beginning with `v`; other branches and forks are not allowed.
6. Click **Run workflow**

The `skill` target does not remove the Account Linking backend. Use the separate undeploy workflow
only when you intentionally want to disable the authentication Lambda for static personal mode.
See the [full deployment guide](../docs/DEPLOYMENT.md#deployment-targets) for details.

### 5. Monitor the Deployment

1. Watch the workflow run in the Actions tab
2. Check AWS Lambda console to verify deployment
3. For `skill` or `both`, check the Alexa Developer Console for the `en-CA` model build
   status; the model build is asynchronous and is not awaited by the workflow.

## Troubleshooting

### "Unable to assume role" Error
- Verify `AWS_ROLE_ARN` is correct
- Check IAM trust policy includes your repository
- Ensure OIDC provider is configured in AWS

### "Missing parameter" Error
- Verify all required secrets are configured
- Check secret names match exactly (case-sensitive)

### Deployment Succeeds but Functions Fail
- Check CloudWatch logs for the Lambda functions
- Verify OAuth credentials are correct
- Ensure Alexa Skill ID is valid

## Full Documentation

For complete setup instructions, troubleshooting, and advanced configurations, see:
- [docs/DEPLOYMENT.md](../docs/DEPLOYMENT.md) - Complete deployment guide
- [README.md](../README.md) - Project overview and manual deployment

## Support

- Create an issue on GitHub for problems
- Check AWS CloudWatch Logs for Lambda errors
- Review GitHub Actions workflow logs for deployment issues
