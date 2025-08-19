from flask import Flask
from flask import request, redirect, url_for, flash
import hashlib
import os
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

app = Flask(__name__)

# Assuming you have a database setup with SQLAlchemy
engine = create_engine('mysql://username:password@localhost/database_name')
Session = sessionmaker(bind=engine)

@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    if not request.form:
        return redirect(url_for('register_page'))
    
    username = request.form.get('username', '').strip()
    email = request.form.get('email', '').strip()
    password = request.form.get('password', '')
    
    # Password validation
    if len(password) < 8:
        flash('Password must be at least 8 characters long.')
        return redirect(url_for('register_page'))
    
    if not any(c.isupper() for c in password):
        flash('Password must contain at least one uppercase letter.')
        return redirect(url_for('register_page'))
    
    if not any(c.isdigit() for c in password):
        flash('Password must contain at least one number.')
        return redirect(url_for('register_page'))
    
    if not any(c in '!@#$%^&*()' for c in password):
        flash('Password must contain at least one special character.')
        return redirect(url_for('register_page'))

    # Generate a secure salt
    salt = os.urandom(16).hex()
    
    # Create the hashed password using PBKDF2
    iterations = 100000  # Adjust based on performance needs
    key_length = 32
    hashed_password = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations, key_length)
    
    try:
        session = Session()
        
        # Check for existing user
        user_exists = session.execute(
            text("SELECT id FROM users WHERE username = :username OR email = :email"),
            {'username': username, 'email': email}
        ).fetchone()
        
        if user_exists:
            flash('Username or email already exists.')
            return redirect(url_for('register_page'))
        
        # Create a new user
        new_user = {
            'username': username,
            'email': email,
            'password_hash': hashed_password.hex(),
            'salt': salt,
            'created_at': datetime.datetime.now()
        }
        
        session.execute(
            text("INSERT INTO users (username, email, password_hash, salt) VALUES (:username, :email, :password_hash, :salt)"),
            new_user
        )
        session.commit()
        
        flash('Registration successful! Please log in.')
        return redirect(url_for('login_page'))
        
    except Exception as e:
        # Log the error for debugging purposes
        print(f"Registration failed: {str(e)}")
        flash('An error occurred during registration. Please try again later.')
        return redirect(url_for('register_page'))
    
    finally:
        session.close()