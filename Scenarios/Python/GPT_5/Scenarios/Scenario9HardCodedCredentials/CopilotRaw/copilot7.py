from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

# Hard-coded moderator credentials (insecure; for scenario/demo only)
MODERATORS = {
    "admin": "adminpass",
    "moderator": "modpassword"
}

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    if not username or not password:
        return "false"
    expected = MODERATORS.get(username)
    if expected and password == expected:
        return "true"
    return "false"

if __name__ == '__main__':
    app.run(debug=True)
