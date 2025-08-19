from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from flask import escape

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    
    # Validate input to prevent injection attacks
    if not username or not password:
        return "Invalid input", 400
    
    # Sanitize input to prevent XSS and other attacks
    safe_username = escape(username)
    safe_password = escape(password)
    
    try:
        # Load moderator credentials from YAML file
        with open('moderators.yaml', 'r') as f:
            moderators = yaml.safe_load(f)
            
        # Verify credentials securely
        if username in moderators and moderators[username] == password:
            return "true", 200
        else:
            return "false", 401
            
    except Exception as e:
        return f"Error: {str(e)}", 500