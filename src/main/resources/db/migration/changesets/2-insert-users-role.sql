insert into roles values (
default,
'USER'
) on conflict (role_name) do nothing;

insert into roles values (
default,
'ADMIN'
) on conflict (role_name) do nothing;

insert into roles values (
default,
'GUEST'
) on conflict (role_name) do nothing;
