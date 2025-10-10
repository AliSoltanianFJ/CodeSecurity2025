from flask import Flask, request, redirect, url_for, flash
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)
app.secret_key = os.urandom(24)  # Required for flashing messages

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    try:
        # Get and sanitize inputs
        username = request.form.get('username')
        password = request.form.get('password')

        if not username or not password:
            flash('Username and password are required!')
            return redirect(url_for('register_page'))

        # Input validation
        if len(username) < 4 or len(username) > 20:
            flash('Username must be between 4 and 20 characters!')
            return redirect(url_for('register_page'))
        
        if not re.match(r'^[a-zA-Z0-9_]+$', username):
            flash('Invalid username! Only letters, numbers, and underscores are allowed.')
            return redirect(url_for('register_page'))

        # Secure password hashing using bcrypt
        hashed_password = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

        # Database connection
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="secure_db")
        cursor = db.cursor()

        # Check if username already exists
        query = "SELECT username FROM users WHERE username=%s"
        cursor.execute(query, (username,))
        if cursor.fetchone():
            flash('Username already exists!')
            return redirect(url_for('register_page'))

        # Securely insert new user into database
        insert_query = """
            INSERT INTO users (username, password_hash)
            VALUES (%s, %s)
        """
        cursor.execute(insert_query, (username, hashed_password))
        db.commit()

        flash('Registration successful! Please login.')
        return redirect(url_for('login_page'))

    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        flash('An error occurred during registration. Please try again.')
        return redirect(url_for('register_page'))
    
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

def sanitize_username(username):
    # Remove any non-alphanumeric characters except underscore
    return re.sub(r'[^\w]', '', username)

def sanitize_password(password):
    # Password should contain at least one uppercase, lowercase, number, and special character
    if (re.search(r'[A-Z]', password) and
        re.search(r'[a-z]', password) and
        re.search(r'\d', password) and
        re.search(r'[^A-Za-z\d]', password)):
        return True
    return False
