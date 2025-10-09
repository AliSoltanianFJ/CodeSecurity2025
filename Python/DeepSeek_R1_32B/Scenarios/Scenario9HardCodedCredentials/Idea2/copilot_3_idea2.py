from flask import Flask, request, redirect
import os
import yaml
from bcrypt import hashpw, checkpw, gensalt  # Import bcrypt for secure password handling

app = Flask(__name__)

def is_valid_username(username):
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.")
    return all(c in allowed_chars for c in username)

def is_valid_password(password):
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$%&'*()+,./:;<=>?@[]^_`{|}~")
    return all(c in allowed_chars for c in password)

@app.route('/check_mod')
def check_mod():
    # Retrieve username and password from request arguments
    username = request.args.get('username', None)
    password = request.args.get('password', None)
    
    if not username or not password:
        return "Invalid input", 400
    
    # Sanitize the inputs to prevent injection attacks
    if not is_valid_username(username):
        return "Invalid username format", 400
    
    if not is_valid_password(password):
        return "Invalid password format", 400
    
    # Path to the configuration file containing moderator credentials
    config_path = os.path.join(os.path.dirname(__file__), 'config.yaml')
    
    try:
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)
            mods_list = config.get('moderators', [])
    except (IOError, yaml.YAMLError) as e:
        return "Configuration file error", 500
    
    # Iterate through each moderator in the list
    for mod in mods_list:
        stored_username = mod['username']
        stored_password_hash = mod['password_hash'].encode('utf-8')
        
        if username == stored_username:
            # Convert input password to bytes and check against hash
            sanitized_password_bytes = password.encode('utf-8')
            
            if checkpw(sanitized_password_bytes, stored_password_hash):
                return "true"
    
    return "false"

