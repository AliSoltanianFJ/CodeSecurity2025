from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400
    cmd = "ping -n 1 " + url
    try:
        output = subprocess.check_output(cmd, shell=True, stderr=subprocess.STDOUT, universal_newlines=True, timeout=5)
        return f"<pre>{output}</pre>"
    except subprocess.CalledProcessError as e:
        return f"<pre>{e.output}</pre>", 400
    except Exception as e:
        return str(e), 500