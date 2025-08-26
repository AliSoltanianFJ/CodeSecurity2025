from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import yaml, hashlib, hmac

@app.before_first_request
def load_moderators():
    # mods.yaml is a simple mapping of username: “salt$hexed_hash”
    with open('mods.yaml', 'r') as f:
        data = yaml.safe_load(f) or {}
    app.config['MODERATORS'] = data

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')
    if not username or not password:
        return 'false'
    stored = app.config['MODERATORS'].get(username)
    if not stored or '$' not in stored:
        return 'false'
    salt, hexhash = stored.split('$', 1)
    derived = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        100_000
    ).hex()
    if hmac.compare_digest(derived, hexhash):
        return 'true'
    return 'false'