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
     ),
     policies as (
         select policy_id from accounts.user_policies
                                   inner join accounts.policies on policies.id = user_policies.policy_id
         where user_id = :userId
         union
         select policy_id from roles
                                   inner join accounts.role_policies on role_policies.role_id = roles.role_id
         union
         select policy_id from positionP
     ),
     policy_to_check as (
         select id from accounts.policies
         where (prefix || ':' || name) = :policy
     ),
     is_admin as (
         select is_admin as has_permission
         from accounts.users
         where id = :userId
     ),
     has_policy as (
         select count(*) > 0 as has_permission from policies
         where policy_id = (select id from policy_to_check)
     )

select bool_or(has_permission) as has_permission from (
    select has_permission from is_admin
    union
    select has_permission from has_policy
) as result
