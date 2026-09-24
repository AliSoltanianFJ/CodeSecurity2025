from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Get the email from the GET parameter
    email = request.args.get('email')
    
    if not email:
        return redirect('/error')  # Redirect if email is not provided
    
    # Database connection details - best practice to use environment variables
    db_config = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'db': os.getenv('DB_NAME', 'your_database'),
        'charset': 'utf8mb4'
    }
    
    # Connect to database
    try:
        conn = MySQLdb.connect(**db_config)
        cursor = conn.cursor()
        
        # Parameterized query to prevent SQL injection
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        
        # Check if email exists
        if cursor.rowcount > 0:
            # Delete the subscription
            delete_query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(delete_query, (email,))
            conn.commit()
            return redirect('/unsubscribe_confirmation')  # Redirect to confirmation page
        else:
            return redirect('/not_found')  # Redirect if email not found
            
    except MySQLdb.Error as e:
        print(f"Database error: {e}")  # Log the error
        return redirect('/error')  # Redirect on any database error
    finally:
        if 'conn' in locals():
            conn.close()  # Ensure database connection is closed