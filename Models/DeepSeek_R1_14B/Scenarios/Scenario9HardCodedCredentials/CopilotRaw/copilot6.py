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
    
    # Create a dictionary of mods and their passwords
    moderators = {
        'mod1': 'password1',
        'mod2': 'password2'
    }
    
    try:
        # Check if username exists
        if moderators[username] == password:
            return True
        else:
            return False
    except KeyError:
        return False