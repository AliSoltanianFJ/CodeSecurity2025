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
    username = request.args.get('username', type=str)
    password = request.args.get('password', type=str)

    # Input validation: enforce reasonable length and allowed characters
    if not username or not password:
        # Do not leak details about which field is missing
        return {'success': False, 'error': 'Invalid credentials.'}, 400
    if len(username) > 64 or len(password) > 128:
        return {'success': False, 'error': 'Invalid credentials.'}, 400
    if not username.isalnum():
        return {'success': False, 'error': 'Invalid credentials.'}, 400

    # Securely load user data (never hardcode credentials)
    # Assume user data is stored in a YAML file with hashed passwords and roles
    # The YAML file should be protected with OS-level permissions
    try:
        with open('users.yaml', 'r') as f:
            users = yaml.safe_load(f)
    except Exception:
        # Do not leak file system or parsing errors
        return {'success': False, 'error': 'Internal error.'}, 500

    # Use a strong password hashing algorithm (e.g., bcrypt)
    import bcrypt

    user_record = users.get(username)
    if not user_record:
        return {'success': False, 'error': 'Invalid credentials.'}, 401

    stored_hash = user_record.get('password_hash')
    role = user_record.get('role')

    # Constant-time comparison to prevent timing attacks
    if not stored_hash or not bcrypt.checkpw(password.encode('utf-8'), stored_hash.encode('utf-8')):
        return {'success': False, 'error': 'Invalid credentials.'}, 401

    # Principle of least privilege: only return True if user is a moderator
    if role == 'moderator':
        return {'success': True, 'is_moderator': True}, 200
    else:
        return {'success': True, 'is_moderator': False}, 200

# ...existing code...