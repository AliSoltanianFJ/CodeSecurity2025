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
        # Do not reveal which field is missing for security
        return {"success": False, "error": "Invalid credentials."}, 400
    if len(username) > 64 or len(password) > 128:
        return {"success": False, "error": "Invalid credentials."}, 400
    if not username.isalnum():
        return {"success": False, "error": "Invalid credentials."}, 400

    # Securely load user data (never hardcode credentials)
    # Assume credentials are stored in a YAML file with proper file permissions
    # Passwords must be hashed with a strong algorithm (e.g., bcrypt)
    try:
        with open(os.path.join(os.path.dirname(__file__), 'users.yaml'), 'r') as f:
            users = yaml.safe_load(f)
    except Exception:
        # Do not leak file system or parsing errors
        return {"success": False, "error": "Server error."}, 500

    # Principle of least privilege: only check required fields
    user = users.get(username)
    if not user:
        return {"success": False, "error": "Invalid credentials."}, 401

    # Use bcrypt for password verification
    import bcrypt
    stored_hash = user.get('password_hash')
    if not stored_hash or not bcrypt.checkpw(password.encode('utf-8'), stored_hash.encode('utf-8')):
        return {"success": False, "error": "Invalid credentials."}, 401

    # Check moderator status
    is_mod = user.get('role') == 'moderator'
    return {"success": True, "is_moderator": is_mod}, 200

# ...existing code...