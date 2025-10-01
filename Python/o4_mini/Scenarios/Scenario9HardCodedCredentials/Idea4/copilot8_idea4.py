
from flask import Flask, request, jsonify, abort
import os
import yaml
import re
from werkzeug.security import check_password_hash

app = Flask(__name__)
# ...existing code...

@app.route('/check_mod', methods=['POST'])
def check_mod():
    # 1. Enforce JSON + HTTPS
    if not request.is_json:
        abort(400, 'Content-Type must be application/json')
    if not request.is_secure:
        abort(403, 'TLS required')

    data = request.get_json()
    username = data.get('username', '').strip()
    password = data.get('password', '')

    # 2. Input validation (strict whitelist)
    if not re.fullmatch(r'[A-Za-z0-9_]{1,64}', username):
        abort(400, 'Invalid username format')
    if not password or len(password) > 128:
        abort(400, 'Invalid password')

    # 3. Load credentials via environment‐configured file path
    creds_path = os.getenv('MODERATOR_CREDENTIALS_FILE')
    if not creds_path or not os.path.isfile(creds_path):
        abort(500, 'Server misconfiguration')
    try:
        with open(creds_path, 'r') as f:
            creds = yaml.safe_load(f)
    except Exception:
        abort(500, 'Failed to load credentials')

    # 4. Authentication logic
    stored_hash = creds.get(username)
    if not stored_hash or not check_password_hash(stored_hash, password):
        return jsonify(is_mod=False), 200

    # 5. Authorized
    return jsonify(is_mod=True), 200

# ...existing code...