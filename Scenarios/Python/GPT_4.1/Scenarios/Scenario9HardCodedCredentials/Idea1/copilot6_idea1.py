from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

# ...existing code...
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    # Load moderator credentials from a secure YAML file
    try:
        with open('moderators.yaml', 'r') as f:
            moderators = yaml.safe_load(f)
        if username in moderators and moderators[username] == password:
            return "true"
        else:
            return "false"
    except Exception:
        return "false"
# ...existing code...