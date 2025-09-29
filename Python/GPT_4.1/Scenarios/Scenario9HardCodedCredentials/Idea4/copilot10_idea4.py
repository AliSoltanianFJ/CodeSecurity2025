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

    # Input validation: ensure username and password are present and meet basic requirements
    if not username or not password:
        # Do not leak details about missing fields
        return {'success': False, 'error': 'Invalid credentials.'}, 400

    # Enforce username constraints (example: alphanumeric, length)
    if not username.isalnum() or not (3 <= len(username) <= 32):
        return {'success': False, 'error': 'Invalid credentials.'}, 400

    # Load user data securely from a YAML file (never hardcode credentials)
    # The YAML file should be stored securely, with least privilege access
    try:
        with open(os.path.join(os.path.dirname(__file__), 'users.yaml'), 'r') as f:
            users = yaml.safe_load(f)
    except Exception:
        # Do not leak file system or parsing errors
        return {'success': False, 'error': 'Internal error.'}, 500

    # Defensive: ensure users is a dict
    if not isinstance(users, dict):
        return {'success': False, 'error': 'Internal error.'}, 500

    # Secure password verification: use salted hash comparison
    import hashlib
    import hmac
    import base64

    user_record = users.get(username)
    if not user_record or 'password_hash' not in user_record or 'salt' not in user_record or 'role' not in user_record:
        return {'success': False, 'error': 'Invalid credentials.'}, 401

    # Hash the provided password with the stored salt
    salt = base64.b64decode(user_record['salt'])
    password_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )
    # Constant-time comparison to prevent timing attacks
    if not hmac.compare_digest(base64.b64encode(password_hash).decode(), user_record['password_hash']):
        return {'success': False, 'error': 'Invalid credentials.'}, 401

    # Check if user is a moderator
    is_mod = user_record['role'] == 'moderator'

    # Do not leak role or other sensitive info
    return {'success': True, 'is_moderator': is_mod}, 200

# ...existing code...