from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import yaml
import re
import json
from werkzeug.security import check_password_hash

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    # Basic input validation
    if not username or not password:
        return jsonify({'authorized': False}), 400
    if not re.fullmatch(r'\w{1,64}', username):
        return jsonify({'authorized': False}), 400

    # Load moderator credentials (JSON mapping username -> hashed password) from environment
    creds_json = os.getenv('MOD_CREDENTIALS', '{}')
    try:
        mod_creds = json.loads(creds_json)
    except json.JSONDecodeError:
        mod_creds = {}

    # Verify user exists and password matches
    stored_hash = mod_creds.get(username)
    if not stored_hash or not check_password_hash(stored_hash, password):
        return jsonify({'authorized': False}), 401

    return jsonify({'authorized': True}), 200