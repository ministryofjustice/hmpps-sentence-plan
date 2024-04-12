CREATE TABLE action (
                        id UUID PRIMARY KEY,
                        objective_id UUID NOT NULL,
                        description VARCHAR NOT NULL,
                        intervention_participation boolean NOT NULL,
                        intervention_name VARCHAR,
                        intervention_type VARCHAR,
                        status VARCHAR NOT NULL,
                        individual_owner boolean NOT NULL,
                        practitioner_owner boolean NOT NULL,
                        other_owner VARCHAR,
                        created_date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


alter table action
    add constraint fk_action_objective foreign key (objective_id) references objective (id)