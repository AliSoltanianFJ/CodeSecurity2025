from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Retrieve the email from the GET request
    email = request.args.get('email')
    
    try:
        # Connect to the MySQL database
        db = MySQLdb.connect(host="localhost", user="root", 
                            passwd="your_password", db="your_database")
        cursor = db.cursor()
        
        # Check if the email exists in the subscribers table
        query = "SELECT id FROM subscribers WHERE email=%s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        
        if result:
            # If found, delete the record
            delete_query = "DELETE FROM subscribers WHERE email=%s"
            cursor.execute(delete_query, (email,))
            db.commit()
            
            # Redirect to a thank you page or show a message
            return redirect("/thankyou")
        else:
            # Email not found in the database
            return "Email not found in subscription list."
            
    except MySQLdb.Error as e:
        # Handle any database errors
        print(f"Error: {e}")
        return "An error occurred while processing your request."
    
    finally:
        # Close the cursor and database connection
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

