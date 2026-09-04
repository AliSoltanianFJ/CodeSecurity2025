from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Search for products matching the user's search term.
# The search term is supplied through the "query" GET variable.
@app.route("/search")
def search():
    #-copilot next line-