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
    # Validate input types and presence
    username = request.args.get('username', type=str)
    password = request.args.get('password', type=str)

    # Input validation: reject missing or malformed input
    if not username or not password:
        # Do not leak details about which field is missing
        return {"success": False, "error": "Invalid credentials."}, 400

    # Sanitize inputs: allow only safe characters (alphanumeric + limited symbols)
    import re
    if not re.fullmatch(r'[A-Za-z0-9_.-]{3,32}', username):
        return {"success": False, "error": "Invalid credentials."}, 400

    # Load user data securely from a YAML file (never hardcode credentials)
    # The YAML file should be stored securely, with restricted permissions
    try:
        with open(os.path.join(os.path.dirname(__file__), 'users.yaml'), 'r') as f:
            users = yaml.safe_load(f)
    except Exception:
        # Do not leak file system or parsing errors
        return {"success": False, "error": "Internal error."}, 500

    # Defensive: ensure users is a dict
    if not isinstance(users, dict):
        return {"success": False, "error": "Internal error."}, 500

    # Principle of least privilege: only check moderator status
    user = users.get(username)
    if not user or 'password_hash' not in user or 'role' not in user:
        return {"success": False, "error": "Invalid credentials."}, 401

    # Use a strong password hash (e.g., bcrypt)
    import bcrypt
    password_bytes = password.encode('utf-8')
    stored_hash = user['password_hash'].encode('utf-8')

    # Constant-time comparison to prevent timing attacks
    if not bcrypt.checkpw(password_bytes, stored_hash):
        return {"success": False, "error": "Invalid credentials."}, 401

    # Only return True if user is a moderator
    is_mod = user['role'] == 'moderator'
    return {"success": True, "is_moderator": is_mod}, 200
# ...existing code...