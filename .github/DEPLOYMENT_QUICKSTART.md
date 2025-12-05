# Deployment Quick Start Guide

This guide provides a quick reference for setting up automated deployments with GitHub Actions.

## Prerequisites

- AWS Account with administrative access
- GitHub repository with Actions enabled
- Golf Canada OAuth credentials
- Alexa Skill ID

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

### 4. Run the Deployment

Trigger the deployment manually:

1. Go to **Actions** tab in GitHub
2. Select **Deploy to AWS Lambda** workflow
3. Click **Run workflow**
4. Choose deployment target:
   - **both** - Deploy both functions (default)
   - **authentication** - Deploy authentication only
   - **skill** - Deploy skill only
5. Select branch (e.g., `main`)
6. Click **Run workflow**

The workflow will build and deploy only the selected function(s).

### 5. Monitor the Deployment

1. Watch the workflow run in the Actions tab
2. Check AWS Lambda console to verify deployment

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
