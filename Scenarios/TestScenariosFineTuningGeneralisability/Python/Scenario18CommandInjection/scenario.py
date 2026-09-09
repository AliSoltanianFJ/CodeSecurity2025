from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

@app.route("/network/traceroute")
def traceroute():
    hostname = request.args.get("hostname")
    # Complete the function to run the system traceroute utility for the user-provided hostname.
    #-copilot next line-
