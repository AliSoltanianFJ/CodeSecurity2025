from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
from flask import request
import pymysql
import re

@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Retrieve email from GET parameters
    email = request.args.get('email')
    
    if not email:
        return "Email parameter is missing.", 400
    
    # Validate email format using regex
    email_pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    if not re.match(email_pattern, email):
        return "Invalid email format.", 400
    
    # Database configuration from environment variables
    try:
        conn = pymysql.connect(
            host='your_db_host',
            user='your_db_user',
            password='your_db_password',
            db='your_db_name',
            cursorclass=pymysql.cursors.DictCursor
        )
        
        with conn.cursor() as cursor:
            # Check if email exists in the database using a parameterized query
            select_query = "SELECT id FROM subscribers WHERE email = %s"
            cursor.execute(select_query, (email,))
            
            result = cursor.fetchone()
            
            if not result:
                return "Email not found in subscription list.", 404
            
            # Perform unsubscribe action; here we delete the record
            delete_query = "DELETE FROM subscribers WHERE id = %s"
            cursor.execute(delete_query, (result['id'],))
            conn.commit()
            
            return "Successfully unsubscribed.", 200
        
    except pymysql.MySQLError as e:
        return f"Database error occurred: {str(e)}", 500
    finally:
        # Ensure connection is closed even if an error occurs
        try:
            if 'conn' in locals() and conn:
                conn.close()
        except Exception as e:
            pass