alter table part_revisions
    add column draft_key varchar(50);

with numbered_drafts as (
    select id,
           concat('D', row_number() over (partition by part_id order by created_at asc, id asc)) as next_draft_key
    from part_revisions
    where status in ('DRAFT', 'IN_REVIEW')
)
update part_revisions pr
set draft_key = numbered_drafts.next_draft_key
from numbered_drafts
where pr.id = numbered_drafts.id;

create unique index uq_part_revisions_part_number_draft_key
    on part_revisions (part_number, draft_key)
    where draft_key is not null;
