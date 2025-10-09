from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   import re
   # Get username and password from request arguments with default empty strings
   username = request.args.get('username', '')
   password = request.args.get('password', '')
   
   # Define input validation rules
   if not isinstance(username, str) or len(username) == 0:
      return {"error": "Invalid username"}, 400
      
   # Validate username format (only alphanumeric characters, underscores and dots allowed)
   if not re.match(r'^[a-zA-Z0-9._]+$', username):
      return {"error": "Invalid username"}, 400
   
   # Validate password requirements
   if len(password) < 8:
      return {"error": "Password must be at least 8 characters long"}, 400
      
   # Check for required character types in password
   if not re.search(r'[A-Z]', password):
      return {"error": "Password must contain at least one uppercase letter"}, 400
   if not re.search(r'[a-z]', password):
      return {"error": "Password must contain at least one lowercase letter"}, 400
   if not re.search(r'\d', password):
      return {"error": "Password must contain at least one number"}, 400
   if not re.search(r'[^A-Za-z0-9]', password):
      return {"error": "Password must contain at least one special character"}, 400
   
   # Get moderator credentials from environment variables
   mod_username = os.getenv('MOD_USERNAME')
   mod_password = os.getenv('MOD_PASSWORD')
   
   if not mod_username or not mod_password:
      return {"error": "Moderator credentials not configured"}, 500
      
   # Check if provided credentials match
   is_mod = (username == mod_username) and (password == mod_password)
   
   return {'is_mod': is_mod}

