from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
# ...existing code...
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    # This is a placeholder for secure password handling.
    # In production, use a strong password hashing library such as 'bcrypt' or 'argon2'.
    # Do not use SHA256 or store plain hashes.
    return "Registration endpoint reached. Implement secure password handling.", 501
# ...existing code...