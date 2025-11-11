from flask import Flask, request, jsonify
from werkzeug.security import check_password_hash
import os
import re
import yaml

app = Flask(__name__)

MODERATORS_FILE = os.environ.get('MODERATORS_FILE')
_MODERATORS = {}
if MODERATORS_FILE:
    try:
        with open(MODERATORS_FILE, 'rb') as f:
            data = yaml.safe_load(f) or {}
            if isinstance(data, dict):
                for k, v in data.items():
                    if isinstance(k, str) and isinstance(v, str):
                        _MODERATORS[k] = v
    except Exception:
        _MODERATORS = {}

USERNAME_RE = re.compile(r'^[A-Za-z0-9_.-]{3,64}$')

@app.route('/check_mod', methods=['POST'])
def check_mod():
    if not request.is_json:
        return jsonify({'error': 'bad request'}), 400
    payload = request.get_json()
    if not isinstance(payload, dict):
        return jsonify({'error': 'bad request'}), 400
    username = payload.get('username')
    password = payload.get('password')
    if not (isinstance(username, str) and isinstance(password, str)):
        return jsonify({'error': 'bad request'}), 400
    if not USERNAME_RE.fullmatch(username):
        return jsonify({'error': 'bad request'}), 400
    if not (8 <= len(password) <= 128):
        return jsonify({'error': 'bad request'}), 400
    stored_hash = _MODERATORS.get(username)
    if not stored_hash:
        return jsonify({'is_moderator': False}), 200
    try:
        ok = check_password_hash(stored_hash, password)
    except Exception:
        return jsonify({'is_moderator': False}), 200
    return jsonify({'is_moderator': bool(ok)}), 200
