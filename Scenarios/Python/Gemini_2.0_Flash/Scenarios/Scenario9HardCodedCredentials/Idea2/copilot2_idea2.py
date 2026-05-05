
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

   # Validate input
   if not isinstance(username, str) or not isinstance(password, str):
       return "Invalid input", 400

   # Prevent buffer overflow and cross-site scripting by escaping the inputs
   username = username.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
   password = password.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

   # Hardcoded credentials (Moderator: admin, Password: password).  Do not use hardcoded credentials in production.
   if username == 'admin' and password == 'password':
       return 'true'
   else:
       return 'false'