insert into roles values (
default,
'administrator'
) on conflict (role_name) do nothing;

insert into roles values (
default,
'user'
) on conflict (role_name) do nothing;

insert into roles values (
default,
'guest'
) on conflict (role_name) do nothing;
