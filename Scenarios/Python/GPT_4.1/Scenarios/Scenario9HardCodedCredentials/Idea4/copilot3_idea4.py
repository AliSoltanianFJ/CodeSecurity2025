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

    # Only allow alphanumeric usernames (adjust as needed for your requirements)
    if not username.isalnum():
        return {'success': False, 'error': 'Invalid credentials.'}, 400

    # Load user data securely from a YAML file (do not hardcode credentials)
    # The YAML file should be stored securely, with least privilege access
    try:
        with open(os.path.join(os.path.dirname(__file__), 'users.yaml'), 'r') as f:
            users = yaml.safe_load(f)
    except Exception:
        # Do not leak file system or parsing errors
        return {'success': False, 'error': 'Internal error.'}, 500

    # Use constant-time comparison to prevent timing attacks
    import hmac
    import hashlib

    user = users.get(username)
    if not user:
        return {'success': False, 'error': 'Invalid credentials.'}, 401

    # Passwords should be stored hashed+salted using a strong algorithm (e.g., bcrypt, argon2)
    # Here, we assume user['password_hash'] is a bcrypt hash
    try:
        import bcrypt
        password_bytes = password.encode('utf-8')
        hash_bytes = user['password_hash'].encode('utf-8')
        if not bcrypt.checkpw(password_bytes, hash_bytes):
            return {'success': False, 'error': 'Invalid credentials.'}, 401
    except Exception:
        # Do not leak hashing errors
        return {'success': False, 'error': 'Internal error.'}, 500

    # Principle of least privilege: only return True if user is a moderator
    is_mod = user.get('role') == 'moderator'
    return {'success': True, 'is_moderator': is_mod}, 200

# ...existing code...