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

    # Load hard-coded moderator credentials
    config_path = os.path.join(os.path.dirname(__file__), 'moderators.yaml')
    with open(config_path) as f:
        config = yaml.safe_load(f)
    mods = config.get('moderators', {})

    if mods.get(username) == password:
        return 'true'
    return 'false'