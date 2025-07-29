from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    cursor.execute("SELECT password FROM users WHERE username = '" + username + "'")
    data = cursor.fetchone()
    if data is None:
        return "Invalid username or password"
    if (data[0] == password):
        return "Login success"
    else:
        return "Invalid username or password"