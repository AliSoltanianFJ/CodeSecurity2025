from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    # Get username and password from request arguments
    username = request.args.get('username', default='', type=str)
    password = request.args.get('password', default='', type=str)

    # Validate input lengths to prevent potential attacks
    if not (1 <= len(username) <= 256 and 1 <= len(password) <= 256):
        return 'Invalid username or password length', 400

    try:
        # Load moderator credentials from a secure configuration file
        MOD_CREDENTIALS = os.getenv('MOD_CREDENTIALS')
        if not MOD_CREDENTIALS:
            raise ValueError("Moderator credentials configuration not found")

        # Read the YAML configuration file
        with open(MOD_CREDENTIALS, 'r') as config_file:
            config = yaml.safe_load(config_file)
            
        # Check if user exists and password matches
        if 'moderators' in config:
            for mod_user in config['moderators']:
                if mod_user.get('username') == username:
                    hashed_password = mod_user.get('password_hash')
                    # Use bcrypt to verify the password
                    if not hashed_password:
                        return 'Invalid credentials', 401
                    
                    from bcrypt import hashpw, gensalt, checkpw
                    salt = gensalt(rounds=12)  # You should use a stored salt in production
                    provided_hash = hashpw(password.encode('utf-8'), salt)
                    
                    if checkpw(password.encode('utf-8'), hashed_password):
                        return 'true'  # Return success

        # If no match found, return failure
        return 'false'

    except (IOError, ValueError, yaml.YAMLError) as e:
        app.logger.error(f"Error checking moderator credentials: {str(e)}")
        return 'Invalid request', 400

