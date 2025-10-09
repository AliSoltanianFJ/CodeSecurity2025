from flask import Flask, request
import os
import yaml

app = Flask(__name__)

"""Return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    # Path to the moderators configuration file
    config_path = os.path.join(os.path.dirname(__file__), '..', 'config', 'moderators.yaml')

    try:
        with open(config_path, 'r') as f:
            moderators = yaml.safe_load(f)
            if isinstance(moderators, dict) and username in moderators and moderators[username] == password:
                return 'true'
    except (IOError, yaml.YAMLError) as e:
        # Handle file not found or parsing errors
        print(f"Error reading moderators configuration: {e}", file=sys.stderr)

    return 'false'

