;Fill
; Two execution ticker message to the same order

{:symbol "BML",
 :incomingComplete true,
 :standingId 3939,
 :account "SAH10171568",
 :ok true,
 :filledAt "2016-01-20T08:31:58.144995432Z",
 :venue "OYFVEX",
 :order
 {:open true,
  :symbol "BML",
  :orderType "limit",
  :totalFilled 98,
  :account "SAH10171568",
  :ts "2016-01-20T08:31:53.802834938Z",
  :id 3939,
  :ok true,
  :originalQty 100,
  :venue "OYFVEX",
  :qty 2,
  :fills
  [{:price 11472, :qty 98, :ts "2016-01-20T08:31:58.144995432Z"}],
  :price 11472,
  :direction "sell"},
 :incomingId 3940,
 :standingComplete false,
 :filled 98,
 :price 11472}

{:symbol "BML",
 :incomingComplete false,
 :standingId 3939,
 :account "SAH10171568",
 :ok true,
 :filledAt "2016-01-20T08:31:58.146951945Z",
 :venue "OYFVEX",
 :order
 {:open false,
  :symbol "BML",
  :orderType "limit",
  :totalFilled 100,
  :account "SAH10171568",
  :ts "2016-01-20T08:31:53.802834938Z",
  :id 3939,
  :ok true,
  :originalQty 100,
  :venue "OYFVEX",
  :qty 0,
  :fills
  [{:price 11472, :qty 98, :ts "2016-01-20T08:31:58.144995432Z"}
   {:price 11472, :qty 2, :ts "2016-01-20T08:31:58.146951945Z"}],
  :price 11472,
  :direction "sell"},
 :incomingId 3941,
 :standingComplete true,
 :filled 2,
 :price 11472}
