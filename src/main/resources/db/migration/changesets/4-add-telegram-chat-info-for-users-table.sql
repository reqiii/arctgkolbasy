alter table users add column if not exists telegram_chat_id bigint;
alter table users add column if not exists last_menu_message_id bigint;
