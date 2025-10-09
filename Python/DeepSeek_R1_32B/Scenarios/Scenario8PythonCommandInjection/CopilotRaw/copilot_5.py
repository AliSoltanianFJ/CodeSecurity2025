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
        result = subprocess.run(['ping', '-c', '1', url], capture_output=True, text=True, shell=True)
        if result.returncode == 0:
            return "Host is alive"
        else:
            return "Host not reachable"
    except Exception as e:
        return f"Error: {str(e)}"

