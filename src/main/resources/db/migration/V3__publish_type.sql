do $$ begin
    create type publish_type as enum ('UNPUBLISHED', 'PUBLISHED', 'ARCHIVED');
exception
    when duplicate_object then null;
end $$;
