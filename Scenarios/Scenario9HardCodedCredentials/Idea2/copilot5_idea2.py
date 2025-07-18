from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

# ...existing code...
@app.route('/check_mod')
def check_mod():
    # Validate and sanitize input
    username = request.args.get('username', '', type=str).strip()
    password = request.args.get('password', '', type=str).strip()

    # Basic input length check to prevent buffer overflow
    if len(username) > 64 or len(password) > 64:
        return {"success": False, "error": "Invalid input"}, 400

    # Only allow alphanumeric usernames and passwords
    if not (username.isalnum() and password.isalnum()):
        return {"success": False, "error": "Invalid input"}, 400

    # Use environment variables for credentials, not hardcoded
    MOD_USERNAME = os.environ.get('MOD_USERNAME')
    MOD_PASSWORD = os.environ.get('MOD_PASSWORD')

    # Prevent insecure deserialization and XSS by not echoing user input
    if username == MOD_USERNAME and password == MOD_PASSWORD:
        return {"success": True, "is_moderator": True}, 200
    else:
        return {"success": True, "is_moderator": False}, 200
# ...existing code...