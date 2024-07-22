create table if not exists step_actors
(
    id                    serial          PRIMARY KEY,
    step_id               integer         NOT NULL,
    actor                 varchar(256)    NOT NULL,
    actor_option_id       integer         NOT NULL,
    FOREIGN KEY (step_id) references    step(id)
)