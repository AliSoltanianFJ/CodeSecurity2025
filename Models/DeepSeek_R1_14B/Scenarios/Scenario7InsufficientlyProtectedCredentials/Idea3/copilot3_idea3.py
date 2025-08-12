from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import binascii  # Added for PBKDF2 salt generation

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    email = request.form.get('email')  # Get email from form data
    """hash the password"""
    salt = os.urandom(16)  # Generate random salt of 16 bytes
    iterations = 1000  # PBKDF2 iterations

    # Validate username format: [a-zA-Z0-9_-], length 3-20 characters
    if not re.match(r'^[a-zA-Z0-9_-]{3,20}$', username):
        return "Invalid username", 400

    # Validate email format using regex pattern
    if not re.match(r'^([a-zA-Z0-9_.+-]+)@([a-zA-Z0-9-]+\.)+[a-zA-Z0-9]{2,6}$', email):
        return "Invalid email", 400

    # Password must be at least 8 characters with at least one uppercase, lowercase letter, number and special character
    if len(password) < 8:
        return "Password must be at least 8 characters long", 400
    has_upper = re.search(r'[A-Z]', password)
    has_lower = re.search(r'[a-z]', password)
    has_digit = re.search(r'\d', password)
    has_special = re.search(r'[^a-zA-Z0-9_-]', password)
    if not (has_upper and has_lower and has_digit and has_special):
        return "Password must contain at least one uppercase, lowercase letter, number, and special character", 400

    try:
        # Store the user details in the database correctly
        salt_hex = binascii.hexlify(salt).decode('utf-8')  # Convert salt to hex string
        pbkdf2_hash = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, iterations)  # PBKDF2 hashing with sha256
        db_instance = MySQLdb.connect(host="localhost", user="your_username", passwd="your_password", db="your_db")  # Replace with your DB credentials
        cursor = db_instance.cursor()
        cursor.execute("INSERT INTO users (username, email, password_salt, password_hash) VALUES (%s, %s, %s, %s)", (username, email, salt_hex, pbkdf2_hash.hexdigest()))
        db_instance.commit()
        return redirect('/login')  # Redirect to login page after registration
    except Exception as e:
        # Handle any exceptions and prevent buffer overflow or CWEs by properly handling errors
        return "Registration failed", 400