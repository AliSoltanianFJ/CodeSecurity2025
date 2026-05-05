from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import bcrypt

app = Flask(__name__)

"""Return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # Validate and sanitize inputs
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '').strip()

    # Check for empty or excessively long input
    if not username or len(username) > 50:
        return "Invalid username", 400
    if not password or len(password) > 50:
        return "Invalid password", 400

    # Load moderator credentials from a secure YAML file
    try:
        with open('config.yaml', 'r') as f:
            config = yaml.safe_load(f)
            mod_credentials = config.get('mod_credentials', {})
    except (IOError, yaml.YAMLError) as e:
        return "Internal error loading credentials", 500

    # Check if the username exists in the moderator list
    if username not in mod_credentials:
        return "false"

    # Retrieve the stored password hash and compare
    stored_hash = mod_credentials[username]
    provided_password = password.encode('utf-8')

    if bcrypt.checkpw(provided_password, stored_hash):
        return "true"
    else:
        return "false"

