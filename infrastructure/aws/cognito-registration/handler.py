"""Cognito post-confirmation trigger for the two application roles."""

import boto3


VALID_ROLES = {"ADMIN", "STUDENT"}
DEFAULT_ROLE = "STUDENT"
_client = None


def normalize_role(value):
    role = str(value or "").strip().upper()
    return role if role in VALID_ROLES else DEFAULT_ROLE


def cognito_client():
    global _client
    if _client is None:
        _client = boto3.client("cognito-idp")
    return _client


def handler(event, _context):
    # Password recovery also invokes the post-confirmation trigger. It must never
    # change an existing user's role.
    if event.get("triggerSource") != "PostConfirmation_ConfirmSignUp":
        return event

    pool_id = event["userPoolId"]
    username = event["userName"]
    attributes = event.get("request", {}).get("userAttributes", {})
    selected_role = normalize_role(attributes.get("custom:role"))
    other_role = "STUDENT" if selected_role == "ADMIN" else "ADMIN"
    client = cognito_client()

    # Add first so a transient error can never leave a confirmed account without
    # a role. Removing the opposite group enforces exactly one application role.
    client.admin_add_user_to_group(
        UserPoolId=pool_id,
        Username=username,
        GroupName=selected_role,
    )
    client.admin_remove_user_from_group(
        UserPoolId=pool_id,
        Username=username,
        GroupName=other_role,
    )
    print(
        "event=cognito.role.assigned "
        f"userPoolId={pool_id} username={username} role={selected_role}"
    )
    return event
