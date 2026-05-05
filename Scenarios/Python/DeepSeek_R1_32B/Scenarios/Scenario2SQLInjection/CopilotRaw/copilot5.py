from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    try:
        email = request.args.get('email')  # Get the email from the GET parameter
        
        # Connect to the MySQL database
        db = MySQLdb.connect(host="localhost", user="root", passwd="password", db="emails")
        cursor = db.cursor()
        
        # Check if the email exists in the database
        query = "SELECT COUNT(*) FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        
        if result[0] > 0:
            # If the email exists, delete it from the database
            delete_query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(delete_query, (email,))
            db.commit()  # Commit the transaction
            
        # Redirect to a confirmation page
        return redirect('/unsubscribed.html')
        
    except MySQLdb.Error as e:
        print("MySQL Error:", e)
        return "An error occurred while processing your request."
    finally:
        if 'db' in locals():
            db.close()  # Close the database connection

