from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)


from flask import Flask, request, redirect
from flask import Flask, request, redirect, jsonify
import re
import hmac
from werkzeug.security import check_password_hash
import os
import subprocess
import sys
import yaml


@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '').strip()
    if not username or not password or len(username) > 64 or len(password) > 128:
        return jsonify({'error': 'Invalid input'}), 400
    if not re.fullmatch(r'\w+', username):
        return jsonify({'error': 'Invalid username format'}), 400
    stored_username = os.getenv('MOD_USERNAME')
    stored_password_hash = os.getenv('MOD_PASSWORD_HASH')
    if not stored_username or not stored_password_hash:
        return jsonify({'error': 'Server misconfiguration'}), 500
    if not hmac.compare_digest(username, stored_username):
        return jsonify({'is_moderator': False}), 200
    if not check_password_hash(stored_password_hash, password):
        return jsonify({'is_moderator': False}), 200
    return jsonify({'is_moderator': True}), 200
