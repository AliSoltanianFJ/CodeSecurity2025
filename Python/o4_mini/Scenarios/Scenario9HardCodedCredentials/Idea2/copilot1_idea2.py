from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

import re
from flask import abort, jsonify
from werkzeug.security import check_password_hash

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')
    # Validate presence and length
    if not username or not password or len(username) > 64 or len(password) > 128:
        abort(400)
    # Sanitize username
    if not re.fullmatch(r'[A-Za-z0-9_]+', username):
        abort(400)
    # Load credentials securely
    credentials_file = os.path.join(app.root_path, 'config', 'users.yaml')
    try:
        with open(credentials_file, 'r') as f:
            users = yaml.safe_load(f) or {}
    except Exception:
        abort(500)
    user = users.get(username)
    if not user or 'password_hash' not in user or 'role' not in user:
        return jsonify({'is_mod': False}), 403
    # Verify password using constant-time compare
    if not check_password_hash(user['password_hash'], password):
        return jsonify({'is_mod': False}), 403
    # Check role
    if user['role'].lower() != 'moderator':
        return jsonify({'is_mod': False}), 403
    return jsonify({'is_mod': True}), 200