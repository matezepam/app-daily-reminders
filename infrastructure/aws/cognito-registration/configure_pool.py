"""Safely attach the role trigger without resetting existing pool settings."""

import argparse
import copy

import boto3


UPDATABLE_POOL_KEYS = (
    "Policies",
    "DeletionProtection",
    "LambdaConfig",
    "AutoVerifiedAttributes",
    "SmsVerificationMessage",
    "EmailVerificationMessage",
    "EmailVerificationSubject",
    "VerificationMessageTemplate",
    "SmsAuthenticationMessage",
    "UserAttributeUpdateSettings",
    "MfaConfiguration",
    "DeviceConfiguration",
    "EmailConfiguration",
    "SmsConfiguration",
    "UserPoolTags",
    "AdminCreateUserConfig",
    "UserPoolAddOns",
    "AccountRecoverySetting",
    "PoolName",
    "UserPoolTier",
    "KeyConfiguration",
    "IssuerConfiguration",
)


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--user-pool-id", required=True)
    parser.add_argument("--function-arn", required=True)
    parser.add_argument("--region", required=True)
    return parser.parse_args()


def ensure_custom_role(client, pool):
    attributes = {item["Name"]: item for item in pool.get("SchemaAttributes", [])}
    existing = attributes.get("custom:role")
    if existing:
        if existing.get("AttributeDataType") != "String":
            raise RuntimeError("custom:role exists but is not a String attribute")
        return
    client.add_custom_attributes(
        UserPoolId=pool["Id"],
        CustomAttributes=[
            {
                "Name": "role",
                "AttributeDataType": "String",
                "Mutable": False,
                "StringAttributeConstraints": {"MinLength": "5", "MaxLength": "7"},
            }
        ],
    )


def ensure_groups(client, pool_id):
    for group in ("ADMIN", "STUDENT"):
        try:
            client.get_group(UserPoolId=pool_id, GroupName=group)
        except client.exceptions.ResourceNotFoundException:
            client.create_group(
                UserPoolId=pool_id,
                GroupName=group,
                Description=(
                    "Profesores y administradores de clases"
                    if group == "ADMIN"
                    else "Estudiantes inscritos en clases"
                ),
            )


def update_pool_preserving_configuration(client, pool, function_arn):
    request = {"UserPoolId": pool["Id"]}
    for key in UPDATABLE_POOL_KEYS:
        if key in pool and pool[key] is not None:
            request[key] = copy.deepcopy(pool[key])

    request.setdefault("LambdaConfig", {})["PostConfirmation"] = function_arn

    # This legacy field conflicts with TemporaryPasswordValidityDays.
    password_policy = request.get("Policies", {}).get("PasswordPolicy", {})
    admin_config = request.get("AdminCreateUserConfig", {})
    if "TemporaryPasswordValidityDays" in password_policy:
        admin_config.pop("UnusedAccountValidityDays", None)

    client.update_user_pool(**request)


def main():
    args = parse_args()
    client = boto3.client("cognito-idp", region_name=args.region)
    pool = client.describe_user_pool(UserPoolId=args.user_pool_id)["UserPool"]
    ensure_custom_role(client, pool)
    ensure_groups(client, args.user_pool_id)

    # Refresh after adding the immutable schema attribute, then preserve every
    # updatable setting while adding only PostConfirmation.
    pool = client.describe_user_pool(UserPoolId=args.user_pool_id)["UserPool"]
    update_pool_preserving_configuration(client, pool, args.function_arn)
    updated = client.describe_user_pool(UserPoolId=args.user_pool_id)["UserPool"]
    actual = updated.get("LambdaConfig", {}).get("PostConfirmation")
    if actual != args.function_arn:
        raise RuntimeError("Cognito post-confirmation trigger was not attached")
    print("Cognito role registration configured successfully")


if __name__ == "__main__":
    main()
