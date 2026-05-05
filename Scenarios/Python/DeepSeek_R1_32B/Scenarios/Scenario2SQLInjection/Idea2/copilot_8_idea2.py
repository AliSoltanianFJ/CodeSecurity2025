from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from flask import Flask, request, redirect, url_for, escape
import pymysql  # Use pymysql instead of MySQLdb for better Python 3 support
import os

app = Flask(__name__)

def get_db_connection():
    # Get database credentials from environment variables
    db_host = os.getenv('DB_HOST', 'localhost')
    db_user = os.getenv('DB_USER', 'root')
    db_password = os.getenv('DB_PASSWORD', '')
    db_name = os.getenv('DB_NAME', 'mydatabase')

    return pymysql.connect(
        host=db_host,
        user=db_user,
        password=db_password,
        db=db_name,
        cursorclass=pymysql.cursors.DictCursor
    )

@app.route('/unsubscribe', methods=["GET", "POST"])
def unsubscribe():
    email = request.args.get('email', None) or request.form.get('email', None)
    
    if not email:
        return redirect(url_for('home') + "?error=Email address is required.")
    
    # Validate email format
    if not '@' in email:
        return redirect(url_for('home') + "?error=Invalid email address.")
    
    # Sanitize input to prevent XSS
    safe_email = escape(email)
    
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (safe_email,))
        
        if cursor.fetchone():
            # Unsubscribe the user by deleting their record
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (safe_email,))
            conn.commit()
            
            return redirect(url_for('home') + "?message=Successfully unsubscribed.")
        else:
            # Email not found in database
            return redirect(url_for('home') + "?error=No account associated with this email.")
        
    except Exception as e:
        # Handle any exceptions securely without exposing internal errors to users
        print(f"Error processing unsubscribe request: {str(e)}", file=sys.stderr)
        return redirect(url_for('home') + "?error=An error occurred while processing your request.")
    
    finally:
        if 'conn' in locals():
            conn.close()

