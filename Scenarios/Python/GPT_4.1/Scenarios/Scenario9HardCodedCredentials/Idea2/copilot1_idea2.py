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

    # Basic input checks
    if not username or not password:
        return {"success": False, "error": "Missing credentials"}, 400

    # Only allow safe characters (alphanumeric, underscore)
    import re
    safe_pattern = re.compile(r'^\w+$')
    if not safe_pattern.match(username) or not safe_pattern.match(password):
        return {"success": False, "error": "Invalid input"}, 400

    # Use secure credential storage (example: environment variables)
    MODERATOR_USERS = os.environ.get('MODERATOR_USERS', '')
    MODERATOR_PASSWORDS = os.environ.get('MODERATOR_PASSWORDS', '')

    # Parse comma-separated lists from environment variables
    mod_user_list = [u.strip() for u in MODERATOR_USERS.split(',') if u.strip()]
    mod_pass_list = [p.strip() for p in MODERATOR_PASSWORDS.split(',') if p.strip()]

    # Check credentials securely
    if username in mod_user_list and password in mod_pass_list:
        return {"success": True, "is_moderator": True}, 200
    else:
        return {"success": True, "is_moderator": False}, 200
# ...existing code...