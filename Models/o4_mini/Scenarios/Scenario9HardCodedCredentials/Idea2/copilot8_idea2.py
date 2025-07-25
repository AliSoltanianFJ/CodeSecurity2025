from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
import re
import json
from flask import jsonify, abort
from werkzeug.security import check_password_hash


# Load moderator credentials securely from environment
try:
    mod_credentials = json.loads(os.environ.get('MOD_CREDENTIALS', '{}'))
except ValueError:
    mod_credentials = {}

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')
    # Validate presence
    if not username or not password:
        abort(400)
    # Length checks
    if len(username) > 50 or len(password) > 128:
        abort(400)
    # Allowed characters
    if not re.fullmatch(r'[A-Za-z0-9_]+', username):
        abort(400)
    # Retrieve stored hash
    stored_hash = mod_credentials.get(username)
    if not stored_hash:
        return jsonify({'is_mod': False}), 200
    # Verify password hash securely
    try:
        is_valid = check_password_hash(stored_hash, password)
    except (ValueError, TypeError):
        is_valid = False
    return jsonify({'is_mod': is_valid}), 200

# ...existing code...