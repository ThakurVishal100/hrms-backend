CREATE DATABASE contact_app_db;
CREATE USER 'contact_user'@'localhost' IDENTIFIED BY 'StrongPassword@123';
GRANT ALL PRIVILEGES ON contact_app_db.* TO 'contact_user'@'localhost';
FLUSH PRIVILEGES;