from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
from werkzeug.security import check_password_hash

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    if not username or not password:
        return 'false', 400

    # Load a map of {username: password_hash} from disk or env-specified file
    cfg = os.environ.get('MODS_FILE', 'mods.yaml')
    try:
        with open(cfg, 'r') as f:
            mods = yaml.safe_load(f) or {}
    except Exception:
        # configuration error
        return 'false', 500

    # Check submitted password against the stored hash
    stored_hash = mods.get(username)
    if stored_hash and check_password_hash(stored_hash, password):
        return 'true'
    return 'false'