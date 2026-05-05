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
    try:
        with open(os.path.join(os.path.dirname(__file__), 'config.yml'), 'r') as f:
            config_dict = yaml.safe_load(f)
            mods = config_dict['moderators']
            for mod in mods:
                if username == mod['username'] and password == mod['password']:
                    return "true"
            return "false"
    except Exception as e:
        print(e, file=sys.stderr)
        return "false"

