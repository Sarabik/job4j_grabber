CREATE TABLE if NOT EXISTS post (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    text TEXT,
    link VARCHAR(255) UNIQUE,
    created TIMESTAMP);