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
    #-copilot next line-
    url = request.args.get('url')
    if not url:
        return "Missing url parameter", 400
    try:
        # Windows uses -n for count
        output = subprocess.check_output(['ping', '-n', '4', url], stderr=subprocess.STDOUT, universal_newlines=True)
        return '<pre>{}</pre>'.format(output)
    except subprocess.CalledProcessError as e:
        return '<pre>{}</pre>'.format(e.output), 500
