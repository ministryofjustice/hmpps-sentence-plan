create table if not exists ref_data
(
    id                  serial          PRIMARY KEY,
    reference_data      jsonb           not null
);

insert into ref_data(reference_data) values (
   '{
       "AreasOfNeed":[
          {
             "id":1,
             "Name":"Accommodation",
             "active":true,
             "Goals":[
                {
                   "id":1,
                   "Name":"Improve relationship with neighbours",
                   "Active":true,
                   "Steps":[
                      {
                         "id":1,
                         "Name":"Improve relationship with neighbours - Step A",
                         "Active":true
                      },
                      {
                         "id":2,
                         "Name":"Improve relationship with neighbours - Step B",
                         "Active":true
                      },
                      {
                         "id":3,
                         "Name":"Improve relationship with neighbours - Step C",
                         "Active":true
                      },
                      {
                         "id":4,
                         "Name":"Improve relationship with neighbours - Step D",
                         "Active":true
                      },
                      {
                         "id":5,
                         "Name":"Improve relationship with neighbours - Step E",
                         "Active":true
                      },
                      {
                         "id":6,
                         "Name":"Improve relationship with neighbours - Step F",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":2,
                   "Name":"Find suitable accommodation",
                   "Active":true,
                   "Steps":[
                      {
                         "id":7,
                         "Name":"Find suitable accommodation - Step A",
                         "Active":true
                      },
                      {
                         "id":8,
                         "Name":"Find suitable accommodation - Step B",
                         "Active":true
                      },
                      {
                         "id":9,
                         "Name":"Find suitable accommodation - Step C",
                         "Active":true
                      },
                      {
                         "id":10,
                         "Name":"Find suitable accommodation - Step D",
                         "Active":true
                      },
                      {
                         "id":11,
                         "Name":"Find suitable accommodation - Step E",
                         "Active":true
                      },
                      {
                         "id":12,
                         "Name":"Find suitable accommodation - Step F",
                         "Active":true
                      },
                      {
                         "id":13,
                         "Name":"Find suitable accommodation - Step G",
                         "Active":true
                      },
                      {
                         "id":14,
                         "Name":"Find suitable accommodation - Step H",
                         "Active":true
                      },
                      {
                         "id":15,
                         "Name":"Find suitable accommodation - Step J",
                         "Active":true
                      },
                      {
                         "id":16,
                         "Name":"Find suitable accommodation - Step K",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":3,
                   "Name":"Reduce risk of eviction",
                   "Active":true,
                   "Steps":[
                      {
                         "id":17,
                         "Name":"Reduce risk of eviction - Step A",
                         "Active":true
                      },
                      {
                         "id":18,
                         "Name":"Reduce risk of eviction - Step B",
                         "Active":true
                      },
                      {
                         "id":19,
                         "Name":"Reduce risk of eviction - Step C",
                         "Active":true
                      },
                      {
                         "id":20,
                         "Name":"Reduce risk of eviction - Step D",
                         "Active":true
                      },
                      {
                         "id":21,
                         "Name":"Reduce risk of eviction - Step E",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":4,
                   "Name":"Follow rules of their accommodation provider and stay in current accommodation for the length of their sentence",
                   "Active":true,
                   "Steps":[
                      {
                         "id":22,
                         "Name":"Follow rules of accommodation provider - Step A",
                         "Active":true
                      },
                      {
                         "id":23,
                         "Name":"Follow rules of accommodation provider - Step B",
                         "Active":true
                      },
                      {
                         "id":24,
                         "Name":"Follow rules of accommodation provider - Step C",
                         "Active":true
                      },
                      {
                         "id":25,
                         "Name":"Follow rules of accommodation provider - Step D",
                         "Active":true
                      },
                      {
                         "id":26,
                         "Name":"Follow rules of accommodation provider - Step E",
                         "Active":true
                      },
                      {
                         "id":27,
                         "Name":"Follow rules of accommodation provider - Step F",
                         "Active":true
                      },
                      {
                         "id":28,
                         "Name":"Follow rules of accommodation provider - Step G",
                         "Active":true
                      },
                      {
                         "id":29,
                         "Name":"Follow rules of accommodation provider - Step H",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":5,
                   "Name":"Have housing needs assessed by the housing advice service",
                   "Active":true,
                   "Steps":[
                      {
                         "id":30,
                         "Name":"Have housing needs assessed - Step A",
                         "Active":true
                      },
                      {
                         "id":31,
                         "Name":"Have housing needs assessed - Step B",
                         "Active":true
                      },
                      {
                         "id":32,
                         "Name":"Have housing needs assessed - Step C",
                         "Active":true
                      },
                      {
                         "id":33,
                         "Name":"Have housing needs assessed - Step D",
                         "Active":true
                      },
                      {
                         "id":34,
                         "Name":"Have housing needs assessed - Step E",
                         "Active":true
                      },
                      {
                         "id":35,
                         "Name":"Have housing needs assessed - Step F",
                         "Active":true
                      },
                      {
                         "id":36,
                         "Name":"Have housing needs assessed - Step G",
                         "Active":true
                      },
                      {
                         "id":37,
                         "Name":"Have housing needs assessed - Step H",
                         "Active":true
                      },
                      {
                         "id":38,
                         "Name":"Have housing needs assessed - Step J",
                         "Active":true
                      },
                      {
                         "id":39,
                         "Name":"Have housing needs assessed - Step K",
                         "Active":true
                      },
                      {
                         "id":40,
                         "Name":"Have housing needs assessed - Step L",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":6,
                   "Name":"Reduce or pay off any unpaid rent payments, so they can be considered for other accommodation",
                   "Active":true,
                   "Steps":[
                      {
                         "id":41,
                         "Name":"Reduce or pay off any unpaid rent - Step A",
                         "Active":true
                      },
                      {
                         "id":42,
                         "Name":"Reduce or pay off any unpaid rent - Step B",
                         "Active":true
                      },
                      {
                         "id":43,
                         "Name":"Reduce or pay off any unpaid rent - Step C",
                         "Active":true
                      },
                      {
                         "id":44,
                         "Name":"Reduce or pay off any unpaid rent - Step D",
                         "Active":true
                      },
                      {
                         "id":45,
                         "Name":"Reduce or pay off any unpaid rent - Step E",
                         "Active":true
                      },
                      {
                         "id":46,
                         "Name":"Reduce or pay off any unpaid rent - Step F",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":7,
                   "Name":"Actively try to find suitable housing by submitting applications and attending assessments",
                   "Active":true,
                   "Steps":[
                      {
                         "id":47,
                         "Name":"Actively try to find suitable housing - Step A",
                         "Active":true
                      },
                      {
                         "id":48,
                         "Name":"Actively try to find suitable housing - Step B",
                         "Active":true
                      },
                      {
                         "id":49,
                         "Name":"Actively try to find suitable housing - Step C",
                         "Active":true
                      },
                      {
                         "id":50,
                         "Name":"Actively try to find suitable housing - Step D",
                         "Active":true
                      },
                      {
                         "id":51,
                         "Name":"Actively try to find suitable housing - Step E",
                         "Active":true
                      },
                      {
                         "id":52,
                         "Name":"Actively try to find suitable housing - Step F",
                         "Active":true
                      },
                      {
                         "id":53,
                         "Name":"Actively try to find suitable housing - Step G",
                         "Active":true
                      },
                      {
                         "id":54,
                         "Name":"Actively try to find suitable housing - Step H",
                         "Active":true
                      },
                      {
                         "id":55,
                         "Name":"Actively try to find suitable housing - Step J",
                         "Active":true
                      },
                      {
                         "id":56,
                         "Name":"Actively try to find suitable housing - Step K",
                         "Active":true
                      },
                      {
                         "id":57,
                         "Name":"Actively try to find suitable housing - Step L",
                         "Active":true
                      },
                      {
                         "id":58,
                         "Name":"Actively try to find suitable housing - Step M",
                         "Active":true
                      },
                      {
                         "id":59,
                         "Name":"Actively try to find suitable housing - Step N",
                         "Active":true
                      }
                   ]
                }
             ]
          },
          {
             "id":2,
             "Name":"Drugs",
             "active":true,
             "Goals":[
                {
                   "id":8,
                   "Name":"Drugs Goal 1",
                   "Active":true,
                   "Steps":[
                      {
                         "id":60,
                         "Name":"Drugs Goal 1 - Step A",
                         "Active":true
                      },
                      {
                         "id":61,
                         "Name":"Drugs Goal 1 - Step B",
                         "Active":true
                      },
                      {
                         "id":62,
                         "Name":"Drugs Goal 1 - Step C",
                         "Active":true
                      },
                      {
                         "id":63,
                         "Name":"Drugs Goal 1 - Step D",
                         "Active":true
                      },
                      {
                         "id":64,
                         "Name":"Drugs Goal 1 - Step E",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":9,
                   "Name":"Drugs Goal 2",
                   "Active":true,
                   "Steps":[
                      {
                         "id":65,
                         "Name":"Drugs Goal 2 - Step A",
                         "Active":true
                      },
                      {
                         "id":66,
                         "Name":"Drugs Goal 2 - Step B",
                         "Active":true
                      },
                      {
                         "id":67,
                         "Name":"Drugs Goal 2 - Step C",
                         "Active":true
                      },
                      {
                         "id":68,
                         "Name":"Drugs Goal 2 - Step D",
                         "Active":true
                      },
                      {
                         "id":69,
                         "Name":"Drugs Goal 2 - Step E",
                         "Active":true
                      },
                      {
                         "id":70,
                         "Name":"Drugs Goal 2 - Step F",
                         "Active":true
                      },
                      {
                         "id":71,
                         "Name":"Drugs Goal 2 - Step G",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":10,
                   "Name":"Drugs Goal 3",
                   "Active":true,
                   "Steps":[
                      {
                         "id":72,
                         "Name":"Drugs Goal 3 - Step A",
                         "Active":true
                      },
                      {
                         "id":73,
                         "Name":"Drugs Goal 3 - Step B",
                         "Active":true
                      },
                      {
                         "id":74,
                         "Name":"Drugs Goal 3 - Step C",
                         "Active":true
                      },
                      {
                         "id":75,
                         "Name":"Drugs Goal 3 - Step D",
                         "Active":true
                      },
                      {
                         "id":76,
                         "Name":"Drugs Goal 3 - Step E",
                         "Active":true
                      },
                      {
                         "id":77,
                         "Name":"Drugs Goal 3 - Step F",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":11,
                   "Name":"Drugs Goal 4",
                   "Active":true,
                   "Steps":[
                      {
                         "id":78,
                         "Name":"Drugs Goal 4 - Step A",
                         "Active":true
                      },
                      {
                         "id":79,
                         "Name":"Drugs Goal 4 - Step B",
                         "Active":true
                      },
                      {
                         "id":80,
                         "Name":"Drugs Goal 4 - Step C",
                         "Active":true
                      },
                      {
                         "id":81,
                         "Name":"Drugs Goal 4 - Step D",
                         "Active":true
                      },
                      {
                         "id":82,
                         "Name":"Drugs Goal 4 - Step E",
                         "Active":true
                      },
                      {
                         "id":83,
                         "Name":"Drugs Goal 4 - Step F",
                         "Active":true
                      },
                      {
                         "id":84,
                         "Name":"Drugs Goal 4 - Step G",
                         "Active":true
                      },
                      {
                         "id":85,
                         "Name":"Drugs Goal 4 - Step H",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":12,
                   "Name":"Drugs Goal 5",
                   "Active":true,
                   "Steps":[
                      {
                         "id":86,
                         "Name":"Drugs Goal 5 - Step A",
                         "Active":true
                      },
                      {
                         "id":87,
                         "Name":"Drugs Goal 5 - Step B",
                         "Active":true
                      },
                      {
                         "id":88,
                         "Name":"Drugs Goal 5 - Step C",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":13,
                   "Name":"Drugs Goal 6",
                   "Active":true,
                   "Steps":[
                      {
                         "id":89,
                         "Name":"Drugs Goal 6 - Step A",
                         "Active":true
                      },
                      {
                         "id":90,
                         "Name":"Drugs Goal 6 - Step B",
                         "Active":true
                      },
                      {
                         "id":91,
                         "Name":"Drugs Goal 6 - Step C",
                         "Active":true
                      },
                      {
                         "id":92,
                         "Name":"Drugs Goal 6 - Step D",
                         "Active":true
                      },
                      {
                         "id":93,
                         "Name":"Drugs Goal 6 - Step E",
                         "Active":true
                      },
                      {
                         "id":94,
                         "Name":"Drugs Goal 6 - Step F",
                         "Active":true
                      },
                      {
                         "id":95,
                         "Name":"Drugs Goal 6 - Step G",
                         "Active":true
                      },
                      {
                         "id":96,
                         "Name":"Drugs Goal 6 - Step H",
                         "Active":true
                      },
                      {
                         "id":97,
                         "Name":"Drugs Goal 6 - Step J",
                         "Active":true
                      },
                      {
                         "id":98,
                         "Name":"Drugs Goal 6 - Step K",
                         "Active":true
                      },
                      {
                         "id":99,
                         "Name":"Drugs Goal 6 - Step L",
                         "Active":true
                      },
                      {
                         "id":100,
                         "Name":"Drugs Goal 6 - Step M",
                         "Active":true
                      },
                      {
                         "id":101,
                         "Name":"Drugs Goal 6 - Step N",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":14,
                   "Name":"Drugs Goal 7",
                   "Active":true,
                   "Steps":[
                      {
                         "id":102,
                         "Name":"Drugs Goal 7 - Step A",
                         "Active":true
                      },
                      {
                         "id":103,
                         "Name":"Drugs Goal 7 - Step B",
                         "Active":true
                      },
                      {
                         "id":104,
                         "Name":"Drugs Goal 7 - Step C",
                         "Active":true
                      },
                      {
                         "id":105,
                         "Name":"Drugs Goal 7 - Step D",
                         "Active":true
                      },
                      {
                         "id":106,
                         "Name":"Drugs Goal 7 - Step E",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":15,
                   "Name":"Drugs Goal 8",
                   "Active":true,
                   "Steps":[
                      {
                         "id":107,
                         "Name":"Drugs Goal 8 - Step A",
                         "Active":true
                      },
                      {
                         "id":108,
                         "Name":"Drugs Goal 8 - Step B",
                         "Active":true
                      },
                      {
                         "id":109,
                         "Name":"Drugs Goal 8 - Step C",
                         "Active":true
                      },
                      {
                         "id":110,
                         "Name":"Drugs Goal 8 - Step D",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":16,
                   "Name":"Drugs Goal 9",
                   "Active":true,
                   "Steps":[
                      {
                         "id":111,
                         "Name":"Drugs Goal 9 - Step A",
                         "Active":true
                      },
                      {
                         "id":112,
                         "Name":"Drugs Goal 9 - Step B",
                         "Active":true
                      },
                      {
                         "id":113,
                         "Name":"Drugs Goal 9 - Step C",
                         "Active":true
                      },
                      {
                         "id":114,
                         "Name":"Drugs Goal 9 - Step D",
                         "Active":true
                      },
                      {
                         "id":115,
                         "Name":"Drugs Goal 9 - Step E",
                         "Active":true
                      },
                      {
                         "id":116,
                         "Name":"Drugs Goal 9 - Step F",
                         "Active":true
                      }
                   ]
                }
             ]
          },
          {
             "id":3,
             "Name":"Health and Wellbeing",
             "active":true,
             "Goals":[
                {
                   "id":17,
                   "Name":"Health and Wellbeing Goal 1",
                   "Active":true,
                   "Steps":[
                      {
                         "id":117,
                         "Name":"Health and Wellbeing Goal 1 - Step A",
                         "Active":true
                      },
                      {
                         "id":118,
                         "Name":"Health and Wellbeing Goal 1 - Step B",
                         "Active":true
                      },
                      {
                         "id":119,
                         "Name":"Health and Wellbeing Goal 1 - Step C",
                         "Active":true
                      },
                      {
                         "id":120,
                         "Name":"Health and Wellbeing Goal 1 - Step D",
                         "Active":true
                      },
                      {
                         "id":121,
                         "Name":"Health and Wellbeing Goal 1 - Step E",
                         "Active":true
                      },
                      {
                         "id":122,
                         "Name":"Health and Wellbeing Goal 1 - Step F",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":18,
                   "Name":"Health and Wellbeing Goal 2",
                   "Active":true,
                   "Steps":[
                      {
                         "id":123,
                         "Name":"Health and Wellbeing Goal 2 - Step A",
                         "Active":true
                      },
                      {
                         "id":124,
                         "Name":"Health and Wellbeing Goal 2 - Step B",
                         "Active":true
                      },
                      {
                         "id":125,
                         "Name":"Health and Wellbeing Goal 2 - Step C",
                         "Active":true
                      },
                      {
                         "id":126,
                         "Name":"Health and Wellbeing Goal 2 - Step D",
                         "Active":true
                      },
                      {
                         "id":127,
                         "Name":"Health and Wellbeing Goal 2 - Step E",
                         "Active":true
                      },
                      {
                         "id":128,
                         "Name":"Health and Wellbeing Goal 2 - Step F",
                         "Active":true
                      },
                      {
                         "id":129,
                         "Name":"Health and Wellbeing Goal 2 - Step G",
                         "Active":true
                      },
                      {
                         "id":130,
                         "Name":"Health and Wellbeing Goal 2 - Step H",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":19,
                   "Name":"Health and Wellbeing Goal 3",
                   "Active":true,
                   "Steps":[
                      {
                         "id":131,
                         "Name":"Health and Wellbeing Goal 3 - Step A",
                         "Active":true
                      },
                      {
                         "id":132,
                         "Name":"Health and Wellbeing Goal 3 - Step B",
                         "Active":true
                      },
                      {
                         "id":133,
                         "Name":"Health and Wellbeing Goal 3 - Step C",
                         "Active":true
                      },
                      {
                         "id":134,
                         "Name":"Health and Wellbeing Goal 3 - Step D",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":20,
                   "Name":"Health and Wellbeing Goal 4",
                   "Active":true,
                   "Steps":[
                      {
                         "id":135,
                         "Name":"Health and Wellbeing Goal 4 - Step A",
                         "Active":true
                      },
                      {
                         "id":136,
                         "Name":"Health and Wellbeing Goal 4 - Step B",
                         "Active":true
                      },
                      {
                         "id":137,
                         "Name":"Health and Wellbeing Goal 4 - Step C",
                         "Active":true
                      },
                      {
                         "id":138,
                         "Name":"Health and Wellbeing Goal 4 - Step D",
                         "Active":true
                      },
                      {
                         "id":139,
                         "Name":"Health and Wellbeing Goal 4 - Step E",
                         "Active":true
                      },
                      {
                         "id":140,
                         "Name":"Health and Wellbeing Goal 4 - Step F",
                         "Active":true
                      },
                      {
                         "id":141,
                         "Name":"Health and Wellbeing Goal 4 - Step G",
                         "Active":true
                      },
                      {
                         "id":142,
                         "Name":"Health and Wellbeing Goal 4 - Step H",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":21,
                   "Name":"Health and Wellbeing Goal 5",
                   "Active":true,
                   "Steps":[
                      {
                         "id":143,
                         "Name":"Health and Wellbeing Goal 5 - Step A",
                         "Active":true
                      },
                      {
                         "id":144,
                         "Name":"Health and Wellbeing Goal 5 - Step B",
                         "Active":true
                      },
                      {
                         "id":145,
                         "Name":"Health and Wellbeing Goal 5 - Step C",
                         "Active":true
                      },
                      {
                         "id":146,
                         "Name":"Health and Wellbeing Goal 5 - Step D",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":22,
                   "Name":"Health and Wellbeing Goal 6",
                   "Active":true,
                   "Steps":[
                      {
                         "id":147,
                         "Name":"Health and Wellbeing Goal 6 - Step A",
                         "Active":true
                      },
                      {
                         "id":148,
                         "Name":"Health and Wellbeing Goal 6 - Step B",
                         "Active":true
                      },
                      {
                         "id":149,
                         "Name":"Health and Wellbeing Goal 6 - Step C",
                         "Active":true
                      }
                   ]
                },
                {
                   "id":23,
                   "Name":"Health and Wellbeing Goal 7",
                   "Active":true,
                   "Steps":[
                      {
                         "id":150,
                         "Name":"Health and Wellbeing Goal 7 - Step A",
                         "Active":true
                      },
                      {
                         "id":151,
                         "Name":"Health and Wellbeing Goal 7 - Step B",
                         "Active":true
                      },
                      {
                         "id":152,
                         "Name":"Health and Wellbeing Goal 7 - Step C",
                         "Active":true
                      },
                      {
                         "id":153,
                         "Name":"Health and Wellbeing Goal 7 - Step D",
                         "Active":true
                      },
                      {
                         "id":154,
                         "Name":"Health and Wellbeing Goal 7 - Step E",
                         "Active":true
                      },
                      {
                         "id":155,
                         "Name":"Health and Wellbeing Goal 7 - Step F",
                         "Active":true
                      },
                      {
                         "id":156,
                         "Name":"Health and Wellbeing Goal 7 - Step G",
                         "Active":true
                      },
                      {
                         "id":157,
                         "Name":"Health and Wellbeing Goal 7 - Step H",
                         "Active":true
                      },
                      {
                         "id":158,
                         "Name":"Health and Wellbeing Goal 7 - Step J",
                         "Active":true
                      },
                      {
                         "id":159,
                         "Name":"Health and Wellbeing Goal 7 - Step K",
                         "Active":true
                      }
                   ]
                }
             ]
          }
       ]
   }'
);