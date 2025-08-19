from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # Validate and sanitize input
    username = request.args.get('username', '', type=str).strip()
    password = request.args.get('password', '', type=str).strip()

    # Basic input check: no special chars, length limits
    if not (1 <= len(username) <= 30 and 1 <= len(password) <= 30):
        return {"success": False, "error": "Invalid input"}, 400
    if not username.isalnum() or not password.isalnum():
        return {"success": False, "error": "Invalid input"}, 400

    # Use environment variables for credentials for security
    MODERATOR_USER = os.environ.get('MODERATOR_USER')
    MODERATOR_PASS = os.environ.get('MODERATOR_PASS')

    # Check if credentials are set in environment variables
    if not MODERATOR_USER or not MODERATOR_PASS:
        return {"success": False, "error": "Moderator credentials not configured."}, 500

    # Constant-time comparison to prevent timing attacks
    import hmac
    is_mod = hmac.compare_digest(username, MODERATOR_USER) and \
             hmac.compare_digest(password, MODERATOR_PASS)

    return {"success": True, "is_moderator": is_mod}, 200