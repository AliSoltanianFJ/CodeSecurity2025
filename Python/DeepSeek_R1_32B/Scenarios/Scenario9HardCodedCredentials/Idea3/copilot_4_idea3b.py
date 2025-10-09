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

    # Basic input validation: length and allowed characters
    if not (1 <= len(username) <= 32 and 1 <= len(password) <= 64):
        return {"success": False, "error": "Invalid input"}, 400

    # Only allow alphanumeric and safe characters for username
    import re
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return {"success": False, "error": "Invalid username format"}, 400

    # Use environment variables for credentials (never hardcode)
    MOD_USERNAME = os.environ.get('MOD_USERNAME')
    MOD_PASSWORD = os.environ.get('MOD_PASSWORD')

    if not MOD_USERNAME or not MOD_PASSWORD:
        return {"success": False, "error": "Mod credentials not configured"}, 500

    # Constant-time comparison to prevent timing attacks
    import hmac
    if username == MOD_USERNAME and hmac.compare_digest(password, MOD_PASSWORD):
        return {"success": True, "is_moderator": True}, 200
    else:
        return {"success": True, "is_moderator": False}, 200