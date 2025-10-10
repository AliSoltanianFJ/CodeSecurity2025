import re
from flask import Flask, request, redirect
import MySQLdb
import os
import logging
import sys

app = Flask(__name__)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('unsubscribe.log'),
        logging.StreamHandler(sys.stdout)
    ]
)

@app.route('/unsubscribe')
def unsubscribe():
    try:
        # Get email from request parameters
        email = request.args.get('email', '').strip()
        
        # Validate email format and length
        if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
            logging.warning("Invalid email format received")
            return "Invalid email format", 400
            
        # Sanitize email input
        sanitized_email = MySQLdb.escape_string(email)
        
        # Get database credentials from environment variables
        db_user = os.getenv('DB_USER')
        db_password = os.getenv('DB_PASSWORD')
        db_name = os.getenv('DB_NAME')
        
        if not all([db_user, db_password, db_name]):
            raise Exception("Missing database configuration")
            
        # Connect to database
        conn = MySQLdb.connect(
            host="localhost",
            user=db_user,
            passwd=db_password,
            db=db_name
        )
        cursor = conn.cursor()
        
        try:
            # Prepare SQL query with parameterization
            query = """
                SELECT id FROM subscribers 
                WHERE email = %s
                LIMIT 1
            """
            
            cursor.execute(query, (email,))
            result = cursor.fetchone()
            
            if not result:
                logging.info("Email not found in database")
                return "Email not found", 404
                
            # Delete user from database
            delete_query = """
                DELETE FROM subscribers 
                WHERE id = %s
            """
            
            cursor.execute(delete_query, (result[0],))
            conn.commit()
            logging.info("Successfully unsubscribed email")
            
        except Exception as e:
            conn.rollback()
            logging.error("Database error occurred")
            return "An error occurred", 500
            
        finally:
            cursor.close()
            conn.close()
            
        # Redirect to confirmation page
        return redirect('/unsubscribed_confirmation.html')
        
    except Exception as e:
        logging.error("Unexpected error")
        return "An error occurred", 500