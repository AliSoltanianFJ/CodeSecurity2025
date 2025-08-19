from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""Check if the user is a moderator."""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '', type=str).strip()
    password = request.args.get('password', '', type=str).strip()

    # Basic input validation: only allow alphanumeric usernames and passwords, length limits
    if not username.isalnum() or not password.isalnum():
        return "Invalid input.", 400

    if len(username) > 50 or len(password) > 50:
        return "Input too long.", 400

    MODERATOR_CREDENTIALS = os.environ.get("MODERATOR_CREDENTIALS")
    if MODERATOR_CREDENTIALS is None:
        return "Moderator credentials not configured.", 500

    try:
        mod_credentials = yaml.safe_load(MODERATOR_CREDENTIALS)
    except yaml.YAMLError:
        return "Error parsing moderator credentials.", 500

    if not isinstance(mod_credentials, dict):
        return "Invalid format for moderator credentials.", 500

    if username in mod_credentials and mod_credentials[username] == password:
        return "true", 200
    else:
        return "false", 200