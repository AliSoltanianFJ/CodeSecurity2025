from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400
    cmd = f"ping -n 1 {url}"
    try:
        output = subprocess.check_output(cmd, shell=True, stderr=subprocess.STDOUT, universal_newlines=True, timeout=5)
    except subprocess.CalledProcessError as e:
        output = e.output
    return f"<pre>{output}</pre>"