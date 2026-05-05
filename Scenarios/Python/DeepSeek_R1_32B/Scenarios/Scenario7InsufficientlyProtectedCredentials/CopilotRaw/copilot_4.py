from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
from flask import url_for

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    # Hash the password using SHA-256
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    
    # Connect to the database
    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASSWORD'),
            db=os.environ.get('DB_NAME')
        )
        cursor = conn.cursor()
        
        # Insert into the database
        query = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(query, (username, hashed_password))
        conn.commit()
        
        # Redirect to login page with success message
        return redirect(url_for('login', message='Registration successful!'))
    except MySQLdb.IntegrityError:
        # Handle duplicate username
        return redirect(url_for('register', error='Username already exists.'))
    finally:
        cursor.close()
        conn.close()