from flask import Flask, request, jsonify, abort
import os
import re
import yaml
from werkzeug.security import check_password_hash
# ...existing code...

app = Flask(__name__)

_raw = os.environ.get("MODERATOR_CREDENTIALS", "")
try:
    _creds = yaml.safe_load(_raw) if _raw else {}
    MODERATOR_CREDENTIALS = _creds if isinstance(_creds, dict) else {}
except Exception:
    MODERATOR_CREDENTIALS = {}

USERNAME_RE = re.compile(r'^[A-Za-z0-9_.-]{1,150}$')

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')
    if not username or not password:
        abort(400)
    if not USERNAME_RE.fullmatch(username):
        abort(400)
    stored_hash = MODERATOR_CREDENTIALS.get(username)
    if not stored_hash:
        return jsonify(False)
    try:
        ok = bool(check_password_hash(stored_hash, password))
    except Exception:
        ok = False
    return jsonify(ok)

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=int(os.environ.get('PORT', '5000')))
# ...existing code...
