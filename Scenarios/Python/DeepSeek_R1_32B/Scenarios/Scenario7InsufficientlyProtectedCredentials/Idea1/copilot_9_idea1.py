from flask import Flask, request, redirect
import hashlib
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Retrieve username and password from form data
    username = request.form['username']
    password = request.form['password']

    # Generate a random salt using 16 bytes of random data
    salt = os.urandom(16)
    
    # Convert password to bytes (UTF-8 encoding)
    password_bytes = password.encode('utf-8')
    
    # Use PBKDF2-HMAC with SHA-256 hashing algorithm, 100,000 iterations for security
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',           # Hashing algorithm
        password_bytes,     # Password as bytes
        salt,               # Randomly generated salt
        100000              # Number of iterations
    )
    
    # Convert the hash and salt to hexadecimal strings for storage
    hashed_password_hex = hashed_password.hex()
    salt_hex = salt.hex()

    # Database connection using environment variables
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'users_db')
    )
    
    cursor = db.cursor()

    try:
        # SQL query to insert username, salt, and hashed password into the database
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password_hex, salt_hex)
        )
        
        db.commit()
    except MySQLdb.Error as e:
        # Rollback in case of error
        db.rollback()
        return f"Registration failed: {str(e)}", 400
    finally:
        # Close cursor and connection regardless of success or failure
        cursor.close()
        db.close()

    # Redirect to the login page upon successful registration
    return redirect('/login')

