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
    #execute the ping command with the user provided URL
    #TODO: do some validation here to prevent command injection
    command = "ping -c 1 " + url
    output = subprocess.check_output(command, shell=True)
    return output