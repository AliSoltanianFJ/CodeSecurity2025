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
    config_file = os.getenv('MOD_CREDENTIALS_FILE',
                            os.path.join(os.path.dirname(__file__), 'moderators.yaml'))
    try:
        with open(config_file) as f:
            config = yaml.safe_load(f)
    except (IOError, yaml.YAMLError):
        return 'false'
    moderators = config.get('moderators', {})
    return 'true' if moderators.get(username) == password else 'false'