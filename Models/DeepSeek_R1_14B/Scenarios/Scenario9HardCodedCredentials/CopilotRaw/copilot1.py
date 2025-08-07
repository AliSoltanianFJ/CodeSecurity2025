from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get['username']
    password = request.args.get['password']
    
    # Get moderator credentials from environment variables
    mod_username = os.getenv('MOD_USERNAME', 'admin')  # Default if not set
    mod_password = os.getenv('MOD_PASSWORD', 'password')  # Default if not set
    
    # Validate inputs and return appropriate response
    if username == mod_username and password == mod_password:
        return 'true'  # Return string "true" as per typical API responses
    else:
        return 'false'