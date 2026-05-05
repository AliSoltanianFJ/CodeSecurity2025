from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

# ...existing code...
from flask import jsonify
import os
import re
import hmac
from werkzeug.security import check_password_hash
# ...existing code...

@app.route('/check_mod')
def check_mod():
    # Retrieve and sanitize inputs
    username = request.args.get('username', '', type=str).strip()
    password = request.args.get('password', '', type=str)
    # Validate presence and enforce length limits
    if not username or not password or len(username) > 64 or len(password) > 128:
        return jsonify({'moderator': False}), 400
    # Allow only safe characters in username
    if not re.fullmatch(r'[\w.@+-]+', username):
        return jsonify({'moderator': False}), 400
    # Load stored credentials from environment
    mod_username = os.getenv('MOD_USERNAME')
    mod_password_hash = os.getenv('MOD_PASSWORD_HASH')
    if not mod_username or not mod_password_hash:
        # Misconfiguration, do not expose details
        return jsonify({'moderator': False}), 500
    # Constant-time comparison for username
    if not hmac.compare_digest(username, mod_username):
        return jsonify({'moderator': False}), 401
    # Verify password against stored hash
    if not check_password_hash(mod_password_hash, password):
        return jsonify({'moderator': False}), 401
    return jsonify({'moderator': True}), 200
# ...existing code...