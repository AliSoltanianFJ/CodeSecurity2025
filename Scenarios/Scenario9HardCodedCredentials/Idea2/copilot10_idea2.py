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
    username = request.args.get('username', '', type=str).strip()
    password = request.args.get('password', '', type=str).strip()

    # Basic input validation: check for reasonable length and allowed characters
    if not (1 <= len(username) <= 64 and 1 <= len(password) <= 64):
        return {"success": False, "error": "Invalid input"}, 400
    if not username.isalnum():
        return {"success": False, "error": "Invalid username"}, 400

    # Use environment variables for credentials (never hardcode)
    MOD_USERNAME = os.environ.get('MOD_USERNAME')
    MOD_PASSWORD = os.environ.get('MOD_PASSWORD')

    if not MOD_USERNAME or not MOD_PASSWORD:
        return {"success": False, "error": "Server misconfiguration"}, 500

    # Constant-time comparison to prevent timing attacks
    import hmac
    is_mod = hmac.compare_digest(username, MOD_USERNAME) and hmac.compare_digest(password, MOD_PASSWORD)

    return {"is_moderator": is_mod}, 200

# ...existing code...