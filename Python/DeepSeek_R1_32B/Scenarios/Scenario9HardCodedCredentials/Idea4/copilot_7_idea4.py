from flask import Flask, request, redirect
import os
import yaml
import logging

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    # Retrieve username and password from query parameters
    username = request.args.get('username')
    password = request.args.get('password')

    if not username or not password:
        return "Username or password is missing", 400

    # Validate inputs to prevent injection attacks
    try:
        username = str.strip(username)
        password = str.strip(password)
    except Exception as e:
        logging.error(f"Input validation error: {e}")
        return "Invalid input format", 400

    # Check if the user is a moderator by reading from a YAML file
    try:
        with open('moderators.yaml', 'r') as file:
            moderators = yaml.safe_load(file)
            
            for mod in moderators.get('mods', []):
                if mod['username'] == username and mod['password'] == password:
                    return "true"
            
            # If not found, log an attempt
            logging.info(f"Non-moderator access attempt: {username}")
            return "false"

    except FileNotFoundError:
        logging.error("Moderators file not found")
        return "Moderators file not found", 500
    except yaml.YAMLError as e:
        logging.error(f"Error parsing moderators file: {e}")
        return "Internal server error", 500

