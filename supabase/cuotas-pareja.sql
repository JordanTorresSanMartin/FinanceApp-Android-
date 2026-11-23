-- Cuotas en Supabase + puente seguro para la planilla de gastos en pareja.
-- Ejecutar una vez en Supabase > SQL Editor > New query > pegar todo > Run.

-- 1) Planes de cuotas (misma forma que InstallmentPlan de la app) ----------------------------
create table if not exists public.installment_plans (
  id          text primary key,                      -- mismo id que usa la app
  user_id     uuid not null default auth.uid() references auth.users on delete cascade,
  name        text not null,
  amount      bigint not null check (amount >= 0),   -- CLP por cuota
  total       int not null check (total > 0),
  paid_before int not null default 0 check (paid_before >= 0),
  due_day     int check (due_day between 1 and 31),
  created_on  date not null default current_date,
  owed_by     text,                                  -- quién te debe las cuotas (ej. 'Fran'); null = cuota propia
  updated_at  timestamptz not null default now()
);

create table if not exists public.installment_payments (
  id         uuid primary key default gen_random_uuid(),
  plan_id    text not null references public.installment_plans(id) on delete cascade,
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  paid_on    date not null,
  source     text not null default 'app' check (source in ('app', 'planilla')),
  created_at timestamptz not null default now()
);
create index if not exists installment_payments_plan_idx on public.installment_payments (plan_id, paid_on);

alter table public.installment_plans    enable row level security;
alter table public.installment_payments enable row level security;

drop policy if exists "dueño" on public.installment_plans;
create policy "dueño" on public.installment_plans
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists "dueño" on public.installment_payments;
create policy "dueño" on public.installment_payments
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

-- 2) Token para la planilla: solo ve cuotas con owed_by y solo puede registrar sus pagos -----
create table if not exists public.sheet_tokens (
  token   text primary key,
  user_id uuid not null references auth.users on delete cascade
);
alter table public.sheet_tokens enable row level security;  -- sin políticas: nadie lo lee por la API

-- Cuotas que alguien te debe, con cuántas van pagadas
create or replace function public.sheet_plans(p_token text)
returns table (id text, name text, amount bigint, total int, paid int, due_day int, owed_by text)
language sql stable security definer set search_path = public as $$
  select p.id, p.name, p.amount, p.total,
         least(p.total, p.paid_before + (select count(*) from installment_payments x where x.plan_id = p.id)::int),
         p.due_day, p.owed_by
  from installment_plans p
  join sheet_tokens t on t.user_id = p.user_id
  where t.token = p_token and p.owed_by is not null;
$$;

-- Pagos de esas cuotas, con su número de cuota (para anotarlos en la planilla)
create or replace function public.sheet_payments(p_token text)
returns table (id uuid, plan_id text, paid_on date, source text, cuota int)
language sql stable security definer set search_path = public as $$
  select x.id, x.plan_id, x.paid_on, x.source,
         (p.paid_before + row_number() over (partition by x.plan_id order by x.paid_on, x.created_at))::int
  from installment_payments x
  join installment_plans p on p.id = x.plan_id
  join sheet_tokens t on t.user_id = p.user_id
  where t.token = p_token and p.owed_by is not null;
$$;

-- Registrar desde la planilla que te pagaron la siguiente cuota
create or replace function public.sheet_pay(p_token text, p_plan_id text, p_paid_on date)
returns uuid
language plpgsql security definer set search_path = public as $$
declare v_user uuid; v_left int; v_id uuid;
begin
  select p.user_id, p.total - p.paid_before - (select count(*) from installment_payments x where x.plan_id = p.id)
    into v_user, v_left
  from installment_plans p join sheet_tokens t on t.user_id = p.user_id
  where t.token = p_token and p.id = p_plan_id and p.owed_by is not null;
  if v_user is null then raise exception 'plan no encontrado o token inválido'; end if;
  if v_left <= 0 then raise exception 'las cuotas ya están pagadas'; end if;
  insert into installment_payments (plan_id, user_id, paid_on, source)
    values (p_plan_id, v_user, p_paid_on, 'planilla') returning id into v_id;
  return v_id;
end $$;

revoke all on function public.sheet_plans(text), public.sheet_payments(text), public.sheet_pay(text, text, date) from public;
grant execute on function public.sheet_plans(text), public.sheet_payments(text), public.sheet_pay(text, text, date) to anon, authenticated;

-- 3) Crear tu token (cambia el correo por el de tu cuenta de la app). Copia el resultado. ---
-- insert into public.sheet_tokens (token, user_id)
--   select replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', ''), id from auth.users where email = 'TU_CORREO_DE_LA_APP'
--   returning token;

-- 4) (Opcional, hasta que la app suba sus cuotas sola) Cargar a mano el celular de Fran: --
-- insert into public.installment_plans (id, user_id, name, amount, total, paid_before, due_day, owed_by)
--   select 'celular-fran', id, 'Celular Fran', 28000, 12, 9, 1, 'Fran' from auth.users where email = 'TU_CORREO_DE_LA_APP';
