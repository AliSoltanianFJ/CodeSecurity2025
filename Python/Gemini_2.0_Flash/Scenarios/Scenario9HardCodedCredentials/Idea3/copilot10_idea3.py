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
   if not username or not password:
      return {"success": False, "error": "Missing credentials."}, 400

   try:
      mod_credentials_file = os.path.join(os.path.dirname(file), 'mod_credentials.yaml')
      with open(mod_credentials_file, 'r') as file:
         mod_credentials = yaml.safe_load(file)
   except FileNotFoundError:
      return {"success": False, "error": "Internal server error."}, 500

   if username in mod_credentials and mod_credentials[username] == password:
      return {"success": True, "is_moderator": True}, 200
   else:
      return {"success": True, "is_moderator": False}, 200
