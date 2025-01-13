do
$$
    begin
        alter type goal_note_type add value 'READDED';
    exception
        when duplicate_object then null;
    end
$$;