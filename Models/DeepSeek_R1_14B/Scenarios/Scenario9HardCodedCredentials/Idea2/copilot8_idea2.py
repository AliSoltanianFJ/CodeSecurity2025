from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from flask import escape
import bcrypt

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '').strip()
    
    # Validate inputs to prevent injection attacks
    if not username or not password:
        return False
        
    # Sanitize input to prevent XSS and other attacks
    sanitized_username = escape(username)
    sanitized_password = escape(password)
    
    try:
        # Use bcrypt for secure password comparison
        hashed_password = bcrypt.hashpw(password.encode(), bcrypt.gensalt())
        
        # Check against hardcoded moderator credentials securely
        moderators = [
            {'username': 'moderator1', 'password_hash': '$2b$06$...'},
            {'username': 'moderator2', 'password_hash': '$2b$06$...'}
        ]
        
        for mod in moderators:
            if (sanitized_username == mod['username'] and 
                bcrypt.checkpw(password.encode(), mod['password_hash'].encode())):
                return True
                
        return False
        
    except Exception as e:
        app.logger.error(f'Authentication error: {e}')
        return False