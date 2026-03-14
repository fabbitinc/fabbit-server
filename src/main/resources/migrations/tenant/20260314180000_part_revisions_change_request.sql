alter table part_revisions
    add column if not exists change_request_id uuid;

create index if not exists ix_part_revisions_change_request_id
    on part_revisions (change_request_id);

alter table part_revisions
    add constraint fk_part_revisions_change_request
        foreign key (change_request_id)
            references change_requests (id);
