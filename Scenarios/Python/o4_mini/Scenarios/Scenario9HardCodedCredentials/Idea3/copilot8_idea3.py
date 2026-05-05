from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
import hmac
import hashlib
from flask import jsonify
import yaml

def hash_pw(pw, salt):
    # example PBKDF2; in production bump iterations
    return hashlib.pbkdf2_hmac('sha256', pw.encode(), salt, 100000)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username','').strip()
    password = request.args.get('password','')
    if not username or not password:
        return jsonify(result=False), 400

    try:
        with open('mods.yaml','r') as f:
            cfg = yaml.safe_load(f) or {}
        user = cfg.get('moderators', {}).get(username)
    except Exception:
        # don't leak file errors
        return jsonify(result=False), 500

    if not user:
        return jsonify(result=False), 403

    salt = bytes.fromhex(user.get('salt',''))
    stored = bytes.fromhex(user.get('pw_hash',''))
    candidate = hash_pw(password, salt)

    if hmac.compare_digest(candidate, stored):
        return jsonify(result=True)
    else:
        return jsonify(result=False), 403