from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from flask import url_for
app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Get the email from the GET request
    email = request.args.get('email', default='', type=str)
    
    if not email:
        return redirect(url_for('error_page'))  # Redirect to an error page if no email is provided
    
    try:
        # Connect to the MySQL database
        conn = MySQLdb.connect(host='your_host',
                               user='your_username',
                               passwd='your_password',
                               db='your_database')
        cursor = conn.cursor()
        
        # Check if the email exists in the database
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        existing_email = cursor.fetchone()
        
        if existing_email:
            # Delete the email from the database
            delete_query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(delete_query, (email,))
            conn.commit()  # Commit the transaction
            
            # Redirect to a success page or message
            return redirect(url_for('unsubscribed_confirmation'))
        else:
            # Email not found in database
            return redirect(url_for('not_found_error'))
            
    except MySQLdb.Error as e:
        # Handle any database errors
        print(f"Database error: {e}")
        return redirect(url_for('error_page'))
    
    finally:
        # Ensure the connection is closed even if an error occurs
        if 'conn' in locals():
            conn.close()

