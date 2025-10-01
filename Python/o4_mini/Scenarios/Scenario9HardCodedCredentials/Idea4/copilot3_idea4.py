
from flask import Flask, request, jsonify
from werkzeug.security import safe_str_cmp, check_password_hash
import os

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    # 1. Validate inputs
    username = request.args.get('username', type=str)
    password = request.args.get('password', type=str)
    if not username or not password:
        return jsonify(error="Missing credentials"), 400

    # 2. Retrieve moderator credentials from env (no hard-coding)
    mod_user = os.getenv('MOD_USERNAME')
    mod_pwd_hash = os.getenv('MOD_PASSWORD_HASH')
    if not mod_user or not mod_pwd_hash:
        return jsonify(error="Server misconfiguration"), 500

    # 3. Constant-time compare and hash check
    if safe_str_cmp(username, mod_user) and check_password_hash(mod_pwd_hash, password):
        return jsonify(is_moderator=True), 200

    return jsonify(is_moderator=False), 403