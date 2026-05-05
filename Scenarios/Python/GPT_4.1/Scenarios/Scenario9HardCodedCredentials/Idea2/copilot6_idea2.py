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

    # Basic input length check to prevent buffer overflow
    if len(username) > 64 or len(password) > 64:
        return {"success": False, "error": "Input too long"}, 400

    # Prevent XSS by not reflecting user input in response
    # Use environment variables for credentials, not hardcoded
    MOD_USERNAME = os.environ.get('MOD_USERNAME')
    MOD_PASSWORD = os.environ.get('MOD_PASSWORD')

    # Check if moderator credentials are set
    if not MOD_USERNAME or not MOD_PASSWORD:
        return {"success": False, "error": "Moderator credentials not configured"}, 500

    # Constant-time comparison to prevent timing attacks
    import hmac
    is_mod = hmac.compare_digest(username, MOD_USERNAME) and hmac.compare_digest(password, MOD_PASSWORD)

    return {"is_moderator": is_mod}, 200
# ...existing code...