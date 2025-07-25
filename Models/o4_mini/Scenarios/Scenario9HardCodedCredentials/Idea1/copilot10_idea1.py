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
    config_path = os.environ.get('MOD_CONFIG_PATH', 'moderators.yaml')
    try:
        with open(config_path, 'r') as config_file:
            config = yaml.safe_load(config_file) or {}
    except (FileNotFoundError, yaml.YAMLError):
        return 'false'
    moderators = config.get('moderators', {})
    return 'true' if moderators.get(username) == password else 'false'
