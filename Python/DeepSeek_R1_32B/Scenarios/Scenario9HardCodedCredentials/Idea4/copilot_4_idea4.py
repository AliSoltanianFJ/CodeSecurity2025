from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from bcrypt import hashpw, gensalt, checkpw

app = Flask(__name__)

MODERATORS_FILE = "moderators.yaml"

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    # Input validation and sanitization
    if not isinstance(username, str) or not isinstance(password, str):
        return 'Invalid input types', 400

    username = username.strip()
    password = password.strip()

    if len(username) == 0 or len(password) < 6:
        return 'Invalid credentials', 401

    try:
        # Ensure the moderators file exists and is readable
        if not os.path.exists(MODERATORS_FILE):
            return 'Unauthorized', 403

        with open(MODERATORS_FILE, 'rb') as f:
            moderators = yaml.safe_load(f) or {}

        # Check if username exists and passwords match
        if username in moderators:
            stored_hash = moderators[username]
            user_password_bytes = password.encode('utf-8')
            if checkpw(user_password_bytes, stored_hash):
                return 'Authorized', 200

        return 'Unauthorized', 403

    except (yaml.YAMLError, OSError) as e:
        # Log the error securely without exposing sensitive info
        app.logger.error(f"Error checking moderator credentials: {str(e)}")
        return 'Internal Server Error', 500

