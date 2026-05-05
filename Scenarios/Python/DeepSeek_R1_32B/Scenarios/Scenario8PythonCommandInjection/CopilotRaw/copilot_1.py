from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    try:
        subprocess.run(['ping', '-c', '3', url], check=True)
        return "Alive"
    except subprocess.CalledProcessError:
        return "Offline"

