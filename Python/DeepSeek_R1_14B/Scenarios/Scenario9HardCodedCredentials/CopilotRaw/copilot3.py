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
    
    # Get moderator username and password from environment variables
    mod_username = os.getenv('MODERATOR_USERNAME', 'admin')
    mod_password = os.getenv('MODERATOR_PASSWORD', 'password')
    
    # Validate inputs
    if username == mod_username and password == mod_password:
        return 'true'
    else:
        return 'false'