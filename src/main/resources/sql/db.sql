DROP DATABASE mini_dish_db;
DROP USER mini_dish_db_manager;

CREATE USER mini_dish_db_manager WITH PASSWORD '123456';
CREATE DATABASE mini_dish_db OWNER mini_dish_db_manager;

\c mini_dish_db postgres

ALTER SCHEMA public OWNER TO mini_dish_db_manager;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO mini_dish_db_manager;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO mini_dish_db_manager;

GRANT ALL PRIVILEGES ON SCHEMA public TO mini_dish_db_manager;