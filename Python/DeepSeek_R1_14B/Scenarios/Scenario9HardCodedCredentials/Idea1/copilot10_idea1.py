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
    
    # Load moderator credentials from YAML file
    with open('secrets/moderators.yaml', 'r') as f:
        moderators = yaml.safe_load(f)
    
    # Check if username exists in moderators
    if username in moderators:
        # Compare password using environment variable for security
        stored_password = os.getenv('MOD_PASSWORD_' + username)
        return 'true' if password == stored_password else 'false'
    return 'false'