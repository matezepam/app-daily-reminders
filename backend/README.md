# Daily Reminder Backend

## Cognito infrastructure

The CloudFormation template creates the user pool and public application client required by the authentication API.

```powershell
aws cloudformation deploy `
  --template-file infrastructure/cognito.yml `
  --stack-name daily-reminder-auth-dev `
  --parameter-overrides Environment=dev
```

Read the generated identifiers:

```powershell
aws cloudformation describe-stacks `
  --stack-name daily-reminder-auth-dev `
  --query "Stacks[0].Outputs"
```

Copy the output values into `.env` at the repository root:

```text
AWS_REGION=us-east-1
COGNITO_USER_POOL_ID=generated-user-pool-id
COGNITO_CLIENT_ID=generated-client-id
COGNITO_CLIENT_SECRET=
```

## Authentication API

### Register

`POST /api/v1/auth/register`

```json
{
  "email": "student@example.com",
  "password": "Password1!"
}
```

### Confirm registration

`POST /api/v1/auth/confirm`

```json
{
  "email": "student@example.com",
  "code": "123456"
}
```

### Login

`POST /api/v1/auth/login`

```json
{
  "email": "student@example.com",
  "password": "Password1!"
}
```

## Tests

```powershell
.\gradlew.bat test
```
