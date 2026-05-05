
import os
import re
import yaml
import bcrypt
from flask import Flask, request, abort, jsonify

app = Flask(__name__)
app.config['DEBUG'] = False  # secure default

# 1) Securely locate and load moderator credentials at startup
MODS_FILE_PATH = os.environ.get(
    'MODS_FILE_PATH',
    r'd:\secure\config\moderators.yaml'  # default secure path; override via env
)
try:
    with open(os.path.abspath(MODS_FILE_PATH), 'r', encoding='utf-8') as f:
        _MODERATORS = yaml.safe_load(f) or {}
except Exception:
    _MODERATORS = {}
    # In a real app you would log this failure to a secure audit log

# 2) Return JSON { "is_mod": true|false } if credentials match
@app.route('/check_mod', methods=['GET'])
def check_mod():
    # Input validation & canonicalization
    username = request.args.get('username', type=str, default='').strip()
    password = request.args.get('password', type=str, default='')
    if not username or not password:
        abort(400, 'Missing required parameters')

    # Whitelist username format: 3–30 alphanumeric or underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400, 'Invalid username format')

    # Lookup hashed password from in‐memory dict
    stored_hash = _MODERATORS.get(username)
    if not stored_hash:
        # user not found → not a moderator
        return jsonify({"is_mod": False}), 200

    # Use bcrypt for constant-time, safe comparison
    try:
        is_valid = bcrypt.checkpw(password.encode('utf-8'),
                                  stored_hash.encode('utf-8'))
    except ValueError:
        # invalid hash format
        is_valid = False

    return jsonify({"is_mod": is_valid}), 200