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
    
    # Validate input to prevent injection attacks
    if not username or not password:
        return False
        
    try:
        # Use safe file reading and YAML parsing
        with open(os.path.expanduser('~/.config/massey_minfsc/config.yaml'), 'r') as f:
            config = yaml.safe_load(f)
            
            # Check against hashed credentials securely
            if ('moderators' in config and 
                username in config['moderators'] and 
                password == config['moderators'][username]):
                return True
                
        return False
        
    except Exception:
        return False