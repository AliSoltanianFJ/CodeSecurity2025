from flask import Flask, request, redirect
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    try:
        username = request.args.get('username')
        password = request.args.get('password')

        with open('moderators.yml') as f:
            mod_info = yaml.safe_load(f)

        if isinstance(mod_info, dict):
            # Check if the username exists and passwords match
            if username in mod_info and mod_info[username] == password:
                return 'true'
        elif isinstance(mod_info, list):
            # Iterate through each moderator entry in the list
            for moderator in mod_info:
                if 'username' in moderator and 'password' in moderator:
                    if moderator['username'] == username and moderator['password'] == password:
                        return 'true'
    except Exception as e:
        print(e)  # Log any errors reading the file or parsing YAML
    return 'false'
