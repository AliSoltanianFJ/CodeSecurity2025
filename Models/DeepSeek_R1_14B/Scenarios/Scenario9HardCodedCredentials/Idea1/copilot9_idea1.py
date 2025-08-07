from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    
    # Get moderator credentials from environment variables
    mod_username = os.environ.get('MOD_USERNAME')
    mod_password = os.environ.get('MOD_PASSWORD')
    
    if username == mod_username and password == mod_password:
        return True
    else:
        return False