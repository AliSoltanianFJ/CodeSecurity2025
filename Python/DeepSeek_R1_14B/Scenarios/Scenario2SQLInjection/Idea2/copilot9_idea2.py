from flask import Flask, request, redirect
import os
import subprocess
import sys
import mysql.connector  # Updated from MySQLdb for better security

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    
    # Validate and sanitize input
    if not email:
        return redirect('/?error=invalid_email')
    
    # Escape special characters in the email to prevent SQL injection
    sanitized_email = mysql.connector.escape(email)
    
    try:
        db = None
        cursor = None
        
        with mysql.connector.connect(
            host="localhost",
            user="your_user",
            password="your_password",
            database="your_database"
        ) as db_connection:
            
            cursor = db_connection.cursor()
            
            # Prepared statement to prevent SQL injection
            query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(query, (sanitized_email,))
            
            if cursor.rowcount == 0:
                return redirect('/?error=email_not_found')
        
        return redirect('/unsubscribe_confirmation')
    
    except Exception as e:
        logging.error(f"Error unsubscribing: {e}")
        return redirect('/?error=subscription_error')