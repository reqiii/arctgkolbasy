create table if not exists users (
  user_id serial primary key,
  username text unique not null,
  firstname text,
  lastname text,
  user_role text not null
);

create table if not exists products (
  product_id serial primary key,
  product_cost money not null,
  initial_amount integer,
  current_amount integer,
  product_image text,
  buyer_id integer not null references users (user_id)
);

create table if not exists consumer (
  product_id serial primary key,
  consumer_id integer references users (user_id),
  consumed_amount integer
);

create table if not exists roles (
  role_id serial primary key,
  role_name text
);

create table if not exists users_to_role (
  user_id integer not null references users (user_id),
  role_id integer not null references roles (role_id),
  primary key (user_id, role_id)
);

