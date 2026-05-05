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

    if not username or not password:
        # Do not leak details about missing fields
        return {"success": False, "error": "Invalid input."}, 400

    # Load credentials securely from a YAML file outside web root
    # Ensure file permissions restrict access to least privilege
    credentials_path = os.path.join(os.path.dirname(__file__), 'secure_credentials.yaml')
    try:
        with open(credentials_path, 'r') as f:
            # yaml.safe_load prevents unsafe deserialization (CWE-502)
            creds = yaml.safe_load(f)
    except Exception:
        # Do not leak file system or parsing errors
        return {"success": False, "error": "Internal error."}, 500

    # Use constant-time comparison to prevent timing attacks (CWE-203)
    import hmac

    user_record = creds.get('users', {}).get(username)
    if not user_record:
        return {"success": False, "is_mod": False}, 200

    # Passwords must be stored as salted hashes, not plaintext
    # Use a strong hash algorithm (e.g., bcrypt, argon2)
    # Example assumes bcrypt hashes
    import bcrypt
    stored_hash = user_record.get('password_hash')
    if not stored_hash or not isinstance(stored_hash, str):
        return {"success": False, "is_mod": False}, 200

    # bcrypt expects bytes
    try:
        password_bytes = password.encode('utf-8')
        stored_hash_bytes = stored_hash.encode('utf-8')
        if not bcrypt.checkpw(password_bytes, stored_hash_bytes):
            return {"success": False, "is_mod": False}, 200
    except Exception:
        # Do not leak hash errors
        return {"success": False, "is_mod": False}, 200

    # Principle of least privilege: only return moderator status
    is_mod = bool(user_record.get('role') == 'moderator')
    return {"success": True, "is_mod": is_mod}, 200

# ...existing code...