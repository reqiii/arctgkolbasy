create table if not exists transactions (
    transaction_id bigserial primary key,
    from_id integer references users (user_id),
    to_id integer references users (user_id),
    amount money not null
);

create table if not exists products_to_transactions(
    product_id integer not null references products (product_id),
    transaction_id integer not null references transactions (transaction_id),
    primary key (product_id, transaction_id)
);
