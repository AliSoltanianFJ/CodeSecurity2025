import hashlib
import hmac
import yaml
from flask import Flask, request
import bcrypt

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')
    if not username or not password:
        return 'false', 400

    try:
        with open('mods.yaml', 'r') as f:
            cfg = yaml.safe_load(f)
    except Exception:
        return 'false', 500

    user = cfg.get('moderators', {}).get(username)
    if not user or 'salt' not in user or 'hash' not in user:
        return 'false', 403

    # Compute candidate hash
    candidate = bcrypt.hashpw(password.encode('utf-8'), user['salt'].encode('utf-8'))
    # Constant-time compare
    if hmac.compare_digest(candidate, user['hash']):
        return 'true'
    return 'false'