
from flask import Flask, request
import os
import yaml

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')

    # Read moderators from environment variable using YAML for security
    moderator_env = os.getenv('MODERATORS', '')
    try:
        moderators = yaml.safe_load(moderator_env)
        if isinstance(moderators, dict):
            pass  # Use the loaded dictionary
        else:
            moderators = {}  # Fallback to empty dict if not a valid dict
    except yaml.YAMLError:
        moderators = {}

    # Check if username exists and password matches
    if username in moderators and moderators[username] == password:
        return 'true'
    else:
        return 'false'

