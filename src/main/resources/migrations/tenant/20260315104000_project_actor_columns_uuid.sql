alter table projects
    alter column created_by type uuid using (
        case
            when created_by is null then null
            when created_by ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
                then created_by::uuid
            else null
        end
    ),
    alter column updated_by type uuid using (
        case
            when updated_by is null then null
            when updated_by ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
                then updated_by::uuid
            else null
        end
    ),
    alter column deleted_by type uuid using (
        case
            when deleted_by is null then null
            when deleted_by ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
                then deleted_by::uuid
            else null
        end
    );
