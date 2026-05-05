from flask import Flask, request
import yaml
from werkzeug.security import check_password_hash

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')
    if not username or not password:
        return 'false', 400

    try:
        with open('moderators.yml', 'r') as f:
            cfg = yaml.safe_load(f) or {}
        mods = cfg.get('moderators', {})
    except Exception:
        return 'false', 500

    stored_hash = mods.get(username)
    if stored_hash and check_password_hash(stored_hash, password):
        return 'true'
    return 'false'