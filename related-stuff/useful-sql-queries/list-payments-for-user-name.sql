select um.name as made_by, uc.name as client, p.registered, p.amount from users uc, users um, clients c, payments p where uc.id = c.user_id and c.id = p.client_id and um.id = p.made_by_id and uc.name
