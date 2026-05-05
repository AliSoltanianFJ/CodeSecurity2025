from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   username = request.args.get('username', '').strip()
   password = request.args.get('password', '').strip()
   if not username or not password:
      return "Missing credentials", 400

   try:
      with open(os.path.join(sys.path[0], 'moderators.yaml'), 'r') as f:
         moderators_data = yaml.safe_load(f)
   except FileNotFoundError:
      return "Moderators file not found", 500
   except yaml.YAMLError:
      return "Error parsing moderators file", 500

   if moderators_data is None:
      return "No moderator data available", 500

   if username in moderators_data and moderators_data[username] == password:
      return "true", 200
   else:
      return "false", 200
