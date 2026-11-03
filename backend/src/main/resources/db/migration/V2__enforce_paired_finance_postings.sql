-- Deployment gate: before starting this version against an existing database, inspect:
-- SELECT count(*) FILTER (WHERE debit_account_id IS NULL) AS null_debit,
--        count(*) FILTER (WHERE credit_account_id IS NULL) AS null_credit,
--        count(*) FILTER (WHERE debit_account_id = credit_account_id) AS same_account,
--        count(*) FILTER (WHERE amount <= 0) AS non_positive_amount
-- FROM finance_transactions;
-- If any rows match, stop deployment. Do not delete or silently rewrite ledger history.
-- Finance must review each affected source record and approve its disposition; preserve original
-- transaction IDs and audit evidence. Do not apply a blanket account assignment or amount update.
-- Retry deployment only after the violations have been resolved with Finance-approved corrections.
-- This migration deliberately fails on incompatible existing data rather than remediating it.

ALTER TABLE finance_transactions
    ALTER COLUMN debit_account_id SET NOT NULL;

ALTER TABLE finance_transactions
    ALTER COLUMN credit_account_id SET NOT NULL;

ALTER TABLE finance_transactions
    ADD CONSTRAINT ck_finance_transactions_distinct_accounts
        CHECK (debit_account_id <> credit_account_id);

ALTER TABLE finance_transactions
    ADD CONSTRAINT ck_finance_transactions_positive_amount
        CHECK (amount > 0);
