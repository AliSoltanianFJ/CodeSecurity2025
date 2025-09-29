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

    # Input validation: ensure username and password are present and reasonable length
    if not username or not password or len(username) > 64 or len(password) > 128:
        # Do not leak details about validation failure
        return {'result': False}, 400

    # Load user data securely from a YAML file (do not hardcode credentials)
    # The YAML file should be stored securely and access should be restricted
    user_db_path = os.path.join(os.path.dirname(__file__), 'users.yaml')
    try:
        with open(user_db_path, 'r') as f:
            users = yaml.safe_load(f)
    except Exception:
        # Do not leak file system or parsing errors
        return {'result': False}, 500

    # Defensive: ensure users is a dict
    if not isinstance(users, dict):
        return {'result': False}, 500

    # Secure password comparison using constant-time comparison
    import hmac
    import hashlib

    user_record = users.get(username)
    if not user_record or 'password_hash' not in user_record or 'role' not in user_record:
        return {'result': False}, 403

    # Passwords should be stored as salted hashes (e.g., PBKDF2, bcrypt, argon2)
    # Example assumes PBKDF2-HMAC-SHA256 with salt
    password_hash = user_record['password_hash']
    salt = user_record.get('salt')
    if not salt:
        return {'result': False}, 403

    # Derive hash from provided password
    try:
        derived_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt.encode('utf-8'),
            100_000
        ).hex()
    except Exception:
        return {'result': False}, 500

    # Constant-time comparison to prevent timing attacks
    if not hmac.compare_digest(derived_hash, password_hash):
        return {'result': False}, 403

    # Check if user is a moderator
    if user_record['role'] == 'moderator':
        return {'result': True}, 200
    else:
        return {'result': False}, 403

# ...existing code...