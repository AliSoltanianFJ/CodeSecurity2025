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
    # SECURITY: Validate and sanitize all user inputs
    username = request.args.get('username', type=str)
    password = request.args.get('password', type=str)

    # SECURITY: Reject requests with missing or malformed parameters
    if not username or not password:
        # Do not leak details about missing fields
        return {'success': False, 'error': 'Invalid request.'}, 400

    # SECURITY: Load credentials from a secure, external source (never hardcoded)
    # Example: credentials.yaml should be protected with strict file permissions
    try:
        with open('credentials.yaml', 'r') as f:
            # SECURITY: Use safe_load to prevent unsafe deserialization
            credentials = yaml.safe_load(f)
    except Exception:
        # Do not leak file or exception details
        return {'success': False, 'error': 'Internal error.'}, 500

    # SECURITY: Use constant-time comparison to prevent timing attacks
    import hmac
    import hashlib

    user_record = credentials.get('users', {}).get(username)
    if not user_record:
        return {'success': False, 'error': 'Authentication failed.'}, 403

    # SECURITY: Passwords should be stored as salted hashes (e.g., PBKDF2, bcrypt, argon2)
    # Example assumes PBKDF2-SHA256 with salt
    stored_hash = user_record.get('password_hash')
    salt = user_record.get('salt')
    if not stored_hash or not salt:
        return {'success': False, 'error': 'Authentication failed.'}, 403

    # Derive hash from provided password
    try:
        pwd_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt.encode('utf-8'),
            100_000
        )
        # SECURITY: Use hmac.compare_digest for constant-time comparison
        if not hmac.compare_digest(stored_hash, pwd_hash.hex()):
            return {'success': False, 'error': 'Authentication failed.'}, 403
    except Exception:
        return {'success': False, 'error': 'Internal error.'}, 500

    # SECURITY: Check moderator status using least privilege
    is_mod = user_record.get('role') == 'moderator'
    return {'success': True, 'is_moderator': is_mod}, 200

# ...existing code...