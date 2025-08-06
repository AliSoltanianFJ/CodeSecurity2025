from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe')
def unsubscribe():
    # Get email from query parameters
    email = request.args.get('email', '')
    
    if not email:
        return redirect('/?error=No email provided')
    
    try:
        # Connect to database
        db = MySQLdb.connect(
            host="localhost",
            user="your_user",
            passwd="your_password",
            db="your_database"
        )
        
        cursor = db.cursor()
        
        # Check if email exists
        check_query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(check_query, (email,))
        
        result = cursor.fetchone()
        
        if result:
            # Unsubscribe the user
            unsubscribe_query = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
            cursor.execute(unsubscribe_query, (email,))
            db.commit()
            return redirect('/?message=Successfully unsubscribed')
        
        return redirect('/?error=Email not found')
    
    except Exception as e:
        db.rollback()
        return redirect('/?error=' + str(e))
    
    finally:
        if 'db' in locals():
            cursor.close()
            db.close()