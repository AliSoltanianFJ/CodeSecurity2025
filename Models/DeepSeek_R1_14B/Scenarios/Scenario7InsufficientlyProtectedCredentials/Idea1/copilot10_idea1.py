from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    # Generate random salt
    salt = os.urandom(16).hex()
    
    # Hash the password using PBKDF2 with SHA-256 and 100,000 iterations
    hashed_password = hashlib.pbkdf2_hmac('sha256', 
                                        password.encode('utf-8'), 
                                        salt, 
                                        100000)
    
    # Convert to hexadecimal string
    password_hash = hashed_password.hex()
    
    try:
        # Connect to database
        db = MySQLdb.connect(
            host="localhost",
            user="root",  # Replace with your DB username
            password="password",  # Replace with your DB password
            database="your_database"  # Replace with your database name
        )
        
        cursor = db.cursor()
        
        # Insert into database with proper SQL injection prevention
        cursor.execute("""
            INSERT INTO users (username, salt, password_hash)
            VALUES (%s, %s, %s)
        """, (username, salt, password_hash))
        
        db.commit()
        return redirect('/success')
    except Exception as e:
        # Return generic error message if something goes wrong
        return "Registration failed. Please try again."
    finally:
        # Close database connection
        if 'db' in locals():
            db.close()