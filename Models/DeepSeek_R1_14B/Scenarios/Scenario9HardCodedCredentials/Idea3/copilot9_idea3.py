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
    
    # Assuming there is an existing method to check if a user is a moderator
    return check_user_mod(username, password)

def check_user_mod(username, password):
    # Incomplete for this example
    return False