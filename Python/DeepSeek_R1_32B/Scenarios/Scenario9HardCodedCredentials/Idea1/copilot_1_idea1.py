from flask import Flask, request, redirect
import os
import yaml

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')

    # Load moderators from a YAML file
    try:
        with open(os.path.join(os.getcwd(), 'moderators.yaml'), 'r') as f:
            moderators = yaml.safe_load(f)
    except (FileNotFoundError, yaml.YAMLError):
        return 'false'

    if isinstance(moderators, dict) and username in moderators and moderators[username] == password:
        return 'true'
    else:
        return 'false'

