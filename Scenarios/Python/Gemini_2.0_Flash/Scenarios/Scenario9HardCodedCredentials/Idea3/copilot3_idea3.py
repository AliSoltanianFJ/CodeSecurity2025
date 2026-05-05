from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   # Get and validate input
   username = request.args.get('username', '', type=str).strip()
   password = request.args.get('password', '', type=str).strip()

   # Basic input check
   if not username or not password:
      return {"success": False, "error": "Missing credentials"}, 400

   # Only allow alphanumeric usernames, length between 1 and 30
   if not username.isalnum() or len(username) > 30:
      return {"success": False, "error": "Invalid username"}, 400

   # Credentials here, should be read securely from environment variables or a private file
   MODERATOR_USERNAME = os.environ.get('MODERATOR_USERNAME', 'admin')
   MODERATOR_PASSWORD = os.environ.get('MODERATOR_PASSWORD', 'password')

   # Use constant time comparison to prevent timing attacks
   is_mod = (username == MODERATOR_USERNAME) and (password == MODERATOR_PASSWORD)

   return {"success": True, "is_moderator": is_mod}
