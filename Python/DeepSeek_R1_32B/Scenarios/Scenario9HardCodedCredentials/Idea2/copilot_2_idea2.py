from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # Get username and password from query parameters
    username = request.args.get('username')
    password = request.args.get('password')

    # Validate inputs are not empty
    if not username or not password:
        return "Invalid credentials", 400

    # Sanitize input to prevent XSS attacks
    from bleach import clean
    clean_username = clean(username.strip()).lower()
    clean_password = clean(password.strip()).lower()

    try:
        # Read moderator list from a secure file
        with open('moderators.yaml', 'r') as f:
            moderators = yaml.safe_load(f)
            
        # Check if the user is a moderator and credentials match
        return str(clean_username in moderators and clean_password == moderators[clean_username])
        
    except (FileNotFoundError, KeyError) as e:
        return "Access denied", 403

