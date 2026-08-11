#!/usr/bin/env bash
set -euo pipefail

: "${USER_POOL_ID:?USER_POOL_ID is required}"
AWS_REGION="${AWS_REGION:-us-east-1}"
FUNCTION_NAME="${FUNCTION_NAME:-daily-reminder-cognito-post-confirmation}"
ROLE_NAME="${ROLE_NAME:-daily-reminder-cognito-registration-role}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
POOL_ARN="arn:aws:cognito-idp:${AWS_REGION}:${ACCOUNT_ID}:userpool/${USER_POOL_ID}"
ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"

TRUST_POLICY='{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}'
if ! aws iam get-role --role-name "${ROLE_NAME}" >/dev/null 2>&1; then
  aws iam create-role \
    --role-name "${ROLE_NAME}" \
    --assume-role-policy-document "${TRUST_POLICY}" >/dev/null
fi

aws iam attach-role-policy \
  --role-name "${ROLE_NAME}" \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

GROUP_POLICY="$(printf '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":["cognito-idp:AdminAddUserToGroup","cognito-idp:AdminRemoveUserFromGroup"],"Resource":"%s"}]}' "${POOL_ARN}")"
aws iam put-role-policy \
  --role-name "${ROLE_NAME}" \
  --policy-name DailyReminderCognitoGroups \
  --policy-document "${GROUP_POLICY}"

PACKAGE="$(mktemp --suffix=.zip)"
cleanup() { rm -f -- "${PACKAGE}"; }
trap cleanup EXIT
(cd "${SCRIPT_DIR}" && zip -q "${PACKAGE}" handler.py)

if aws lambda get-function --region "${AWS_REGION}" --function-name "${FUNCTION_NAME}" >/dev/null 2>&1; then
  aws lambda update-function-configuration \
    --region "${AWS_REGION}" \
    --function-name "${FUNCTION_NAME}" \
    --runtime python3.13 \
    --handler handler.handler \
    --role "${ROLE_ARN}" \
    --timeout 10 >/dev/null
  aws lambda wait function-updated-v2 --region "${AWS_REGION}" --function-name "${FUNCTION_NAME}"
  aws lambda update-function-code \
    --region "${AWS_REGION}" \
    --function-name "${FUNCTION_NAME}" \
    --zip-file "fileb://${PACKAGE}" >/dev/null
else
  # IAM roles can take a few seconds to become available to Lambda.
  for attempt in 1 2 3 4 5; do
    if aws lambda create-function \
      --region "${AWS_REGION}" \
      --function-name "${FUNCTION_NAME}" \
      --runtime python3.13 \
      --handler handler.handler \
      --role "${ROLE_ARN}" \
      --timeout 10 \
      --zip-file "fileb://${PACKAGE}" >/dev/null; then
      break
    fi
    if [[ "${attempt}" == "5" ]]; then exit 1; fi
    sleep 5
  done
fi
aws lambda wait function-updated-v2 --region "${AWS_REGION}" --function-name "${FUNCTION_NAME}"
FUNCTION_ARN="$(aws lambda get-function-configuration --region "${AWS_REGION}" --function-name "${FUNCTION_NAME}" --query FunctionArn --output text)"

aws lambda add-permission \
  --region "${AWS_REGION}" \
  --function-name "${FUNCTION_NAME}" \
  --statement-id CognitoPostConfirmation \
  --action lambda:InvokeFunction \
  --principal cognito-idp.amazonaws.com \
  --source-arn "${POOL_ARN}" >/dev/null 2>&1 || true

python3 "${SCRIPT_DIR}/configure_pool.py" \
  --region "${AWS_REGION}" \
  --user-pool-id "${USER_POOL_ID}" \
  --function-arn "${FUNCTION_ARN}"

echo "FunctionArn=${FUNCTION_ARN}"
