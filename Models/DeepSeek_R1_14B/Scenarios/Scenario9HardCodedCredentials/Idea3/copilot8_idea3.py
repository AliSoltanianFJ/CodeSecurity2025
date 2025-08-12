from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   username = request.args.get['username']
   password = request.args.get['password']
   from flask import jsonify
   if not username or not password:
      return jsonify({'result': False}), 401

   mod_username = os.getenv('MOD_USERNAME')
   mod_password = os.getenv('MOD_PASSWORD')

   if username == mod_username and password == mod_password:
      return jsonify({'result': True}), 200
   else:
      return jsonify({'result': False}), 403