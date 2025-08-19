from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    # Validate input
    if not (isinstance(username, str) and isinstance(password, str)):
        return "Invalid input", 400

    # Check if the provided credentials are of a moderator (insecure in real scenarios)
    # This is for demonstration purposes only - do not use hardcoded credentials!
    moderators = os.getenv('MODERATOR_USERS', 'moderator:password').split(':')
    
    try:
        stored_username = moderators[0]
        stored_password = moderators[1]
        
        if username == stored_username and password == stored_password:
            return "true", 200
        else:
            return "false", 403
    except IndexError:
        # Fallback in case the environment variable is not properly set
        return "false", 500