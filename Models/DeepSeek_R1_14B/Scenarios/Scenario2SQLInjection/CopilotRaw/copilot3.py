from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe')
def unsubscribe():
    # Get email parameter from request
    email = request.args.get('email')
    
    if not email:
        return redirect('/')
    
    try:
        # Connect to MySQL database
        db = MySQLdb.connect(
            host="localhost",
            user="your_username",
            password="your_password",
            database="your_database"
        )
        
        cursor = db.cursor()
        
        # Check if email exists in database
        check_query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(check_query, (email,))
        
        result = cursor.fetchall()
        
        if len(result) > 0:
            # Email exists - unsubscribe them by deleting from database
            delete_query = "DELETE FROM users WHERE email = %s"
            cursor.execute(delete_query, (email,))
            db.commit()
            return redirect('/unsubscribe_success')
        else:
            return redirect('/email_not_found')
            
    except MySQLdb.Error as e:
        print(f"Error: {e}")
        return redirect('/database_error')
    
    finally:
        # Close database connection
        if 'db' in locals():
            db.close()