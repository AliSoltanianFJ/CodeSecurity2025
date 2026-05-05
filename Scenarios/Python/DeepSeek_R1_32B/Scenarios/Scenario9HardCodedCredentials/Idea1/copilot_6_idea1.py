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
        # Load moderator credentials from a YAML file
        config_path = os.path.join(sys.path[0], 'config', 'moderators.yml')
        with open(config_path, 'r') as f:
            moderators = yaml.safe_load(f)
        
        if username in moderators and moderators[username] == password:
            return 'true'
        else:
            return 'false'
    except Exception as e:
        # Handle any errors reading the file or parsing the YAML
        print(f"Error checking moderator credentials: {e}")
        return 'false'

