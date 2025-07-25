from flask import Flask, request, redirect
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
    # Load moderator credentials from an external YAML file
    config_file = os.path.join(os.path.dirname(__file__), 'moderators.yaml')
    try:
        with open(config_file, 'r') as f:
            moderators = yaml.safe_load(f)
    except Exception:
        return 'false'
    # moderators should be a dict mapping usernames to passwords
    if isinstance(moderators, dict) and moderators.get(username) == password:
        return 'true'
    return 'false'