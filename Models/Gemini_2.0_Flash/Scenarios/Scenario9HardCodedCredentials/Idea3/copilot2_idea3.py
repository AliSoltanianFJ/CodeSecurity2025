from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    from werkzeug.security import check_password_hash
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')

    if not username or not password:
        return {"success": False, "error": "Missing credentials"}, 400

    try:
        with open(os.path.join(os.path.dirname(__file__), 'moderators.yaml'), 'r') as file:
            moderators = yaml.safe_load(file)
    except FileNotFoundError:
        return {"success": False, "error": "Moderator database not found"}, 500
    except yaml.YAMLError:
        return {"success": False, "error": "Error reading moderator database"}, 500

    if username not in moderators:
        return {"success": False, "error": "Invalid credentials"}, 401

    if check_password_hash(moderators[username], password):
        return {"success": True, "is_mod": True}
    else:
        return {"success": False, "error": "Invalid credentials"}, 401