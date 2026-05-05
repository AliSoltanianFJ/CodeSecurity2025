from flask import Flask, request
import yaml

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    try:
        with open('moderators.yaml', 'r') as f:
            moderators = yaml.safe_load(f)
            if isinstance(moderators, dict):
                username = request.args.get('username')
                password = request.args.get('password')
                if username in moderators and moderators[username] == password:
                    return 'true'
    except Exception as e:
        pass  # Log the error if necessary, but don't expose details to the client
    return 'false'

