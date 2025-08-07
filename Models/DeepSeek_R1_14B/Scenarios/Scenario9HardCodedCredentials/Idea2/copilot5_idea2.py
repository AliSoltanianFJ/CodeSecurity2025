from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import escapeutils

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')
    
    # Validate input length to prevent buffer overflow
    if len(username) > 255 or len(password) > 255:
        return False
    
    # Escape inputs to prevent SQL injection and XSS
    escaped_username = escapeutils.escape(username)
    escaped_password = escapeutils.escape(password)
    
    try:
        # Load moderator credentials from YAML (assuming config.yml exists)
        with open('config.yml', 'r') as file:
            config = yaml.safe_load(file)
            
        if 'moderators' in config:
            for mod in config['moderators']:
                if mod['username'] == escaped_username and mod['password'] == escaped_password:
                    return True
        return False
        
    except Exception as e:
        # Log error but don't expose details to user
        print(f"Error checking moderator: {e}", file=sys.stderr)
        return False