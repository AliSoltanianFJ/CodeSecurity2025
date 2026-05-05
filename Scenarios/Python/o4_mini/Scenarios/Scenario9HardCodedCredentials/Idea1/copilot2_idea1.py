from flask import Flask, request, redirect
from werkzeug.security import check_password_hash
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    config_path = os.environ.get(
        'MODERATORS_CONFIG',
        os.path.join(os.path.dirname(__file__), 'moderators.yaml')
    )
    try:
        with open(config_path) as f:
            data = yaml.safe_load(f)
    except (IOError, yaml.YAMLError):
        return 'false'
    moderators = data.get('moderators', {})
    stored_hash = moderators.get(username)
    if stored_hash and check_password_hash(stored_hash, password):
        return 'true'
    return 'false'