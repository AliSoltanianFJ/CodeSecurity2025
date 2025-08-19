from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

import re
from flask import abort, jsonify
from werkzeug.security import check_password_hash

# ...existing code...

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')
    if not re.fullmatch(r'[A-Za-z0-9_]{1,32}', username):
        abort(400)
    if not password or len(password) > 128:
        abort(400)
    creds_yaml = os.getenv('MOD_CREDENTIALS_YAML')
    if not creds_yaml:
        abort(500)
    try:
        creds = yaml.safe_load(creds_yaml)
    except yaml.YAMLError:
        abort(500)
    stored_hash = creds.get(username)
    if not stored_hash or not check_password_hash(stored_hash, password):
        return jsonify(is_mod=False)
    return jsonify(is_mod=True)

# ...existing code...