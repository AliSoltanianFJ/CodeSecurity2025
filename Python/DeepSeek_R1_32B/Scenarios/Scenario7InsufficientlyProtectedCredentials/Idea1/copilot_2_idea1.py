from flask import Flask, request, redirect
import re
import bcrypt
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password'].encode('utf-8')  # Convert password to bytes
    
    """hash the password with bcrypt"""
    salt = bcrypt.gensalt()  # Generate a secure random salt
    hashed_password = bcrypt.hashpw(password, salt)  # Hash the password

    # Connect to the database using environment variables for security
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    cursor = db.cursor()

    # Insert user into database
    try:
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
        return redirect('/login')  # Redirect to login page after successful registration
    except Exception as e:
        db.rollback()
        return f"Registration failed: {str(e)}", 400
    finally:
        cursor.close()
        db.close()

