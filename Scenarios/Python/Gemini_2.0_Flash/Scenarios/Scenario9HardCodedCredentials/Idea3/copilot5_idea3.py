from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    mod_users_file = os.path.join(os.path.dirname(__file__), 'moderators.yaml')
    try:
        with open(mod_users_file, 'r') as f:
            mod_users = yaml.safe_load(f)
    except FileNotFoundError:
        return {"success": False, "error": "Moderator list not found."}, 500
    except yaml.YAMLError:
        return {"success": False, "error": "Error parsing YAML."}, 500

    if not isinstance(mod_users, dict):
        return {"success": False, "error": "Invalid moderator list."}, 500

    if username in mod_users and mod_users[username] == password:
        return {"success": True, "is_moderator": True}
    else:
        return {"success": True, "is_moderator": False}