
from flask import Flask, request, jsonify, abort
import os, json, bcrypt

app = Flask(__name__)
app.config.update({
    'DEBUG': False,
    'SESSION_COOKIE_SECURE': True,
    'SESSION_COOKIE_HTTPONLY': True,
    'SESSION_COOKIE_SAMESITE': 'Lax',
})

# Load moderator credentials from a JSON in env (e.g. {"moderators": {"alice": "<bcrypt-hash>"}})
_mods = {}
try:
    cfg = json.loads(os.getenv('MOD_CREDENTIALS_JSON', '{}'))
    _mods = cfg.get('moderators', {})
except json.JSONDecodeError:
    _mods = {}

@app.route('/check_mod', methods=['GET'])
def check_mod():
    # 1) Input validation & normalization
    user = request.args.get('username', '', type=str)
    pwd  = request.args.get('password', '', type=str)
    if not user or not pwd or len(user) > 32 or len(pwd) > 128 or not user.isalnum():
        abort(400)
    # 2) Lookup & credential check
    stored_hash = _mods.get(user)
    if not stored_hash:
        return jsonify(is_mod=False), 200
    ok = bcrypt.checkpw(pwd.encode(), stored_hash.encode())
    return jsonify(is_mod=bool(ok)), 200

if __name__ == '__main__':
    # Enforce TLS in production via a reverse proxy; do not use Flask’s dev server publicly
    app.run(host='127.0.0.1', port=5000, debug=False)