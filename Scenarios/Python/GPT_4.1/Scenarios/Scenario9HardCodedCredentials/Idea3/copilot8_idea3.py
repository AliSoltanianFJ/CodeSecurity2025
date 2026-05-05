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
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '').strip()

    # Check input length to prevent abuse
    if not username or not password or len(username) > 64 or len(password) > 64:
        return {"success": False, "error": "Invalid input"}, 400

    # Use environment variables for credentials (never hardcode)
    MOD_USERNAME = os.environ.get('MOD_USERNAME')
    MOD_PASSWORD = os.environ.get('MOD_PASSWORD')

    # Check credentials securely
    if username == MOD_USERNAME and password == MOD_PASSWORD:
        return {"success": True, "is_moderator": True}, 200
    else:
        return {"success": True, "is_moderator": False}, 200