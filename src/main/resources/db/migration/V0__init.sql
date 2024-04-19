create table if not exists ref_data
(
    reference_data jsonb null
);

insert into ref_data(reference_data) values (
   '{
     "AreasOfNeed": [
       {
         "id": 1,
         "Name": "Accommodation",
         "active": true,
         "Goals": [
           {
             "id": 1,
             "Name": "Improve relationship with neighbours",
             "Active": true,
             "Steps": [
               {"id": 1, "Name": "Improve relationship with neighbours - Step A", "Active": true},
               {"id": 2, "Name": "Improve relationship with neighbours - Step B", "Active": true}
             ]
           },
           {
             "id": 2,
             "Name": "Find suitable accommodation",
             "Active": true,
             "Steps": [
               {"id": 7, "Name": "Find suitable accommodation - Step A", "Active": true},
               {"id": 8, "Name": "Find suitable accommodation - Step B", "Active": true}
             ]
           }
         ]
       },
       {
         "id": 2,
         "Name": "Drugs",
         "active": true,
         "Goals": [
           {
             "id": 8,
             "Name": "Drugs Goal 1",
             "Active": true,
             "Steps": [
               {"id": 60, "Name": "Drugs Goal 1 - Step A", "Active": true},
               {"id": 61, "Name": "Drugs Goal 1 - Step B", "Active": true}
             ]
           },
           {
             "id": 9,
             "Name": "Drugs Goal 2",
             "Active": true,
             "Steps": [
               {"id": 65, "Name": "Drugs Goal 2 - Step A", "Active": true},
               {"id": 66, "Name": "Drugs Goal 2 - Step B", "Active": true}
             ]
           }
         ]
       },
       {
         "id": 3,
         "Name": "Health and Wellbeing",
         "active": true,
         "Goals": [
           {
             "id": 17,
             "Name": "Health and Wellbeing Goal 1",
             "Active": true,
             "Steps": [
               {"id": 117, "Name": "Health and Wellbeing Goal 1 - Step A", "Active": true}
             ]
           }
         ]
       }
     ]
   }'
);