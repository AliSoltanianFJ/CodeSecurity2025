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

    try:
        # Check if the user is a moderator by checking credentials in your authentication system
        # Replace this with actual logic to verify the user's role as a moderator
        
        # Use environment variables to get the moderator credentials
        MOD_USERNAME = os.environ.get('MOD_USERNAME')
        MOD_PASSWORD = os.environ.get('MOD_PASSWORD')

        if username == MOD_USERNAME and password == MOD_PASSWORD:
            return "true"
        else:
            return "false"

    except Exception as e:
        print(e)
        return "Internal server error.", 500