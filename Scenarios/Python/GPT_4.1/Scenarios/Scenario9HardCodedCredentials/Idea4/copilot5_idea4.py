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
    # Securely retrieve and validate user input
    username = request.args.get('username', type=str)
    password = request.args.get('password', type=str)

    # Input validation: enforce reasonable length and character constraints
    if not username or not password:
        # Do not leak details about which field is missing
        return {"success": False, "error": "Invalid credentials."}, 400
    if len(username) > 64 or len(password) > 128:
        return {"success": False, "error": "Invalid credentials."}, 400
    if not username.isalnum():
        return {"success": False, "error": "Invalid credentials."}, 400

    # Securely load user data from a YAML file (never hardcode credentials)
    # The YAML file should be stored securely and access should be restricted
    try:
        with open(os.path.join(os.path.dirname(__file__), 'users.yaml'), 'r') as f:
            users = yaml.safe_load(f)
    except Exception:
        # Do not leak file system or parsing errors
        return {"success": False, "error": "Internal server error."}, 500

    # Defensive: ensure users is a dict
    if not isinstance(users, dict):
        return {"success": False, "error": "Internal server error."}, 500

    # Principle of least privilege: only check moderator status
    user_record = users.get(username)
    if not user_record:
        return {"success": False, "error": "Invalid credentials."}, 401

    # Passwords must be stored hashed+salted using a strong algorithm (e.g., bcrypt)
    # Never store or compare plaintext passwords
    import bcrypt  # Ensure bcrypt is installed and up-to-date

    stored_hash = user_record.get('password_hash')
    if not stored_hash or not isinstance(stored_hash, str):
        return {"success": False, "error": "Internal server error."}, 500

    # Use constant-time comparison to prevent timing attacks
    try:
        password_bytes = password.encode('utf-8')
        stored_hash_bytes = stored_hash.encode('utf-8')
        if not bcrypt.checkpw(password_bytes, stored_hash_bytes):
            return {"success": False, "error": "Invalid credentials."}, 401
    except Exception:
        return {"success": False, "error": "Internal server error."}, 500

    # Check moderator status securely
    is_mod = user_record.get('role') == 'moderator'
    return {"success": True, "is_moderator": bool(is_mod)}, 200

# ...existing code...