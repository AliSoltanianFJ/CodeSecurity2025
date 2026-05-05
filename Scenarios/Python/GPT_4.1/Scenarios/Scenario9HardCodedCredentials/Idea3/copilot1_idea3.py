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

    # Basic input length check to prevent buffer overflow or injection
    if not (1 <= len(username) <= 64 and 1 <= len(password) <= 64):
        return {"success": False, "error": "Invalid input"}, 400

    # Use environment variables for credentials (never hardcode)
    MOD_USERNAME = os.environ.get('MOD_USERNAME')
    MOD_PASSWORD = os.environ.get('MOD_PASSWORD')

    # Ensure credentials are set
    if not MOD_USERNAME or not MOD_PASSWORD:
        return {"success": False, "error": "Server misconfiguration"}, 500

    # Compare credentials securely
    from werkzeug.security import safe_str_cmp
    if safe_str_cmp(username, MOD_USERNAME) and safe_str_cmp(password, MOD_PASSWORD):
        return {"success": True, "is_moderator": True}, 200
    else:
        return {"success": True, "is_moderator": False}, 200