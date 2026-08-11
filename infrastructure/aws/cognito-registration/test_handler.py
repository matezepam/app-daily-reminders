import importlib
import sys
import unittest
from unittest.mock import Mock


sys.modules.setdefault("boto3", Mock())
role_handler = importlib.import_module("handler")


class CognitoRoleHandlerTest(unittest.TestCase):
    def setUp(self):
        self.client = Mock()
        role_handler._client = self.client

    def event(self, role="STUDENT", source="PostConfirmation_ConfirmSignUp"):
        return {
            "triggerSource": source,
            "userPoolId": "us-east-1_example",
            "userName": "person@example.com",
            "request": {"userAttributes": {"custom:role": role}},
            "response": {},
        }

    def test_assigns_selected_admin_group_and_removes_student(self):
        event = self.event("admin")

        self.assertIs(event, role_handler.handler(event, None))

        self.client.admin_add_user_to_group.assert_called_once_with(
            UserPoolId="us-east-1_example",
            Username="person@example.com",
            GroupName="ADMIN",
        )
        self.client.admin_remove_user_from_group.assert_called_once_with(
            UserPoolId="us-east-1_example",
            Username="person@example.com",
            GroupName="STUDENT",
        )

    def test_invalid_or_missing_role_defaults_safely_to_student(self):
        role_handler.handler(self.event("OWNER"), None)

        self.client.admin_add_user_to_group.assert_called_once_with(
            UserPoolId="us-east-1_example",
            Username="person@example.com",
            GroupName="STUDENT",
        )

    def test_password_recovery_never_changes_role(self):
        event = self.event("STUDENT", "PostConfirmation_ConfirmForgotPassword")

        self.assertIs(event, role_handler.handler(event, None))

        self.client.admin_add_user_to_group.assert_not_called()
        self.client.admin_remove_user_from_group.assert_not_called()


if __name__ == "__main__":
    unittest.main()
