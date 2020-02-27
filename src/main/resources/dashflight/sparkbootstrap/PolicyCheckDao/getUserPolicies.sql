with position as (
    select role_id from accounts.user_positions
                            inner join accounts.position_roles on position_roles.position_id = user_positions.position_id
    where user_positions.user_id = :userId
),
     positionP as (
         select policy_id from accounts.user_positions
                                   inner join accounts.position_policies on position_policies.position_id = user_positions.position_id
         where user_positions.user_id = :userId
     ),
     roles as (
         select user_roles.role_id from accounts.user_roles
                                            inner join accounts.role_policies on role_policies.role_id = user_roles.role_id
         where user_roles.user_id = :userId
         union
         select role_id from position
     )

select policy_id from accounts.user_policies
inner join accounts.policies on policies.id = user_policies.policy_id
where user_id = :userId
union
select policy_id from roles
inner join accounts.role_policies on role_policies.role_id = roles.role_id
union
select policy_id from positionP