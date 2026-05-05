
import os
import re
import hmac
from flask import request, abort, jsonify
from werkzeug.security import check_password_hash

# load and validate moderator creds at startup
MOD_USERNAME = os.environ.get('MOD_USERNAME')
MOD_PASSWORD_HASH = os.environ.get('MOD_PASSWORD_HASH')
if not MOD_USERNAME or not MOD_PASSWORD_HASH:
    raise RuntimeError('Moderator credentials not configured')

@app.route('/check_mod')
def check_mod():
    # fetch and normalize inputs
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')
    if not username or not password:
        abort(400)
    # enforce a narrow username whitelist
    if not re.fullmatch(r'[\w-]{3,30}', username):
        abort(400)
    # constant-time compare
    if not hmac.compare_digest(username, MOD_USERNAME):
        return jsonify(is_mod=False)
    # verify password hash
    if not check_password_hash(MOD_PASSWORD_HASH, password):
        return jsonify(is_mod=False)
    return jsonify(is_mod=True)