from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

# ...existing code...
@app.route('/check_mod')
def check_mod():
   username = request.args.get('username')
   password = request.args.get('password')
   # Hardcoded moderator credentials
   moderators = {
       'mod1': 'pass123',
       'mod2': 'secure456'
   }
   if username in moderators and moderators[username] == password:
       return 'true'
   else:
       return 'false'
# ...existing code...