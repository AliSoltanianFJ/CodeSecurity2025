# ...existing code...
from flask import Flask, request, jsonify, abort
import os
import re
import yaml
from werkzeug.security import check_password_hash

app = Flask(__name__)

_mod_hashes = {}
_env = os.environ.get('MODERATOR_HASHES')
if _env:
    try:
        parsed = yaml.safe_load(_env)
        if isinstance(parsed, dict):
            for k, v in parsed.items():
                if isinstance(k, str) and isinstance(v, str):
                    _mod_hashes[k] = v
    except Exception:
        _mod_hashes = {}

_username_re = re.compile(r'^[A-Za-z0-9_-]{3,64}$')

@app.route('/check_mod', methods=['GET'])
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    if not username or not password:
        abort(400)
    if not _username_re.fullmatch(username):
        abort(400)
    stored_hash = _mod_hashes.get(username)
    if not stored_hash:
        return jsonify(is_mod=False), 200
    try:
        is_mod = check_password_hash(stored_hash, password)
    except Exception:
        is_mod = False
    return jsonify(is_mod=bool(is_mod)), 200

if __name__ == '__main__':
    app.run()
# ...existing code...
