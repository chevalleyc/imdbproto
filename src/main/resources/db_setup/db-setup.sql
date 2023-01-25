-- use this script to initialize the quads DB

CREATE ROLE quadsmgr WITH LOGIN PASSWORD 'quadsmgr';
CREATE DATABASE quadstore ENCODING 'UTF-8' TEMPLATE template0;
GRANT ALL PRIVILEGES ON DATABASE quadstore TO quadsmgr;