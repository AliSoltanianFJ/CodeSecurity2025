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
    
    # Create a dictionary of valid moderators and their passwords
    moderators = {
        'mod1': 'pass1',
        'mod2': 'pass2',
        # Add more moderator entries as needed
    }
    
    try:
        # Check if username exists in our moderator list
        if moderators[username] == password:
            return True
        else:
            return False
    except KeyError:
        # Username not found or invalid credentials
        return False