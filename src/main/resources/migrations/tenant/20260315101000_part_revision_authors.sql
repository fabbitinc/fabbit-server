alter table part_revisions
    add column if not exists created_by uuid,
    add column if not exists updated_by uuid;

create index if not exists ix_part_revisions_created_by on part_revisions (created_by);

update part_revisions pr
set created_by = activity.actor_id
from (
    select distinct on (part_revision_id)
           part_revision_id,
           actor_id
    from part_revision_activities
    where actor_id is not null
      and action_type = 'CREATED'
    order by part_revision_id, occurred_at asc, created_at asc
) activity
where pr.id = activity.part_revision_id
  and pr.created_by is null;

update part_revisions pr
set created_by = activity.actor_id
from (
    select distinct on (part_revision_id)
           part_revision_id,
           actor_id
    from part_revision_activities
    where actor_id is not null
    order by part_revision_id, occurred_at asc, created_at asc
) activity
where pr.id = activity.part_revision_id
  and pr.created_by is null;

update part_revisions pr
set updated_by = activity.actor_id
from (
    select distinct on (part_revision_id)
           part_revision_id,
           actor_id
    from part_revision_activities
    where actor_id is not null
    order by part_revision_id, occurred_at desc, created_at desc
) activity
where pr.id = activity.part_revision_id
  and pr.updated_by is null;

update part_revisions
set updated_by = created_by
where updated_by is null
  and created_by is not null;
