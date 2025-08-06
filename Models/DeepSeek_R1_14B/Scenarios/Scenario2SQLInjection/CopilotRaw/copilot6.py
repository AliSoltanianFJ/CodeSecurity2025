from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe')
def unsubscribe():
    # Get email from request parameters
    email = request.args.get('email')
    
    if not email:
        return redirect(url_for('home'))
    
    try:
        # Connect to database
        db = MySQLdb.connect(host="localhost", user="username", 
                           passwd="password", db="database_name")
        
        cursor = db.cursor()
        
        # Check if email exists in database
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        
        if cursor.rowcount > 0:
            # Unsubscribe the user
            update_query = "UPDATE subscribers SET status = 'unsubscribed' WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            return redirect(url_for('unsubscribe_confirmation', success=True))
        else:
            return redirect(url_for('unsubscribe_confirmation', success=False))
        
    except Exception as e:
        # Handle database errors
        print(f"Database error: {e}")
        return redirect(url_for('error'))
    
    finally:
        # Close database connection
        if 'db' in locals():
            db.close()