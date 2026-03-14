drop index if exists uq_part_revisions_part_number_draft_key;

create unique index uq_part_revisions_initial_draft_key
    on part_revisions (part_number, draft_key)
    where draft_key is not null and base_revision_id is null;

create unique index uq_part_revisions_revision_draft_key
    on part_revisions (base_revision_id, draft_key)
    where draft_key is not null and base_revision_id is not null;
