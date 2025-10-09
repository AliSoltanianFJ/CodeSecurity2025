from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    try:
        with open('config.yaml', 'r') as f:
            config = yaml.safe_load(f)
            return username in config.get('moderators', [])
    except Exception as e:
        return False

