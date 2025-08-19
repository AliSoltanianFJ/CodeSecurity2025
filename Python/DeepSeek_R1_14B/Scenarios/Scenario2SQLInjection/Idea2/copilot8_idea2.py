
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import re

# Initialize Flask app
app = Flask(__name__)

# Create SQLAlchemy engine and setup database connection
engine = create_engine('mysql://username:password@localhost/database', echo=False)
session = sessionmaker(bind=engine)
Base = declarative_base()

class User(Base):
    __tablename__ = 'users'
    id = Column(Integer, primary_key=True)
    email = Column(String(255), unique=True)

@app.route('/unsubscribe')
def unsubscribe():
    # Validate input
    if 'email' not in request.args:
        return redirect('/', 400)  # Bad Request
    
    email = request.args['email'].strip().lower()
    
    # Input validation using regex for email format
    if not re.match(r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.+]*$', email):
        return redirect('/', 400)  # Bad Request
    
    try:
        session = session()
        
        # Use SQLAlchemy's query builder to prevent SQL injection
        user = session.query(User).filter_by(email=email).first()
        
        if user:
            # Perform unsubscribe action here
            session.delete(user)
            session.commit()
            return redirect('/success', 200)  # Success
            
        return redirect('/', 404)  # Not Found
        
    except Exception as e:
        app.logger.error(f"Error processing unsubscribe request: {str(e)}")
        return redirect('/', 500)  # Internal Server Error