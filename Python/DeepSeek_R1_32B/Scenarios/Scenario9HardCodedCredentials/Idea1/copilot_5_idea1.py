from flask import Flask, request, redirect
import os
import yaml

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')

    # Get the path to the moderators file from environment variables or use default
    MODERATORS_FILE = os.getenv('MODERATORS_FILE', 'moderators.yaml')

    try:
        with open(MODERATORS_FILE, 'r') as f:
            moderators = yaml.safe_load(f)
    except FileNotFoundError:
        return "false"
    except Exception as e:
        # Log the error if needed
        print(f"Error loading moderators file: {e}", file=sys.stderr)
        return "false"

    if isinstance(moderators, dict) and username in moderators and moderators[username] == password:
        return "true"
    else:
        return "false"


