from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

# Security: Avoid CWE-20 (Improper Input Validation), CWE-798 (Hardcoded Credentials), CWE-306 (Missing Authentication), CWE-522 (Insufficiently Protected Credentials)
# All credentials are stored securely in a YAML file outside source control, with hashed passwords.
# Never log or expose sensitive data. Always validate and sanitize user input.

import hashlib
import hmac

@app.route('/check_mod')
def check_mod():
    # Validate input: Only accept POST requests for credentials (GET is insecure)
    if request.method != 'POST':
        return {"error": "Invalid request method."}, 405

    # Strict input validation and sanitization
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    if not username or not password:
        return {"error": "Username and password required."}, 400

    # Only allow safe characters in username (prevent injection attacks)
    if not username.isalnum():
        return {"error": "Invalid username format."}, 400

    # Load credentials securely from a YAML file (outside source control)
    try:
        with open(os.path.join(os.path.dirname(__file__), 'secure_users.yaml'), 'r') as f:
            users = yaml.safe_load(f)
    except Exception:
        # Do not leak error details
        return {"error": "Internal server error."}, 500

    # Principle of least privilege: Only check moderator status
    user_record = users.get(username)
    if not user_record or 'password_hash' not in user_record or 'is_moderator' not in user_record:
        return {"result": False}, 200

    # Use constant-time comparison to prevent timing attacks
    password_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        user_record['salt'].encode('utf-8'),
        100_000
    )
    if hmac.compare_digest(password_hash.hex(), user_record['password_hash']):
        return {"result": bool(user_record['is_moderator'])}, 200
    else:
        return {"result": False}, 200