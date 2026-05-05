from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import yaml, hmac, hashlib
from flask import Flask, request

app = Flask(__name__)

# Load and index users by username
with open('users.yaml', 'r') as f:
    cfg = yaml.safe_load(f)
USERS = {u['username']: u for u in cfg.get('users', [])}


"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # safely pull parameters
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    # lookup user
    user = USERS.get(username)
    if not user or user.get('role') != 'moderator':
        return 'false'

    # derive hash from supplied password + stored salt
    salt = user.get('salt', '').encode('utf-8')
    # use a strong KDF; e.g. PBKDF2 with many iterations
    derived = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    ).hex()

    # constant-time comparison
    if hmac.compare_digest(derived, user.get('password_hash', '')):
        return 'true'
    return 'false'