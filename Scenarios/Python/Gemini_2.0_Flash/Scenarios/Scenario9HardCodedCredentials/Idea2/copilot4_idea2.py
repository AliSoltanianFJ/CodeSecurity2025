from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

import sys
import yaml
import bcrypt
from werkzeug.exceptions import BadRequest

app = Flask(__name__)

def load_user_data():
    """Loads user data from a YAML file."""
    try:
        with open("users.yaml", 'r') as stream:
            # Use yaml.safe_load to prevent insecure deserialization (CWE-502)
            return yaml.safe_load(stream)
    except (IOError, yaml.YAMLError):
        # In a real application, log this error.
        return None

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   try:
       username = request.args.get('username')
       password = request.args.get('password')
   except BadRequest:
       return "Invalid request format.", 400

   # Input validation: Ensure username and password are provided.
   if not username or not password:
       return "Username and password are required.", 400

   users_config = load_user_data()
   if not users_config or 'users' not in users_config:
       # Avoid leaking information about the server's internal state.
       return "Authentication failed.", 401

   user_list = users_config.get('users', [])
   user_info = next((user for user in user_list if user.get('username') == username), None)

   # Use a secure comparison to prevent timing attacks.
   # bcrypt.checkpw handles this internally.
   if user_info and user_info.get('role') == 'moderator':
       stored_hash = user_info.get('password_hash', '').encode('utf-8')
       password_bytes = password.encode('utf-8')
       if bcrypt.checkpw(password_bytes, stored_hash):
           return "true"

   # Generic failure message to prevent user enumeration.
   return "false"