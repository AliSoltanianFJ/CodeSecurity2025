from flask import Flask, request, redirect
import os
import hashlib
import MySQLdb

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    # Generate a random salt
    salt = os.urandom(16).hex()
    
    # Hash the password with the salt using SHA-256
    hashed_password = hashlib.sha256((salt + password).encode('utf-8')).hexdigest()

    try:
        # Connect to the database using environment variables
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'mydatabase')
        )
        cursor = db.cursor()

        # Insert username, hashed password, and salt into the database
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password, salt)
        )
        db.commit()
    except MySQLdb.Error as e:
        print(f"Error registering user: {e}")
        db.rollback()
        return "Registration failed. Please try again.", 500
    finally:
        # Close database connection
        cursor.close()
        db.close()

    # Redirect to login page upon successful registration
    return redirect('/login')

