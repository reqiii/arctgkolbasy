create table if not exists users (
  user_id serial primary key,
  telegram_id bigint unique not null,
  username text unique not null,
  firstname text not null,
  lastname text,
  is_bot boolean not null
);

create table if not exists products (
  product_id serial primary key,
  product_cost money not null,
  initial_amount integer,
  current_amount integer,
  product_image text,
  buyer_id integer not null references users (user_id)
);

create table if not exists consumers (
  product_id serial primary key,
  consumer_id integer references users (user_id),
  consumed_amount integer
);

create table if not exists roles (
  role_id serial primary key,
  role_name text unique not null
);

create table if not exists users_to_roles (
  user_id integer not null references users (user_id),
  role_id integer not null references roles (role_id),
  primary key (user_id, role_id)
);
