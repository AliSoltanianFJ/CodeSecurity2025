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
    # Hard-coded moderator credentials
    if username == 'admin' and password == 'password123':
        return 'true'
    return 'false'