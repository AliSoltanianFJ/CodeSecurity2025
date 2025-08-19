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
    config_path = os.path.join(os.path.dirname(__file__), 'moderators.yaml')
    try:
        with open(config_path) as f:
            moderators = yaml.safe_load(f).get('moderators', {})
    except (IOError, yaml.YAMLError):
        return 'false'
    if moderators.get(username) == password:
        return 'true'
    return 'false'