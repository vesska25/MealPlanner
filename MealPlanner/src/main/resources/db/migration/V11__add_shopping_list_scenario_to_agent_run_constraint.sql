-- Step 10 Phase B added the SHOPPING_LIST agent scenario (AgentScenario.java) but never updated
-- this constraint to allow it — a dormant bug since then, since every existing test mocks
-- AgentRunner rather than exercising a real run against Postgres. Found by manually driving the
-- SHOPPING_LIST scenario end-to-end (PRD 9.1 step 11), which no automated test does.
ALTER TABLE agent_run DROP CONSTRAINT chk_agent_run_scenario;
ALTER TABLE agent_run ADD CONSTRAINT chk_agent_run_scenario CHECK (scenario IN
    ('PANTRY_ASSISTANT', 'MEAL_PLANNING', 'SHOPPING_LIST'));
