CREATE TYPE dish_type AS ENUM ('STARTER', 'MAIN', 'DESSERT');
CREATE TYPE ingredient_category AS ENUM ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');
CREATE TYPE unit AS ENUM ('PCS', 'KG', 'L');
CREATE TYPE movement_type AS ENUM ('IN', 'OUT');

CREATE TABLE dish (
                      id            SERIAL PRIMARY KEY,
                      name          VARCHAR(255) NOT NULL,
                      dish_type     dish_type,
                      selling_price NUMERIC(10, 2)
);

CREATE TABLE ingredient (
                            id            SERIAL PRIMARY KEY,
                            name          VARCHAR(255) NOT NULL,
                            price         NUMERIC(10, 2),
                            category      ingredient_category,
                            initial_stock NUMERIC(10, 2) DEFAULT 0
);

CREATE TABLE dish_ingredient (
                                 id                SERIAL PRIMARY KEY,
                                 id_dish           INT REFERENCES dish(id),
                                 id_ingredient     INT REFERENCES ingredient(id),
                                 required_quantity NUMERIC(10, 2),
                                 unit              unit
);

CREATE TABLE "order" (
                         id                SERIAL PRIMARY KEY,
                         reference         VARCHAR(255) UNIQUE,
                         creation_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dish_order (
                            id       SERIAL PRIMARY KEY,
                            id_order INT REFERENCES "order"(id),
                            id_dish  INT REFERENCES dish(id),
                            quantity INT NOT NULL
);

CREATE TABLE stock_movement (
                                id                SERIAL PRIMARY KEY,
                                id_ingredient     INT REFERENCES ingredient(id),
                                quantity          NUMERIC(10, 2),
                                unit              unit,
                                type              movement_type,
                                creation_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);