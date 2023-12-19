drop table if exists item;
drop table if exists item_sku;
drop table if exists remain;
drop table if exists catalogue;

create table item
(
    item_id      bigint,
    itemurl      varchar,
    type         varchar,
    catalogue_id bigint,
    name         varchar,
    i            varchar,
    brand_id     bigint,
    brand        varchar,
    catalogue    varchar,
    description  varchar
);

create table item_sku
(
    item_id bigint,
    sku     varchar
);

create table remain
(
    item_id   bigint,
    region_id bigint,
    price     bigint
);

create table catalogue
(
    catalogue_id bigint,
    realcatname  varchar,
    image        varchar,
    parent_id    bigint,
    name         varchar
);

INSERT INTO item (item_id, itemurl, type, catalogue_id, name, i, brand_id, brand, catalogue, description)
VALUES
    (1, 'example.com/item1', 'electronics', 1, 'Smartphone X', 'i1', 1, 'Ant', 'Electronics', 'High-end smartphone with X'),
    (2, 'example.com/item2', 'clothing', 2, 'T-Shirt Y', 'i2', 2, 'Brand', 'Fashion', 'Comfortable cotton T-shirt Y'),
    (3, 'example.com/item3', 'electronics', 1, 'Laptop Z', 'i3', 1, 'Ant', 'Electronics', 'Powerful laptop with Z'),
    (4, 'example.com/item4', 'furniture', 3, 'Sofa A', 'i4', 3, 'Cool', 'Home', 'Stylish sofa with A'),
    (5, 'example.com/item5', 'clothing', 2, 'Jeans B', 'i5', 2, 'Brand', 'Fashion', 'Trendy denim jeans B'),
    (6, 'example.com/item6', 'electronics', 1, 'Headphones C', 'i6', 4, 'Dirty', 'Electronics', 'Noise-canceling headphones C'),
    (7, 'example.com/item7', 'clothing', 2, 'Shoes', 'i7', 2, 'Cool', 'Fashion', 'Comfortable stylish shoes'),
    (8, 'example.com/item8', 'electronics', 1, 'Macbook Air 2022', 'i8', 4, 'Apple', 'Electronics', 'Super mega cool laptop');

INSERT INTO item_sku (item_id, sku)
VALUES
    (1, '12345'),
    (2, '23456'),
    (3, '34567'),
    (4, '45678'),
    (5, '56789'),
    (6, '67890'),
    (7, '78901'),
    (8, '89012');

INSERT INTO remain (item_id, region_id, price)
VALUES
    (1, 1, 10000),
    (2, 1, 500),
    (3, 2, 15000),
    (4, 2, 20000),
    (5, 3, 800),
    (6, 3, 3000),
    (7, 1, 12000),
    (8, 1, 120000);

INSERT INTO catalogue (catalogue_id, realcatname, image, parent_id, name)
VALUES
    (1, 'Electronics', 'example.com/electronics', 1, 'Electronics'),
    (2, 'Fashion', 'example.com/fashion', 2, 'Fashion'),
    (3, 'Home', 'example.com/home', 3, 'Home');
