
from flask import Flask, request, abort, jsonify
import os
import yaml
import re
from passlib.hash import bcrypt
from werkzeug.middleware.proxy_fix import ProxyFix

app = Flask(__name__)
app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1)
app.config['DEBUG'] = False  # enforce non-debug in all environments

# Load credentials from a fixed path or env var; file must be owner-only readable
_CRED_FILE = os.getenv('MOD_CREDENTIALS_FILE', r'd:\secure\moderators.yaml')
try:
    with open(_CRED_FILE, 'r', encoding='utf-8') as f:
        _MOD_CREDENTIALS = yaml.safe_load(f) or {}
except Exception:
    _MOD_CREDENTIALS = {}
    app.logger.error("Unable to load moderator credentials", exc_info=False)

@app.route('/check_mod', methods=['GET'])
def check_mod():
    # Enforce HTTPS in production
    if app.env == 'production' and not request.is_secure:
        abort(403, description="HTTPS required")

    # Extract and validate inputs
    username = request.args.get('username', type=str)
    password = request.args.get('password', type=str)
    if not username or not password:
        abort(400, description="Missing username or password")

    # Whitelist: allow only alphanumerics, dash, underscore; 3–64 chars
    if not re.fullmatch(r'[A-Za-z0-9_-]{3,64}', username):
        abort(400, description="Invalid username format")

    # Lookup stored bcrypt hash
    stored_hash = _MOD_CREDENTIALS.get(username)
    if not stored_hash:
        return jsonify(is_moderator=False), 200

    # Verify password using bcrypt (constant-time under the hood)
    try:
        if bcrypt.verify(password, stored_hash):
            return jsonify(is_moderator=True), 200
        else:
            return jsonify(is_moderator=False), 200
    except Exception:
        # Do not reveal internal errors
        return jsonify(is_moderator=False), 200
# ...existing code...