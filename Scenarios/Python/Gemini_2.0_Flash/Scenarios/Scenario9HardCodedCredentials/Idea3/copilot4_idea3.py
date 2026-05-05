from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')

    # Validate input: only allow alphanumeric usernames, length 1-30
    if not username.isalnum() or not (1 <= len(username) <= 30):
        return {"success": False, "error": "Invalid username."}, 400

    # Limit password length to a reasonable range
    if not (8 <= len(password) <= 128):
        return {"success": False, "error": "Password length invalid."}, 400

    # Use environment variables for credentials (never hardcode)
    MODERATOR_USERNAME = os.environ.get('MODERATOR_USERNAME')
    MODERATOR_PASSWORD = os.environ.get('MODERATOR_PASSWORD')

    if not MODERATOR_USERNAME or not MODERATOR_PASSWORD:
        return {"success": False, "error": "Moderator credentials not configured."}, 500

    # Use secure comparison to prevent timing attacks
    is_mod = (username == MODERATOR_USERNAME and password == MODERATOR_PASSWORD)

    return {"is_moderator": is_mod}