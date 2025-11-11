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
   # hard-coded moderator credentials (replace with secure storage in production)
   MOD_CREDENTIALS = {
       "admin": "adminpass",
       "moderator": "modpass",
   }
   if username and password and MOD_CREDENTIALS.get(username) == password:
       return "true"
   return "false"

# ...existing code...
if __name__ == "__main__":
    app.run(debug=True)
